/*
 * Copyright 2017 Santeri Paavolainen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model._
import net.jcazevedo.moultingyaml._


trait WorldYamlProtocol extends DefaultYamlProtocol {
  def capacityModel(model: String, data: Option[YamlValue]): DistributionModel =
    model match {
      case "uniform" ⇒
        // uniform capacity model is assumed to be [0, 1] relative
        // of maximum value, but it can be given other values, either
        // LOW or [LOW, HIGH]
        data match {
          case None ⇒ UniformDistributionModel
          case Some(YamlNumber(v)) ⇒ UniformDistributionModel(v.toDouble, 1.0)
          case Some(YamlArray(Seq(YamlNumber(l), YamlNumber(h)))) ⇒ UniformDistributionModel(l.toDouble, h.toDouble)
        }
      case "constant" ⇒
        data match {
          case None => ConstantDistributionModel
          case Some(YamlNumber(v)) ⇒ ConstantDistributionModel(v.toDouble)
        }
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
            StepDistributionModel(steps)
        }
      case "beta" ⇒
        data match {
          case Some(YamlArray(Seq(YamlNumber(alpha), YamlNumber(beta)))) ⇒
            BetaDistributionModel(alpha.toDouble, beta.toDouble)
        }
      case other ⇒
        throw new IllegalArgumentException(s"capacity model type $other is not known")

    }

  case class TypeHolder(name: Option[String],
                        aggregated: Option[Boolean],
                        model: String,
                        data: Option[YamlValue])

  // we use the same "UnitHolder" for all drains, sources etc., since all
  // fields are optional it doesn't matter
  case class UnitHolder(id: Option[String],
                        name: Option[String],
                        capacity: Option[Int],
                        `type`: Option[String],
                        ghg: Option[Double],
                        areas: Option[Tuple2[String, String]],
                        disabled: Option[Boolean])

  case class AreaHolder(name: Option[String],
                        sources: Option[Seq[UnitHolder]],
                        drains: Option[Seq[UnitHolder]],
                        external: Option[Boolean])

  case class WorldHolder(name: Option[String],
    version: Option[Int],
                         types: Option[Map[String, TypeHolder]],
                         areas: Option[Map[String, AreaHolder]],
                         lines: Option[Seq[UnitHolder]]) {
    require(types.isEmpty ||
      (types.get.keySet & Set("constant", "uniform")).isEmpty)
  }

  implicit val typeHolderFormat = yamlFormat4(TypeHolder)
  implicit val unitHolderFormat = yamlFormat7(UnitHolder)
  implicit val areaHolderFormat = yamlFormat4(AreaHolder)
  implicit val worldHolderFormat = yamlFormat5(WorldHolder)


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
          id → DistributionType(id,
            typeHolder.name,
            typeHolder.aggregated.getOrElse(false),
            capacityModel(typeHolder.model, typeHolder.data))
      }

      def getType(name: String) =
        name match {
          case "constant" ⇒ ConstantDistributionType
          case "uniform" ⇒ UniformDistributionType
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
                unitHolder.ghg.getOrElse(0.0),
                unitHolder.disabled.getOrElse(false))
          }
          val drains = areaHolder.drains.getOrElse(Seq.empty[UnitHolder]).map {
            unitHolder ⇒
              Drain(
                orId(unitHolder.id),
                unitHolder.name,
                unitHolder.capacity.getOrElse(0),
                getType(unitHolder.`type`.getOrElse("constant")),
                unitHolder.disabled.getOrElse(false))
          }

          id → Area(id,
            name = areaHolder.name,
            sources = sources,
            drains = drains,
            external = areaHolder.external.getOrElse(false))
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
            area1, area2,
            lineHolder.disabled.getOrElse(false))
      }

      World(name = worldHolder.name.getOrElse("unnamed world"),
        version = worldHolder.version.getOrElse(1),
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
