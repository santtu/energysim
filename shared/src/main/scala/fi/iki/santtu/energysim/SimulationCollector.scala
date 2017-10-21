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
    s"[loss=$loss,total=$total,excess=$excess,generation=$generation,drain=$drain,transfer=$transfer]"
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
}

object SourceStatistics { def apply() = new SourceStatistics() }
object DrainStatistics { def apply() = new DrainStatistics() }
object LineStatistics { def apply() = new LineStatistics() }


class SimulationCollector(world: World) {
  val areas: Map[Area, AreaStatistics] =
    world.areas.map { _ → AreaStatistics() }.toMap
  val sources: Map[Source, SourceStatistics] =
    world.areas.map(_.sources).flatten.map { _ → SourceStatistics() }.toMap
  val drains: Map[Drain, DrainStatistics] =
    world.areas.map(_.drains).flatten.map { _ → DrainStatistics() }.toMap
  val lines: Map[Line, LineStatistics] =
    world.lines.map { _ → LineStatistics() }.toMap
  val global = AreaStatistics()

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

            val ag = a.sources.map(s ⇒ round.units(s).used * s.ghgPerCapacity).sum
            areas(a).ghg += ag
            ghg += ag
        }

        global += AreaData(total, excess, generation, drain, transfer)
        global.ghg += ghg

        round.units.foreach {
          case (s: Source, sd) ⇒
            sources(s) += sd
            sources(s).ghg += sd.used * s.ghgPerCapacity
          case (d: Drain, dd) ⇒
            drains(d) += dd
          case (l: Line, ld) ⇒
            lines(l) += ld
        }
    }
  }

  override def toString: String = s"[global:$global,areas:$areas,sources:$sources,drains:$drains,lines:$lines]"
}

object SimulationCollector {
  def apply(world: World) =
    new SimulationCollector(world)

  def simulate(world: World, simulation: Simulation, count: Int) = {
    val collector = SimulationCollector(world)
    collector += simulation.simulate(world, count)
    collector
  }
}
