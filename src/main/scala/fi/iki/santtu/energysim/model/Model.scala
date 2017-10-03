package fi.iki.santtu.energysim.model

case class Capacity(val amount: Int = 0)

abstract class CapacityModel {
  def capacity(): Capacity
}

abstract class Unit(val name: String, val capacityModel: CapacityModel) {
}

class Drain(name: String, capacityModel: CapacityModel) extends Unit(name, capacityModel) {
  override def toString: String = s"Drain($name,$capacityModel)"
}

object Drain {
  def apply(name: String, capacityModel: CapacityModel): Drain =
    new Drain(name, capacityModel)
}

class Source(name: String, capacityModel: CapacityModel, val ghgPerCapacity: Double) extends Unit(name, capacityModel) {
  override def toString: String = s"Source($name,$capacityModel,$ghgPerCapacity)"
}

object Source {
  def apply(name: String, capacityModel: CapacityModel, ghgPerCapacity: Double = 0.0): Source =
    new Source(name, capacityModel, ghgPerCapacity)
}

class Line(name: String, capacityModel: CapacityModel, val areas: Tuple2[Area, Area]) extends Unit(name, capacityModel) {

  override def toString: String = s"Line($name,$capacityModel,${areas._1.name}<->${areas._2.name})"
}

object Line {
  def apply(name: String,
            capacityModel: CapacityModel,
            areas: Tuple2[Area, Area]) =
    new Line(name, capacityModel, areas)
}

case class Area (val name: String,
                 val drains: Seq[Drain] = Seq.empty[Drain],
                 val sources: Seq[Source] = Seq.empty[Source],
                 val links: Seq[Line] = Seq.empty[Line])

class World (val name: String,
             val areas: Seq[Area] = Seq.empty[Area],
             val lines: Seq[Line] = Seq.empty[Line]) {
  val units: Seq[Unit] = areas.map(_.drains).flatten ++
    areas.map(_.sources).flatten ++
    lines

  override def toString: String =
    s"World(name=$name,areas=$areas)"
}

object World {
  def apply(name: String,
            areas: Seq[Area] = Seq.empty[Area],
            lines: Seq[Line] = Seq.empty[Line]): World =
    new World(name, areas, lines)
}
