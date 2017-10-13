package fi.iki.santtu.energysim.model

case class Capacity(amount: Int = 0) {
  override def toString: String = s"${amount}MW"
}

abstract class CapacityModel {
  def capacity(): Capacity
}

abstract class Unit(val name: String, val capacityModel: CapacityModel) {
}

class Drain(name: String, capacityModel: CapacityModel) extends Unit(name, capacityModel) {
  override def toString: String = name
}

object Drain {
  def apply(name: String = "drain",
            capacityModel: CapacityModel = NullCapacityModel): Drain =
    new Drain(name, capacityModel)
}

class Source(name: String, capacityModel: CapacityModel, val ghgPerCapacity: Double = 0.0) extends Unit(name, capacityModel) {
  override def toString: String = name
}

object Source {
  def apply(name: String = "source",
            capacityModel: CapacityModel = NullCapacityModel,
            ghgPerCapacity: Double = 0.0): Source =
    new Source(name, capacityModel, ghgPerCapacity)
}

class Line(name: String,
           capacityModel: CapacityModel,
           val areas: Tuple2[Area, Area])
  extends Unit(name, capacityModel) {
  override def toString: String = s"${areas._1.name}<->${areas._2.name}"
}

object Line {
  def apply(name: String = "line",
            capacityModel: CapacityModel = NullCapacityModel,
            areas: Tuple2[Area, Area]) =
    new Line(name, capacityModel, areas)
}

class Area (val name: String, val drains: Seq[Drain], val sources: Seq[Source]) {
  override def toString: String = name
}

object Area {
  def apply(name: String = "area",
            drains: Seq[Drain] = Seq.empty[Drain],
            sources: Seq[Source] = Seq.empty[Source]) =
    new Area(name, drains, sources)
}

class World (val name: String,
             val areas: Seq[Area] = Seq.empty[Area],
             val lines: Seq[Line] = Seq.empty[Line]) {
  val units: Seq[Unit] = areas.flatMap(_.drains) ++
    areas.flatMap(_.sources) ++
    lines

  override def toString: String =
    s"World(name=$name,areas=$areas)"
}

object World {
  def apply(name: String = "world",
            areas: Seq[Area] = Seq.empty[Area],
            lines: Seq[Line] = Seq.empty[Line]): World =
    new World(name, areas, lines)
}
