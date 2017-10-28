package fi.iki.santtu.energysim.model

case class Capacity(amount: Int = 0) {
  override def toString: String = s"${amount}MW"
}

abstract class CapacityModel {
  def capacity(amount: Int): Capacity
}

case class CapacityType(name: String, size: Int, model: CapacityModel)

object ConstantCapacityType extends CapacityType("constant", 0, ConstantCapacityModel)
object UniformCapacityType extends CapacityType("uniform", 0, UniformCapacityModel)

abstract class Unit(val name: String, val unitCapacity: Int, val capacityType: CapacityType) {
  def capacity(): Capacity = {
    capacityType.size match {
      case 0 ⇒ capacityType.model.capacity(unitCapacity)
      case size ⇒
        val result = (Seq.fill(unitCapacity / size) { capacityType.model.capacity(size) } ++
          (unitCapacity % size match {
            case 0 ⇒ Seq()
            case pad ⇒ Seq(capacityType.model.capacity(pad))
          })).foldLeft[Int](0) { case (v, c) ⇒ v + c.amount }
        // yeah nicer would be to use .sum and define Capacity either as
        // alias to Int, or implement summable for that .. but that'd
        // be overkill
        Capacity(result)
    }
  }
}

class Drain(name: String, capacity: Int, capacityType: CapacityType) extends Unit(name, capacity, capacityType) {
  override def toString: String = name
}

object Drain {
  def apply(name: String = "drain",
            capacity: Int = 0,
            capacityType: CapacityType = ConstantCapacityType): Drain =
    new Drain(name, capacity, capacityType)
}

class Source(name: String,
             capacity: Int = 0,
             capacityType: CapacityType = ConstantCapacityType,
             val ghgPerCapacity: Double = 0.0) extends Unit(name, capacity, capacityType) {
  override def equals(obj: scala.Any): Boolean =
    obj match {
      case o: Source ⇒
        o.name == name &&
          o.unitCapacity == unitCapacity &&
          o.capacityType == capacityType &&
          o.ghgPerCapacity == ghgPerCapacity
    }

  override def toString: String = name
//  override def toString: String = s"$name,$capacity,$capacityType,$ghgPerCapacity"
}

object Source {
  def apply(name: String = "source",
            capacity: Int = 0,
            capacityType: CapacityType = ConstantCapacityType,
            ghgPerCapacity: Double = 0.0): Source =
    new Source(name, capacity, capacityType, ghgPerCapacity)
}

class Line(name: String,
           capacity: Int,
           capacityType: CapacityType,
           val area1: Area,
           val area2: Area)
  extends Unit(name, capacity, capacityType) {
  override def toString: String = s"$area1<->$area2"
  val areas: (Area, Area) = (area1, area2)
}

object Line {
  def apply(name: String = "line",
            capacity: Int,
            capacityType: CapacityType = ConstantCapacityType,
            area1: Area, area2: Area): Line =
    new Line(name, capacity, capacityType, area1, area2)
}

case class Area (name: String, drains: Seq[Drain], sources: Seq[Source]) {
  override def toString: String = name
}

object Area {
  def apply(name: String = "area",
            drains: Seq[Drain] = Seq.empty[Drain],
            sources: Seq[Source] = Seq.empty[Source]) =
    new Area(name, drains, sources)
}

case class World (name: String,
                  types: Seq[CapacityType] = Seq.empty[CapacityType],
                  areas: Seq[Area] = Seq.empty[Area],
                  lines: Seq[Line] = Seq.empty[Line]) {
  val units: Seq[Unit] = areas.flatMap(_.drains) ++
    areas.flatMap(_.sources) ++
    lines

  override def toString: String =
    s"World(name=$name,areas=$areas,types=$types,lines=$lines)"

  // some manipulations, these return a copy
  def remove(area: Area, drain: Drain) =
    copy(areas = areas.map {
      case a if a == area ⇒ a.copy(drains = a.drains.filter(_ != drain))
      case a ⇒ a
    })
}

object World {
  def apply(name: String = "world",
            types: Seq[CapacityType] = Seq.empty[CapacityType],
            areas: Seq[Area] = Seq.empty[Area],
            lines: Seq[Line] = Seq.empty[Line]): World =
    new World(name, types, areas, lines)
}
