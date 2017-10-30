package fi.iki.santtu.energysim


import fi.iki.santtu.energysim.model._
import io.circe.{JsonNumber, _}
import io.circe.generic.auto._
import io.circe.syntax._

object JsonDecoder extends ModelDecoder {
  private type CapacityHolder = Seq[Json]
  private case class TypeHolder(size: Option[Int],
                                model: String,
                                data: Option[Json])
  private case class UnitHolder(name: Option[String],
                                capacity: Option[Double],
                                `type`: Option[String],
                                ghg: Option[Double])
  private case class AreaHolder(sources: Option[Seq[UnitHolder]],
                                drains: Option[Seq[UnitHolder]])
  private case class LineHolder(name: Option[String],
                                capacity: Option[Double],
                                `type`: Option[String],
                                areas: Tuple2[String, String])
  private case class WorldHolder(name: Option[String],
                                 types: Option[Map[String, TypeHolder]],
                                 areas: Option[Map[String, AreaHolder]],
                                 lines: Option[Seq[LineHolder]])
  
  override def decode(data: Array[Byte]): World = {
    val json = new String(data, "UTF-8").asJson
    val model = io.circe.parser.decode[WorldHolder](new String(data, "UTF-8"))
    model match {
      case Left(error) ⇒ throw error
      case Right(model) ⇒ holder2world(model)
    }
  }

  private var counter: Int = 0

  private def nameFor(str: Option[String], label: String): String =
    str match {
      case Some(name) ⇒ name
      case None ⇒
        counter += 1
        s"$label $counter"
    }

  private def capacityData(model: CapacityModel): (String, Option[Json]) = {
    def steps2json(steps: Seq[Step]) =
      Json.fromValues(steps.map(s ⇒
        Json.fromValues(
          Seq(s.probability, s.low, s.high).
            map(Json.fromDoubleOrNull))))

    model match {
      case m: UniformCapacityModel ⇒ ("uniform", Some(Json.fromValues(
        Seq(m.low, m.high).map(Json.fromDoubleOrNull))))
      case _: ConstantCapacityModel ⇒ ("constant", None)
      case m: ScaledCapacityModel ⇒ ("scaled", Some(
        Json.fromFields(Seq(
          "mean" → Json.fromDoubleOrNull(m.mean),
          "bins" → steps2json(m.steps)))))
      case m: StepCapacityModel ⇒ ("step", Some(steps2json(m.steps)))
      case m: BetaCapacityModel ⇒ ("beta", Some(Json.fromValues(
        Seq(Json.fromDoubleOrNull(m.alpha), Json.fromDoubleOrNull(m.beta)))))
      case _ ⇒ ???
    }
  }

  private def capacityModel(model: String, data: Option[Json]): CapacityModel = {
    def ary2steps(ary: Seq[Seq[Double]]) =
      ary.map {
        case Seq(p, c) ⇒ Step(p, c, c)
        case Seq(p, l, h) ⇒ Step(p, l, h)
      }

    model match {
      case "uniform" ⇒
        // uniform capacity model is assumed to be [0, 1] relative
        // of maximum value, but it can be given other values, either
        // LOW or [LOW, HIGH]
        data match {
          case None ⇒ UniformCapacityModel
          case Some(json) ⇒
            (json.as[Double], json.as[Seq[Double]]) match {
              case (Right(c), _) ⇒ UniformCapacityModel(c, 1.0)
              case (_, Right(Seq(l, h))) ⇒ UniformCapacityModel(l, h)
            }
        }
        UniformCapacityModel
      case "constant" ⇒
        ConstantCapacityModel
      case "step" ⇒
        data.get.as[Seq[Seq[Double]]] match {
          case Right(ary) ⇒
            StepCapacityModel(ary2steps(ary))
          case _ ⇒
            throw new IllegalArgumentException(s"'step' model parameters are invalid: $data")
        }
      case "scaled" ⇒
        val d = data.get

        (d \\ "mean", d \\ "bins") match {
          case (Seq(a), Seq(b)) ⇒
            (a.as[Double], b.as[Seq[Seq[Double]]]) match {
              case (Right(mean), Right(ary)) ⇒
//                println(s"mean=$mean ary=$ary")
                ScaledCapacityModel(mean, ary2steps(ary))
            }
        }
      case "beta" ⇒
        data.get.as[Seq[Double]] match {
          case Right(Seq(alpha, beta)) ⇒ BetaCapacityModel(alpha, beta)
        }
      case other ⇒
        throw new IllegalArgumentException(s"capacity model type $other is not known")

    }
  }

  private def holder2world(model: WorldHolder): World = {
    // first generate types
    val types = (model.types match {
      case Some(types) ⇒
        types.map { case (name, data) ⇒ name → CapacityType(name, data.size.getOrElse(0), capacityModel(data.model, data.data)) }
      case None ⇒
        Nil
    }).toMap


    def getType(name: String) =
      name match {
        case "constant" ⇒ ConstantCapacityType
        case "uniform" ⇒ UniformCapacityType
        case other ⇒ types(other)
      }

    // we need to first generate areas, then use them later to create lines
    // and finally modify areas to contain those lines
    val areas = model.areas.getOrElse(Map.empty[String, AreaHolder]).map {
      case (name, a) ⇒
        val drains = a.drains.getOrElse(Seq.empty[UnitHolder]).map(
          d ⇒ Drain(nameFor(d.name, "drain"),
            d.capacity.getOrElse(0.0).toInt,
            getType(d.`type`.getOrElse("constant"))))

        val sources = a.sources.getOrElse(Seq.empty[UnitHolder]).map(
          s ⇒ Source(nameFor(s.name, "source"),
            s.capacity.getOrElse(0.0).toInt,
            getType(s.`type`.getOrElse("constant")),
            s.ghg.getOrElse(0)))

        name → Area(name = name, sources = sources, drains = drains)
    }

    val lines = model.lines.getOrElse(Seq.empty[LineHolder]).map {
      l ⇒ Line(
        nameFor(l.name, "line"),
        l.capacity.getOrElse(0.0).toInt,
        getType(l.`type`.getOrElse("constant")),
        areas(l.areas._1), areas(l.areas._2))
    }

    World(name = nameFor(model.name, "world"),
      types = types.values.toList,
      areas = areas.values.toList,
      lines = lines.toList
    )
  }

  override def encode(w: World): Array[Byte] = {
    WorldHolder(
      name = Some(w.name),
      types = Some(w.types.map { t ⇒
        val (model, data) = capacityData(t.model)
        t.name → TypeHolder(
          size = Some(t.size),
          model = model,
          data = data
        )
      }.toMap),
      areas = Some(w.areas.map { a ⇒
        a.name → AreaHolder(
          sources = Some(a.sources.map {
            s ⇒ UnitHolder(
              name = Some(s.name),
              `type` = Some(s.capacityType.name),
              capacity = Some(s.unitCapacity),
              ghg = Some(s.ghgPerCapacity))
          }),
          drains = Some(a.drains.map {
            d ⇒ UnitHolder(
              name = Some(d.name),
              `type` = Some(d.capacityType.name),
              capacity = Some(d.unitCapacity),
              ghg = None)
          })
        )
      }.toMap),
      lines = Some(w.lines.map { l ⇒
        LineHolder(
          name = Some(l.name),
          capacity = Some(l.unitCapacity),
          `type` = Some(l.capacityType.name),
          areas = (l.area1.name, l.area2.name)
        )
      })
    ).asJson.toString().getBytes("UTF-8")
  }
}
