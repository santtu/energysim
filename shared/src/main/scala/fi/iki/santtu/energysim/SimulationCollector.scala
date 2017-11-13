package fi.iki.santtu.energysim

import javax.sound.sampled.SourceDataLine

import fi.iki.santtu.energysim.model.{Area, Drain, Line, Source, World}
import fi.iki.santtu.energysim.simulation.{AreaData, Result, Simulation, UnitData}

import scala.concurrent.Future

case class Results(val rounds: Int)
//
//                   val areaIndex: Map[Area, Int],
//                   val drainIndex: Map[Drain, Int],
//                   val sourceIndex: Map[Source, Int],
//                   val lineIndex: Map[Line, Int])

class MeanVariance {
  private var n: Long = 0
  private var m: Double = 0.0
  private var M2: Double = 0.0
  private var high = Double.NegativeInfinity
  private var low = Double.PositiveInfinity

  def count: Long = n
  def mean: Double = m
  def variance: Double = {
    if (n > 1)
      M2 / (n - 1)
    else
      Double.NaN
  }
  def dev: Double = Math.sqrt(variance)
  def min: Double = low
  def max: Double = high

  def +=(v: Double): Unit = {
    n += 1
    val delta = v - m
    m += delta / n
    val delta2 = v - m
    M2 += delta * delta2
    high = Math.max(high, v)
    low = Math.min(low, v)
  }

  def *(v: Double): MeanVariance = {
    val o = new MeanVariance()
    o.n = n

    o.m = m * v
    o.M2 = M2 * v
    o.high = high * v
    o.low = low * v

    o
  }

  def /(v: Double): MeanVariance = this * (1 / v)

  override def toString: String = {
    val head = if (dev.isNaN || dev == 0.0)
      f"$mean%.0f"
    else
      f"$mean%.0f ± $dev%.1f"

//    val tail = if (!min.isInfinite)
//      f" ($min%.0f–$max%.0f)"
//    else ""
//
//    head + tail

    head
  }
}

class Portion {
  private var n: Long = 0
  private var p: Long = 0

  def count: Long = n
  def positive: Long = p
  def negative: Long = n - p
  def portion: Double = positive.toDouble / count.toDouble
  def percentage: Double = portion * 100.0

  def +=(v: Boolean): Unit = {
    n += 1
    if (v)
      p += 1
  }

  override def toString: String = f"$percentage%.1f%%"
}

object MeanVariance {
  def apply() = new MeanVariance()
}

object Portion {
  def apply() = new Portion()
}

class AreaStatistics {
  val total = MeanVariance()
  val excess = MeanVariance()
  val generation = MeanVariance()
  val drain = MeanVariance()
  val transfer = MeanVariance()
  val loss = Portion()
  val ghg = MeanVariance()

  def +=(a: AreaData): Unit = {
    loss += a.total < 0
    total += a.total
    excess += a.excess
    generation += a.generation
    drain += a.drain
    transfer += a.transfer

    // TODO: area GHG
  }

  override def toString: String =
    s"[loss=$loss,total=$total,excess=$excess,generation=$generation,drain=$drain,transfer=$transfer,ghg=$ghg]"
}

object AreaStatistics {
  def apply() = new AreaStatistics()
}

abstract class UnitStatistics {
  val used = MeanVariance()
  val excess = MeanVariance()
  val capacity = MeanVariance()

  def +=(ud: UnitData): Unit = {
    used += ud.used
    excess += ud.excess
    capacity += ud.capacity
  }
}

class DrainStatistics extends UnitStatistics {
}

class SourceStatistics extends UnitStatistics {
  val ghg = MeanVariance()
  val atCapacity = Portion()

  override def +=(sd: UnitData): Unit = {
    super.+=(sd)
    atCapacity += sd.excess == 0
    // TODO GHG
  }

  override def toString: String =
    s"[used=$used,excess=$excess,capacity=$capacity,ghg=$ghg,maxed=$atCapacity]"
}

class TypeStatistics extends SourceStatistics {
  // actually no difference here, since we count sources (but
  // multiple of them)
}

class LineStatistics extends UnitStatistics {
  val transfer = MeanVariance() // abs of used
  val unused = MeanVariance() // abs against capacity
  val right = MeanVariance() // from first to second
  val left = MeanVariance()
  val atCapacity = Portion()

  override def +=(ld: UnitData): Unit = {
    super.+=(ld)

    val actual = Math.abs(ld.used)

    atCapacity += actual == ld.capacity
    transfer += actual
    unused += ld.capacity - actual

    if (ld.used > 0)
      right += actual
    else
      left += actual
  }

  override def toString: String =
    s"[transfer=$transfer,unused=$unused,right=$right,left=$left,full=$atCapacity]"
}

object SourceStatistics { def apply() = new SourceStatistics() }
object DrainStatistics { def apply() = new DrainStatistics() }
object LineStatistics { def apply() = new LineStatistics() }
object TypeStatistics { def apply() = new TypeStatistics() }


class SimulationCollector(private val world: World) {
  var rounds: Int = 0
  val areas: Map[String, AreaStatistics] =
    world.areas.map { _.id → AreaStatistics() }.toMap
  val sources: Map[String, SourceStatistics] =
    world.areas.flatMap(_.sources).map { _.id → SourceStatistics() }.toMap
  val drains: Map[String, DrainStatistics] =
    world.areas.flatMap(_.drains).map { _.id → DrainStatistics() }.toMap
  val lines: Map[String, LineStatistics] =
    world.lines.map { _.id → LineStatistics() }.toMap
  val types: Map[String, TypeStatistics] =
    world.types.map { _.id → TypeStatistics() }.toMap
  val global = AreaStatistics()

  // some fields that are used only internally and not public
  private val sourcesByAreaId = world.areas.map(a ⇒ a.id → a.sources).toMap
  private val sourcesById = world.areas.flatMap(_.sources).map(s ⇒ s.id → s).toMap
  // note: if built-in types (uniform, constant) are used, they do not
  // have a defined type in the world -- these sums are effectively
  // discarded (we generate TypeStatistics for them, but it is not
  // visible externally).
  private val unitsByType = world.units.filter(u ⇒ types.contains(u.capacityType.id)).map {
    u ⇒ u.capacityType.id → u.id
  }.groupBy(_._1).mapValues(_.map(_._2))

  def +=(r: Result): Unit = {
    r.rounds foreach {
      round ⇒
        // to have proper round-to-round global statistics, we need
        // to add all areas together and add them to the global
        // statistics in one part
        var total = 0
        var excess = 0
        var transfer = 0
        var generation = 0
        var drain = 0
        var ghg = 0.0

        round.areas.foreach {
          case (a, ad) ⇒
            areas(a) += ad
            total += ad.total
            excess += ad.excess
            generation += ad.generation
            drain += ad.drain
            transfer += ad.transfer

            val ag = sourcesByAreaId(a).map(s ⇒ round.units(s.id).used * s.ghgPerCapacity).sum
            areas(a).ghg += ag
            ghg += ag
        }

        unitsByType.foreach {
          case (t, units) ⇒
            val ud = units.foldLeft(UnitData(0, 0, 0)) {
              case (ud, u) ⇒ ud + round.units(u)
            }
            types(t) += ud
        }

        global += AreaData(total, excess, generation, drain, transfer)
        global.ghg += ghg

        round.units.foreach {
          case (s, sd) if sources.contains(s) ⇒
            sources(s) += sd
            sources(s).ghg += sd.used * sourcesById(s).ghgPerCapacity
//            typesByUnit(s) += sd
//            typesByUnit(s).ghg += sd.used * sourcesById(s).ghgPerCapacity
          case (d, dd) if drains.contains(d) ⇒
            drains(d) += dd
          case (l, ld) if lines.contains(l) ⇒
            lines(l) += ld
          case _ ⇒
            // should not happen
            ???
        }

        rounds += 1
    }
  }

  override def toString: String = s"[global:$global,areas:$areas,sources:$sources,drains:$drains,lines:$lines]"
}

object SimulationCollector {
  def apply(world: World) =
    new SimulationCollector(world)

  def simulate(world: World, simulation: Simulation, count: Int): SimulationCollector = {
    val collector = SimulationCollector(world)
    collector += simulation.simulate(world, count)
    collector
  }

  private def h(s: String, pad: String) =
    s"${pad * 4} $s ${pad * (65 - s.length)}"

  private def areaSummary(id: String, a: AreaStatistics) = {
    Seq(h(id, "="),
      f"  loss        ${a.loss.positive}%d / ${a.loss.percentage}%.1f%%",
      f"  total       ${a.total}%s MW",
      f"  excess      ${a.excess}%s MW",
      f"  generation  ${a.generation}%s MW",
      f"  drain       ${a.drain}%s MW",
      f"  transfer    ${a.transfer}%s MW",
      f"  ghg         ${a.ghg / 1e3 * 365 * 24}%s t/a")
  }

  private def sourceSummary(id: String, s: SourceStatistics) = {
    Seq(s"  > $id",
      f"    maxed     ${s.atCapacity.percentage}%.1f%%",
      f"    used      ${s.used}%s MW",
      f"    excess    ${s.excess}%s MW",
      f"    capacity  ${s.capacity}%s MW",
      f"    ghg       ${s.ghg / 1e3 * 365 * 24}%s t/a")
  }

  private def drainSummary(id: String, d: DrainStatistics) = {
    Seq(f"  < $id%-9s ${d.used}%s MW")
  }

  private def lineSummary(id: String, leftName: String, rightName: String, l: LineStatistics) = {
    Seq(h(s"$id ($leftName ↔︎ $rightName)", "-"),
      f"  maxed       ${l.atCapacity.percentage}%.1f%%",
      f"  transfer    ${l.transfer} MW",
      f"  unused      ${l.unused} MW",
      f" →$leftName%-11s ${l.left} MW",
      f" →$rightName%-11s ${l.right} MW")
  }

  def summary(collector: SimulationCollector): String = {
    val result =
      globalSummary(collector) ++
        typesSummary(collector) ++
        areasSummary(collector) ++
        linesSummary(collector)

    result.mkString("\n")
  }

  def globalSummary(collector: SimulationCollector) =
    areaSummary(collector.world.name, collector.global)

  def typesSummary(collector: SimulationCollector) =
        Seq(h("Types", "~")) ++
        collector.world.types.flatMap {
          t ⇒ sourceSummary(t.id, collector.types(t.id))
        }

  def areasSummary(collector: SimulationCollector) =
        collector.areas.flatMap {
          case (id, s) ⇒
            val a = collector.world.areaById(id).get
            areaSummary(id, s) ++
              a.sources.flatMap(s ⇒ sourceSummary(s.id, collector.sources(s.id))) ++
              a.drains.flatMap(d ⇒ drainSummary(d.id, collector.drains(d.id)))
        }

  def linesSummary(collector: SimulationCollector) =
    collector.lines.flatMap {
      case (id, s) ⇒
        val l = collector.world.lineById(id).get
        lineSummary(l.id, l.area1.id, l.area2.id, s)
    }
}
