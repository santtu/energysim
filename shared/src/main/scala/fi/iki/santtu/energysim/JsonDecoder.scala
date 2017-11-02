package fi.iki.santtu.energysim


import fi.iki.santtu.energysim.model._
import fi.iki.santtu.energysim.simulation.Result
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._

object JsonDecoder extends ModelDecoder {
  private type CapacityHolder = Seq[Json]
  private case class TypeHolder(name: Option[String],
                                size: Option[Int],
                                model: String,
                                data: Option[Json])
  private case class DrainHolder(id: Option[String],
                                name: Option[String],
                                capacity: Option[Double],
                                 `type`: Option[String],
                                 disabled: Option[Boolean])
  private case class SourceHolder(id: Option[String],
                                  name: Option[String],
                                  capacity: Option[Double],
                                  `type`: Option[String],
                                  ghg: Option[Double],
                                  disabled: Option[Boolean])
  private case class AreaHolder(name: Option[String],
                                sources: Option[Seq[SourceHolder]],
                                drains: Option[Seq[DrainHolder]])
  private case class LineHolder(id: Option[String],
                                name: Option[String],
                                capacity: Option[Double],
                                `type`: Option[String],
                                areas: Tuple2[String, String],
                                disabled: Option[Boolean])
  private case class WorldHolder(name: Option[String],
                                 types: Option[Map[String, TypeHolder]],
                                 areas: Option[Map[String, AreaHolder]],
                                 lines: Option[Seq[LineHolder]])

  def decodeWorldFromJson(json: Json): World = {
    json.as[WorldHolder] match {
      case Left(error) ⇒ throw error
      case Right(model) ⇒ holder2world(model)
    }
  }

  def decodeResultFromJson(json: Json): Result = {
    json.as[Result] match {
      case Left(error) ⇒ throw error
      case Right(result) ⇒ result
    }
  }

  override def decode(data: String): World = {
    parse(data) match {
      case Left(error) ⇒ throw error
      case Right(json) ⇒ decodeWorldFromJson(json)
    }
  }

  private var counter: Int = 0
  private def orId(str: Option[String]) =
    str match {
      case Some(s) ⇒ s
      case None ⇒
        counter += 1
        s"id-$counter"
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
        types.map { case (id, data) ⇒ id →
          CapacityType(id, data.name, data.size.getOrElse(0), capacityModel(data.model, data.data)) }
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
      case (id, a) ⇒
        val drains = a.drains.getOrElse(Seq.empty[DrainHolder]).map(
          d ⇒ Drain(orId(d.id), d.name,
            d.capacity.getOrElse(0.0).toInt,
            getType(d.`type`.getOrElse("constant")),
            d.disabled.getOrElse(false)))

        val sources = a.sources.getOrElse(Seq.empty[SourceHolder]).map(
          s ⇒ Source(orId(s.id),
            s.name,
            s.capacity.getOrElse(0.0).toInt,
            getType(s.`type`.getOrElse("constant")),
            s.ghg.getOrElse(0),
            s.disabled.getOrElse(false)))

        id → Area(id = id, name = a.name, sources = sources, drains = drains)
    }

    val lines = model.lines.getOrElse(Seq.empty[LineHolder]).map {
      l ⇒ Line(
        orId(l.id),
        l.name,
        l.capacity.getOrElse(0.0).toInt,
        getType(l.`type`.getOrElse("constant")),
        areas(l.areas._1), areas(l.areas._2),
        l.disabled.getOrElse(false))
    }

    World(name = model.name.getOrElse("unnamed world"),
      types = types.values.toList,
      areas = areas.values.toList,
      lines = lines.toList
    )
  }

  override def encode(w: World): String = {
    encodeAsJson(w).toString()
  }

  def encodeAsJson(w: World): Json = {
    WorldHolder(
      name = Some(w.name),
      types = Some(w.types.map { t ⇒
        val (model, data) = capacityData(t.model)
        t.id → TypeHolder(
          name = t.name,
          size = Some(t.size),
          model = model,
          data = data
        )
      }.toMap),
      areas = Some(w.areas.map { a ⇒
        a.id → AreaHolder(
          name = a.name,
          sources = Some(a.sources.map {
            s ⇒ SourceHolder(
              id = Some(s.id),
              name = s.name,
              `type` = Some(s.capacityType.id),
              capacity = Some(s.unitCapacity),
              ghg = Some(s.ghgPerCapacity),
              disabled = Some(s.disabled))
          }),
          drains = Some(a.drains.map {
            d ⇒ DrainHolder(
              id = Some(d.id),
              name = d.name,
              `type` = Some(d.capacityType.id),
              capacity = Some(d.unitCapacity),
              disabled = Some(d.disabled))
          })
        )
      }.toMap),
      lines = Some(w.lines.map { l ⇒
        LineHolder(
          id = Some(l.id),
          name = l.name,
          capacity = Some(l.unitCapacity),
          `type` = Some(l.capacityType.id),
          areas = (l.area1.id, l.area2.id),
          disabled = Some(l.disabled))
      })
    ).asJson
  }

  def encodeAsJson(result: Result): Json = {
    result.asJson
  }
}
