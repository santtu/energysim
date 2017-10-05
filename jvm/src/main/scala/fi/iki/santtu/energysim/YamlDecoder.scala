package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.JsonDecoder.CapacityHolder
import fi.iki.santtu.energysim.model._
import net.jcazevedo.moultingyaml._


private object Counter {
  var _count: Int = 0
  def count: Int = {
    _count += 1
    _count
  }
}

trait WorldYamlProtocol extends DefaultYamlProtocol {
  val emptyArray = Vector.empty[YamlValue]

  def getString(obj: YamlObject, field: String, default: String = null): String = {
    obj.getFields(YamlString(field)) match {
      case Seq(YamlString(value)) ⇒ value
      case Seq(something) ⇒
        deserializationError(s"field $field is not a string type, instead $something")
      case Nil if default == null ⇒
        deserializationError(s"field $field is missing and no default given")
      case Nil ⇒
        default
    }
  }

  def getArray(obj: YamlObject, field: String, default: Vector[YamlValue] = null): Vector[YamlValue] = {
    obj.getFields(YamlString(field)) match {
      case Seq(YamlArray(value)) ⇒ value
      case Seq(something) ⇒
        deserializationError(s"field $field is not a array type, instead $something")
      case Nil if default == null ⇒
        deserializationError(s"field $field is missing and no default given")
      case Nil ⇒
        default
    }
  }

  private type CapacityHolder = Seq[YamlValue]

  def capacity(c: Option[CapacityHolder]): CapacityModel =
    c match {
      case Some(c) ⇒
        CapacityConverter.convert(
          c(0).convertTo[String],
          c.slice(1, c.length).map(_.asInstanceOf[YamlNumber].value.toDouble))
      case None ⇒
        NullCapacityModel()
    }

  case class UnitHolder(name: Option[String], capacity: Option[CapacityHolder], ghg: Option[Double])
  implicit val unitHolderFormat = yamlFormat3(UnitHolder)

  implicit object AreaFormat extends YamlFormat[Area] {
    override def read(yaml: YamlValue): Area = {
      val obj = yaml.asYamlObject
//      println("areaformat.read: yaml=$yaml obj=$obj")

      val (name, drains, sources) = (
        getString(obj, "name", s"area ${Counter.count}"),
        getArray(obj, "drains", emptyArray),
        getArray(obj, "sources", emptyArray))

//      println(s"area: name=$name drains=$drains sources=$sources")

      // note: links are generated in the world parser later
      Area(name = name,
        drains = drains.map(_.convertTo[UnitHolder]).map(
          dh ⇒ Drain(dh.name.getOrElse(s"drain ${Counter.count}"), capacity(dh.capacity))),
        sources = sources.map(_.convertTo[UnitHolder]).map(
          sh ⇒ Source(sh.name.getOrElse(s"source ${Counter.count}"), capacity(sh.capacity), sh.ghg.getOrElse(0.0))))
    }

    override def write(obj: Area): YamlValue = ???
  }

  case class LineHolder(name: Option[String], capacity: Option[CapacityHolder], areas: Tuple2[String, String])
  implicit val lineHolderFormat = yamlFormat3(LineHolder)

  implicit object WorldFormat extends YamlFormat[World] {
    override def read(yaml: YamlValue): World = {
      val obj = yaml.asYamlObject
//      println(s"read: yaml=$yaml obj=$obj")

      val (name, areaHolders, lineHolders) = (
        getString(obj, "name", "nameless world"),
        getArray(obj, "areas", emptyArray),
        getArray(obj, "lines", emptyArray))

//      println(s"name=$name areas=$areas lines=$lines")

      val areas = areaHolders.map(_.convertTo[Area])
      val areasMap = areas.map(a ⇒ a.name → a).toMap

      val lines = lineHolders.map(_.convertTo[LineHolder]).map {
        lh ⇒
          val area1 = areasMap(lh.areas._1)
          val area2 = areasMap(lh.areas._2)
//          println(s"lh=$lh area1=$area1 area2=$area2")
          Line(lh.name.getOrElse(s"line ${Counter.count}"),
            capacity(lh.capacity),
            Tuple2(area1, area2))
      }

      World(name, areas, lines)
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