package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.JsonDecoder.CapacityHolder
import fi.iki.santtu.energysim.model._
import net.jcazevedo.moultingyaml._


trait WorldYamlProtocol extends DefaultYamlProtocol {
  def capacityModel(model: String, data: Option[YamlValue]): CapacityModel =
    ???

  case class TypeHolder(size: Option[Int],
                        model: String,
                        data: Option[YamlValue])

  // we use the same "UnitHolder" for all drains, sources etc., since all
  // fields are optional it doesn't matter
  case class UnitHolder(name: Option[String],
                        capacity: Option[Int],
                        `type`: Option[String],
                        ghg: Option[Double],
                        areas: Option[Tuple2[String, String]])

  case class AreaHolder(sources: Option[Seq[UnitHolder]],
                        drains: Option[Seq[UnitHolder]])

  case class WorldHolder(name: Option[String],
                         types: Option[Map[String, TypeHolder]],
                         areas: Option[Map[String, AreaHolder]],
                         lines: Option[Seq[UnitHolder]])

  implicit val typeHolderFormat = yamlFormat3(TypeHolder)
  implicit val unitHolderFormat = yamlFormat5(UnitHolder)
  implicit val areaHolderFormat = yamlFormat2(AreaHolder)
  implicit val worldHolderFormat = yamlFormat4(WorldHolder)

  implicit object WorldFormat extends YamlFormat[World] {
    override def read(yaml: YamlValue): World = {
      val worldHolder = yaml.convertTo[WorldHolder]

      // first map types, these are needed in drains and sources (and lines
      // later too)
      val types = worldHolder.types.getOrElse(Map.empty[String, TypeHolder]).map {
        case (name, typeHolder) ⇒
          name → CapacityType(name, typeHolder.size.getOrElse(0), capacityModel(typeHolder.model, typeHolder.data))
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
        case (name, areaHolder) ⇒
          val sources = areaHolder.sources.getOrElse(Seq.empty[UnitHolder]).map {
            unitHolder ⇒
              Source(unitHolder.name.getOrElse("unnamed source"),
                unitHolder.capacity.getOrElse(0),
                getType(unitHolder.`type`.getOrElse("constant")),
                unitHolder.ghg.getOrElse(0.0))
          }
          val drains = areaHolder.drains.getOrElse(Seq.empty[UnitHolder]).map {
            unitHolder ⇒
              Drain(unitHolder.name.getOrElse("unnamed drain"),
                unitHolder.capacity.getOrElse(0),
                getType(unitHolder.`type`.getOrElse("constant")))
          }

          name → Area(name = name, sources = sources, drains = drains)
      }

      // lines needs areas, so these come last
      val lines = worldHolder.lines.getOrElse(Seq.empty[UnitHolder]).map {
        lineHolder ⇒
          val area1 = areas(lineHolder.areas.get._1)
          val area2 = areas(lineHolder.areas.get._2)

          Line(lineHolder.name.getOrElse("unnamed line"),
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
  override def decode(data: Array[Byte]): World = {
    val yaml = new String(data, "UTF-8").parseYaml
    yaml.convertTo[World]
  }

  override def encode(world: World): Array[Byte] = ???
}