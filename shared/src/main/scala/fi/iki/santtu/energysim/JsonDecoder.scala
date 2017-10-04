package fi.iki.santtu.energysim


import fi.iki.santtu.energysim.model._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

object JsonDecoder extends ModelDecoder {
  private type CapacityHolder = Seq[Json]
  private case class UnitHolder(name: Option[String],
                                capacity: CapacityHolder,
                                ghg: Option[JsonNumber])
  private case class AreaHolder(name: Option[String],
                                sources: Option[Seq[UnitHolder]],
                                drains: Option[Seq[UnitHolder]])
  private case class LineHolder(name: Option[String],
                                capacity: CapacityHolder,
                                areas: Tuple2[String, String])
  private case class WorldHolder(name: Option[String],
                                 areas: Option[Seq[AreaHolder]],
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

  private def name(str: Option[String], label: String): String =
    str match {
      case Some(name) ⇒ name
      case None ⇒
        counter += 1
        s"$label $counter"
    }

  private def holder2world(model: WorldHolder): World = {
    def capacity(c: CapacityHolder): CapacityModel =
      CapacityConverter.convert(
        c(0).asString.get,
        c.slice(1, c.length).map(_.asNumber.get.toDouble))
    // we need to first generate areas, then use them later to create lines
    // and finally modify areas to contain those lines
    val areas = model.areas.getOrElse(Seq.empty[AreaHolder]).map {
      a ⇒ Area(name = name(a.name, "area"),
        drains = a.drains.getOrElse(Seq.empty[UnitHolder]).map(
          d ⇒ Drain(name(d.name, "drain"),
            capacity(d.capacity))),
        sources = a.sources.getOrElse(Seq.empty[UnitHolder]).map(
          s ⇒ Source(name(s.name, "source"),
            capacity(s.capacity),
            s.ghg.getOrElse(JsonNumber.fromString("0").get).toDouble)))
    }.map(a ⇒ a.name → a).toMap

    val lines = model.lines.getOrElse(Seq.empty[LineHolder]).map {
      l ⇒ Line(
        name(l.name, "line"),
        capacity(l.capacity),
        (areas(l.areas._1), areas(l.areas._2)))
    }

    val areaLines = lines.groupBy(_.areas._1) ++ lines.groupBy(_.areas._2)

    val areasWithLinks = areas.values.map(
      a ⇒ a.copy(links = areaLines.getOrElse(a, a.links))).toSeq

    World(name = name(model.name, "world"),
      areas = areasWithLinks,
      lines = lines
    )
  }

  override def encode(world: World): Array[Byte] = ???
}
