package fi.iki.santtu.energysim.model

trait Capacity {
  def aggregated: Boolean
  def independent: Boolean = !aggregated

  def amount(value: Int): Int
}

object NullCapacity extends Capacity {
  def aggregated = false
  def amount(value: Int) = 0
}

case class AggregatedCapacity(scaleFactor: Double) extends Capacity {
  def aggregated = true
  def amount(value: Int): Int = (scaleFactor * value).toInt

  override def toString: String = s"*$scaleFactor"
}

case class IndependentCapacity(scaleGetter: () ⇒ Double) extends Capacity {
  def aggregated = false
  def amount(value: Int): Int = (scaleGetter() * value).toInt

  override def toString: String = s"*($scaleGetter)"
}

//case class Capacity(amount: Int = 0) {
//  override def toString: String = s"${amount}MW"
//}

abstract class DistributionModel {
  /**
    * Sample the distribution model once, and return its value.
    *
    * @return Scaling factor, typically in range [0, 1] (but depending on
    *         context can also be in <0 or >0 range)
    */
  def sampleScaleFactor(): Double
}

case class DistributionType(id: String, name: Option[String], aggregated: Boolean, model: DistributionModel) {
  def makeCapacity: Capacity =
    if (aggregated) {
      AggregatedCapacity(model.sampleScaleFactor())
    } else {
      IndependentCapacity(model.sampleScaleFactor)
    }
}

object ConstantDistributionType extends DistributionType("constant", Some("Constant"), false, ConstantDistributionModel)
object UniformDistributionType extends DistributionType("uniform", Some("Uniform 0-1"), false, UniformDistributionModel)

abstract class Unit(val id: String, val name: Option[String], val unitCapacity: Int, val capacityType: DistributionType, val disabled: Boolean) {
  override def hashCode(): Int = Seq(id, name, unitCapacity, disabled).hashCode()
}

class Drain(id: String, name: Option[String], capacity: Int, capacityType: DistributionType, disabled: Boolean) extends Unit(id, name, capacity, capacityType, disabled) {
  override def toString: String = id

  override def equals(obj: scala.Any): Boolean =
    obj match {
      case d: Drain ⇒
        d.id == id &&
          d.name == name &&
          d.unitCapacity == unitCapacity &&
          d.capacityType == capacityType &&
          d.disabled == disabled
      case _ ⇒ false
    }
}

object Drain {
  def apply(id: String,
            name: Option[String] = None,
            capacity: Int = 0,
            capacityType: DistributionType = ConstantDistributionType,
            disabled: Boolean = false): Drain =
    new Drain(id, name, capacity, capacityType, disabled)
}

class Source(id: String,
             name: Option[String],
             capacity: Int = 0,
             capacityType: DistributionType = ConstantDistributionType,
             val ghgPerCapacity: Double = 0.0,
             disabled: Boolean = false) extends Unit(id, name, capacity, capacityType, disabled) {
  override def equals(obj: scala.Any): Boolean =
    obj match {
      case o: Source ⇒
        o.id == id &&
          o.name == name &&
          o.unitCapacity == unitCapacity &&
          o.capacityType == capacityType &&
          o.ghgPerCapacity == ghgPerCapacity &&
          o.disabled == disabled
      case _ ⇒ false
    }

  def copy(id: String = id, name: Option[String] = name,
           capacity: Int = unitCapacity, capacityType: DistributionType = capacityType,
           ghgPerCapacity: Double = ghgPerCapacity,
           disabled: Boolean = disabled) =
    Source(id, name, capacity, capacityType, ghgPerCapacity, disabled)

  override def toString: String = id
//  override def toString: String = s"$name,$capacity,$capacityType,$ghgPerCapacity"

  override def hashCode(): Int = super.hashCode() ^ ghgPerCapacity.hashCode()

}

object Source {
  def apply(id: String,
            name: Option[String] = None,
            capacity: Int = 0,
            capacityType: DistributionType = ConstantDistributionType,
            ghgPerCapacity: Double = 0.0,
            disabled: Boolean = false): Source =
    new Source(id, name, capacity, capacityType, ghgPerCapacity, disabled)
}

class Line(id: String,
           name: Option[String],
           capacity: Int,
           capacityType: DistributionType,
           val area1: Area,
           val area2: Area,
           disabled: Boolean)
  extends Unit(id, name, capacity, capacityType, disabled) {
  override def toString: String = s"$area1<->$area2"
  val areas: (Area, Area) = (area1, area2)
  val areasSeq = Seq(area1, area2)

  override def equals(obj: scala.Any): Boolean =
    obj match {
      case l: Line ⇒ l.id == id &&
        l.name == name &&
        l.unitCapacity == unitCapacity &&
        l.capacityType == capacityType && (
        (l.area1 == area1 && l.area2 == area2) ||
          (l.area1 == area2 && l.area2 == area1)) &&
        l.disabled == disabled
      case _ ⇒ false
    }

  override def hashCode(): Int = super.hashCode() ^ Seq(area1, area2).hashCode()

  def copy(id: String = id, name: Option[String] = name,
           capacity: Int = unitCapacity,
           capacityType: DistributionType = capacityType,
           area1: Area = area1,
           area2: Area = area2,
           disabled: Boolean = disabled) =
    Line(id, name, capacity, capacityType, area1, area2, disabled)
}

object Line {
  def apply(id: String,
            name: Option[String] = None,
            capacity: Int = 0,
            capacityType: DistributionType = ConstantDistributionType,
            area1: Area, area2: Area,
            disabled: Boolean = false): Line =
    new Line(id, name, capacity, capacityType, area1, area2, disabled)
}

case class Area (id: String, name: Option[String], drains: Seq[Drain], sources: Seq[Source], external: Boolean) {
  override def toString: String = id

  def drainByName(name: String): Option[Drain] = drains.find(_.name == name)
  def sourceByName(name: String): Option[Source] = sources.find(_.name == name)

  def update(source: Source) =
    copy(sources = sources.map {
      case old if old.id == source.id ⇒ source
      case old ⇒ old
    })
}

object Area {
  def apply(id: String,
            name: Option[String] = None,
            drains: Seq[Drain] = Seq.empty[Drain],
            sources: Seq[Source] = Seq.empty[Source],
            external: Boolean = false) =
    new Area(id, name, drains, sources, external)
}

case class World (name: String,
                  types: Seq[DistributionType] = Seq.empty[DistributionType],
                  areas: Seq[Area] = Seq.empty[Area],
                  lines: Seq[Line] = Seq.empty[Line]) {

  val units: Seq[Unit] = areas.flatMap(_.drains) ++
    areas.flatMap(_.sources) ++
    lines

  (types.map(_.id) ++ areas.map(_.id) ++ units.map(_.id)).foldLeft(Set.empty[String]) {
     (set, id) ⇒
       require(!set.contains(id), s"Identifier ${id} is used multiple times")
       set + id
  }

  override def toString: String =
    s"World(name=$name,areas=$areas,types=$types,lines=$lines)"

  // some manipulations, these return a copy
  def remove(area: Area, drain: Drain) =
    copy(areas = areas.map {
      case a if a == area ⇒ a.copy(drains = a.drains.filter(_ != drain))
      case a ⇒ a
    })

  def update(line: Line) =
    copy(lines = lines.map {
      case old if old.id == line.id ⇒ line
      case old ⇒ old
    })

  def update(area: Area) =
    copy(areas = areas.map {
      case old if old.id == area.id ⇒ area
      case old ⇒ old
    })

  def areaById(id: String): Option[Area] = areas.find(_.id == id)
  def lineById(id: String): Option[Line] = lines.find(_.id == id)
  def linesForArea(area: Area): Seq[Line] =
    lines.filter(l ⇒ l.area1 == area || l.area2 == area)

  override def equals(obj: scala.Any): Boolean =
    obj match {
      case w: World ⇒
        w.name == name &&
          w.types.toSet == types.toSet &&
          w.areas.toSet == areas.toSet &&
          w.lines.toSet == lines.toSet
      case _ ⇒ false
    }

  override def hashCode(): Int =
    name.hashCode() ^
    types.toSet.hashCode() ^
    areas.toSet.hashCode() ^
    lines.toSet.hashCode()
}

object World {
  def apply(name: String = "world",
            types: Seq[DistributionType] = Seq.empty[DistributionType],
            areas: Seq[Area] = Seq.empty[Area],
            lines: Seq[Line] = Seq.empty[Line]): World =
    new World(name, types, areas, lines)
}
