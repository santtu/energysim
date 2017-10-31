package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model._
import net.jcazevedo.moultingyaml._


trait WorldYamlProtocol extends DefaultYamlProtocol {
  case class ScaledHolder(mean: Double, bins: Seq[Seq[Double]])
  implicit val scaledHolderFormat = yamlFormat2(ScaledHolder)

  def capacityModel(model: String, data: Option[YamlValue]): CapacityModel =
    model match {
      case "uniform" ⇒
        // uniform capacity model is assumed to be [0, 1] relative
        // of maximum value, but it can be given other values, either
        // LOW or [LOW, HIGH]
        data match {
          case None ⇒ UniformCapacityModel
          case Some(YamlNumber(v)) ⇒ UniformCapacityModel(v.toDouble, 1.0)
          case Some(YamlArray(Seq(YamlNumber(l), YamlNumber(h)))) ⇒ UniformCapacityModel(l.toDouble, h.toDouble)
        }
        UniformCapacityModel
      case "constant" ⇒
        ConstantCapacityModel
      case "step" ⇒
        data match {
          case Some(YamlArray(ary)) ⇒
            val steps = ary.map {
              // prob, constant
              case YamlArray(Seq(YamlNumber(p), YamlNumber(c))) ⇒
                Step(p.toDouble, c.toDouble, c.toDouble)

              // prob, low, high
              case YamlArray(Seq(YamlNumber(p), YamlNumber(l), YamlNumber(h))) ⇒
                Step(p.toDouble, l.toDouble, h.toDouble)
            }
            StepCapacityModel(steps)
        }
      case "scaled" ⇒
        data.get.asYamlObject.convertTo[ScaledHolder] match {
          case ScaledHolder(mean, bins) ⇒
            val steps = bins.map {
              case Seq(p, c) ⇒ Step(p, c, c)
              case Seq(p, l, h) ⇒ Step(p, l, h)
            }
            ScaledCapacityModel(mean, steps)
        }
      case "beta" ⇒
        data match {
          case Some(YamlArray(Seq(YamlNumber(alpha), YamlNumber(beta)))) ⇒
            BetaCapacityModel(alpha.toDouble, beta.toDouble)
        }
      case other ⇒
        throw new IllegalArgumentException(s"capacity model type $other is not known")

    }

  case class TypeHolder(name: Option[String],
                        size: Option[Int],
                        model: String,
                        data: Option[YamlValue])

  // we use the same "UnitHolder" for all drains, sources etc., since all
  // fields are optional it doesn't matter
  case class UnitHolder(id: Option[String],
                        name: Option[String],
                        capacity: Option[Int],
                        `type`: Option[String],
                        ghg: Option[Double],
                        areas: Option[Tuple2[String, String]])

  case class AreaHolder(name: Option[String],
                        sources: Option[Seq[UnitHolder]],
                        drains: Option[Seq[UnitHolder]])

  case class WorldHolder(name: Option[String],
                         types: Option[Map[String, TypeHolder]],
                         areas: Option[Map[String, AreaHolder]],
                         lines: Option[Seq[UnitHolder]]) {
    require(types.isEmpty ||
      (types.get.keySet & Set("constant", "uniform")).isEmpty)
  }

  implicit val typeHolderFormat = yamlFormat4(TypeHolder)
  implicit val unitHolderFormat = yamlFormat6(UnitHolder)
  implicit val areaHolderFormat = yamlFormat3(AreaHolder)
  implicit val worldHolderFormat = yamlFormat4(WorldHolder)


  private var counter: Int = 0
  private def orId(str: Option[String]) =
    str match {
      case Some(s) ⇒ s
      case None ⇒
        counter += 1
        s"id-$counter"
    }

  implicit object WorldFormat extends YamlFormat[World] {
    override def read(yaml: YamlValue): World = {
      val worldHolder = yaml.convertTo[WorldHolder]

      // first map types, these are needed in drains and sources (and lines
      // later too)
      val types = worldHolder.types.getOrElse(Map.empty[String, TypeHolder]).map {
        case (id, typeHolder) ⇒
          id → CapacityType(id, typeHolder.name, typeHolder.size.getOrElse(0), capacityModel(typeHolder.model, typeHolder.data))
      }

      def getType(name: String) =
        name match {
          case "constant" ⇒ ConstantCapacityType
          case "uniform" ⇒ UniformCapacityType
          case other ⇒ types(other)
        }

      // second generate areas with sources and drains, named areas are
      // needed for lines later
      val areas = worldHolder.areas.getOrElse(Map.empty[String, AreaHolder]).map {
        case (id, areaHolder) ⇒
          val sources = areaHolder.sources.getOrElse(Seq.empty[UnitHolder]).map {
            unitHolder ⇒
              Source(
                orId(unitHolder.id),
                unitHolder.name,
                unitHolder.capacity.getOrElse(0),
                getType(unitHolder.`type`.getOrElse("constant")),
                unitHolder.ghg.getOrElse(0.0))
          }
          val drains = areaHolder.drains.getOrElse(Seq.empty[UnitHolder]).map {
            unitHolder ⇒
              Drain(
                orId(unitHolder.id),
                unitHolder.name,
                unitHolder.capacity.getOrElse(0),
                getType(unitHolder.`type`.getOrElse("constant")))
          }

          id → Area(id, name = areaHolder.name, sources = sources, drains = drains)
      }

      // lines needs areas, so these come last
      val lines = worldHolder.lines.getOrElse(Seq.empty[UnitHolder]).map {
        lineHolder ⇒
          val area1 = areas(lineHolder.areas.get._1)
          val area2 = areas(lineHolder.areas.get._2)

          Line(
            orId(lineHolder.id),
            lineHolder.name,
            lineHolder.capacity.getOrElse(0),
            getType(lineHolder.`type`.getOrElse("constant")),
            area1, area2)
      }

      World(worldHolder.name.getOrElse("unnamed world"),
        types = types.values.toSeq,
        areas = areas.values.toSeq,
        lines = lines.toSeq)
    }

    override def write(obj: World): YamlValue = ???
  }
}



object YamlDecoder extends ModelDecoder with WorldYamlProtocol {
  override def decode(data: String): World = {
    val yaml = data.parseYaml
    yaml.convertTo[World]
  }

  override def encode(world: World): String = ???
}