package fi.iki.santtu.energysim


import fi.iki.santtu.energysim.model._
import io.circe.{JsonNumber, _}
import io.circe.generic.auto._
import io.circe.syntax._

object JsonDecoder extends ModelDecoder {
  private type CapacityHolder = Seq[Json]
  private case class TypeHolder(size: Option[Int], model: String, data: Option[Json])
  private case class UnitHolder(name: Option[String],
                                capacity: Option[Int],
                                `type`: Option[String],
                                ghg: Option[JsonNumber])
  private case class AreaHolder(sources: Option[Seq[UnitHolder]],
                                drains: Option[Seq[UnitHolder]])
  private case class LineHolder(name: Option[String],
                                capacity: Option[Int],
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

  private def capacityModel(model: String, data: Option[Json]): CapacityModel =
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
            val steps = ary.map {
              case Seq(p, c) ⇒ Step(p, c, c)
              case Seq(p, l, h) ⇒ Step(p, l, h)
            }
            StepCapacityModel(steps)
        }
      case "beta" ⇒
        data.get.as[Seq[Double]] match {
          case Right(Seq(alpha, beta)) ⇒ BetaCapacityModel(alpha, beta)
        }
      case other ⇒
        throw new IllegalArgumentException(s"capacity model type $other is not known")

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
            d.capacity.getOrElse(0),
            getType(d.`type`.getOrElse("constant"))))

        val sources = a.sources.getOrElse(Seq.empty[UnitHolder]).map(
          s ⇒ Source(nameFor(s.name, "source"),
            s.capacity.getOrElse(0),
            getType(s.`type`.getOrElse("constant")),
            s.ghg.getOrElse(JsonNumber.fromString("0").get).toDouble))

        name → Area(name = name, sources = sources, drains = drains)
    }

    val lines = model.lines.getOrElse(Seq.empty[LineHolder]).map {
      l ⇒ Line(
        nameFor(l.name, "line"),
        l.capacity.getOrElse(0),
        getType(l.`type`.getOrElse("constant")),
        areas(l.areas._1), areas(l.areas._2))
    }

    World(name = nameFor(model.name, "world"),
      types = types.values.toSeq,
      areas = areas.values.toSeq,
      lines = lines
    )
  }

  override def encode(world: World): Array[Byte] = ???
}
