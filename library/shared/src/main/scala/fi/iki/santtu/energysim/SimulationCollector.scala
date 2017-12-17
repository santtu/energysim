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


import java.util.{Formattable, Formatter}

import fi.iki.santtu.energysim.model.{Area, Drain, Line, Source, World}
import fi.iki.santtu.energysim.simulation.{AreaData, Result, Simulation, UnitData}

import scala.reflect._

case class Results(val rounds: Int)
//
//                   val areaIndex: Map[Area, Int],
//                   val drainIndex: Map[Drain, Int],
//                   val sourceIndex: Map[Source, Int],
//                   val lineIndex: Map[Line, Int])

trait FixedBuffer[T] {
  def length: Int
  def size: Int
  def +=(v: T): Unit
  def update(v: Seq[T]): Unit
  def toSeq: Seq[T]
}

object FixedBuffer {
  class empty[T] extends FixedBuffer[T] {
    override def length: Int = 0
    override def size: Int = 0
    override def +=(v: T): Unit = Unit
    override def toSeq: Seq[T] = Seq.empty[T]
    override def update(v: Seq[T]): Unit = Unit
  }

  class nonempty[T](val size: Int)(implicit tag: ClassTag[T]) extends FixedBuffer[T] {
    private val data = tag.newArray(size)
    private var pos = 0
    var length = 0

    def +=(v: T): Unit = {
      data(pos) = v
      pos = (pos + 1) % size
      if (length < size)
        length += 1
    }

    override def update(v: Seq[T]): Unit = {
      if (v.length <= size) {
        pos = v.length
        length = v.length
        v.copyToArray(data)
      } else {
        pos = size
        length = size
        v.copyToArray(data, v.length - size)
      }
    }

    def toSeq = data.slice(pos, length) ++ data.slice(0, pos)
  }

  def apply[T](size: Int)(implicit tag: ClassTag[T]) = size match {
    case 0 ⇒ new empty[T]()
    case _ ⇒ new nonempty[T](size)
  }
}


class MeanVariance(history: Int = 0) extends Formattable {
  private var n: Long = 0
  private var m: Double = 0.0
  private var M2: Double = 0.0
  private var high = Double.NegativeInfinity
  private var low = Double.PositiveInfinity
  private var values = FixedBuffer[Double](history)

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

  def +(b: MeanVariance): MeanVariance = {
    val o = new MeanVariance(history)
    o.n = n + b.n
    o.m = m + b.m
    o.M2 = M2 + b.M2
    o.high = Math.max(high, b.high)
    o.low = Math.min(low, b.low)
    o
  }

  def +=(v: Double): Unit = {
    n += 1
    val delta = v - m
    m += delta / n
    val delta2 = v - m
    M2 += delta * delta2
    high = Math.max(high, v)
    low = Math.min(low, v)
    values += v
  }

  def *(v: Double): MeanVariance = {
    val o = new MeanVariance(history)
    o.n = n
    o.m = m * v
    o.M2 = M2 * v * v
    o.high = high * v
    o.low = low * v
    o.values.update(values.toSeq.map(_ * v))
    o
  }

  def -(): MeanVariance = this * -1.0

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

  def toSeq = values.toSeq

  override def formatTo(formatter: Formatter, flags: Int, width: Int, precision: Int) = {
    val fmt = (width, precision) match {
      case (-1, -1) ⇒ "%.1f"
      case (w, -1) ⇒ s"%${w}.1f"
      case (-1, p) ⇒ s"%.${p}f"
      case (w, p) ⇒ s"%${w}.${p}f"
    }

    formatter.format(s"$fmt", mean.asInstanceOf[AnyRef])

    if (!(dev.isNaN || dev == 0.0))
      formatter.format(s" ± $fmt", dev.asInstanceOf[AnyRef])
  }
}

object MeanVariance {
  implicit object MeanVarianceNumeric extends Numeric[MeanVariance] {
    override def plus(x: MeanVariance, y: MeanVariance): MeanVariance = x + y

    override def minus(x: MeanVariance, y: MeanVariance): MeanVariance = x - y

    override def times(x: MeanVariance, y: MeanVariance): MeanVariance = x * y

    override def negate(x: MeanVariance): MeanVariance = x * -1.0

    override def fromInt(x: Int): MeanVariance = ???

    override def toInt(x: MeanVariance): Int = ???

    override def toLong(x: MeanVariance): Long = ???

    override def toFloat(x: MeanVariance): Float = ???

    override def toDouble(x: MeanVariance): Double = ???

    override def compare(x: MeanVariance, y: MeanVariance): Int = ???

    override def zero: MeanVariance = MeanVariance()
  }

  def apply(history: Int = 0) = new MeanVariance(history)
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

object Portion {
  def apply() = new Portion()
}

class AreaStatistics(history: Int) {
  val total = MeanVariance(history)
  val excess = MeanVariance(history)
  val generation = MeanVariance(history)
  val drain = MeanVariance(history)
  val transfer = MeanVariance(history)
  val loss = Portion()
  val ghg = MeanVariance(history)

  def +=(a: AreaData): Unit = {
    loss += a.total < 0
    total += (if (a.total < 0) a.total else a.excess)
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
  def apply(history: Int = 0) = new AreaStatistics(history)
}

abstract class UnitStatistics(history: Int) {
  val used = MeanVariance(history)
  val excess = MeanVariance(history)
  val capacity = MeanVariance(history)
  val usage = MeanVariance(history)

  def +=(ud: UnitData): Unit = {
    used += ud.used
    excess += ud.excess
    capacity += ud.capacity
    usage += (if (ud.capacity > 0)
      ud.used.toDouble.abs / ud.capacity.abs
    else
      0.0)
  }
}

class DrainStatistics(history: Int) extends UnitStatistics(history) {
}

class SourceStatistics(history: Int) extends UnitStatistics(history) {
  val ghg = MeanVariance(history)
  val atCapacity = Portion()
  val proportion = MeanVariance(history)

  override def +=(sd: UnitData): Unit = {
    super.+=(sd)
    atCapacity += sd.excess == 0
    // TODO GHG
  }

  override def toString: String =
    s"[used=$used,excess=$excess,capacity=$capacity,ghg=$ghg,maxed=$atCapacity]"
}

class TypeStatistics(history: Int) extends SourceStatistics(history) {
  // actually no difference here, since we count sources (but
  // multiple of them)
}

class LineStatistics(history: Int) extends UnitStatistics(history) {
  val transfer = MeanVariance(history) // abs of used
  val unused = MeanVariance(history) // abs against capacity
  val right = MeanVariance(history) // from first to second
  val left = MeanVariance(history)
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

object SourceStatistics { def apply(history: Int = 0) = new SourceStatistics(history) }
object DrainStatistics { def apply(history: Int = 0) = new DrainStatistics(history) }
object LineStatistics { def apply(history: Int = 0) = new LineStatistics(history) }
object TypeStatistics { def apply(history: Int = 0) = new TypeStatistics(history) }


class SimulationCollector(private val world: World, history: Int) {
  var rounds: Int = 0
  val areas: Map[String, AreaStatistics] =
    world.areas.map { _.id → AreaStatistics(history) }.toMap
  val sources: Map[String, SourceStatistics] =
    world.areas.flatMap(_.sources).map { _.id → SourceStatistics(history) }.toMap
  val drains: Map[String, DrainStatistics] =
    world.areas.flatMap(_.drains).map { _.id → DrainStatistics(history) }.toMap
  val lines: Map[String, LineStatistics] =
    world.lines.map { _.id → LineStatistics(history) }.toMap
  val types: Map[String, TypeStatistics] =
    world.types.map { _.id → TypeStatistics(history) }.toMap
  val global = AreaStatistics(history)
  val external = AreaStatistics(history)

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
        // calculate total power generation and drain, this is used for
        // calculating proportions of units, types and areas
        val totalGenerated = world.areas.filter(!_.external).flatMap(_.sources).map {
          s ⇒ round.units(s.id).used
        }.sum

        // to have proper round-to-round global statistics, we need
        // to add all areas together and add them to the global
        // statistics in one part
        var globalArea = AreaData(0, 0, 0, 0, 0)
        var externalArea = AreaData(0, 0, 0, 0, 0)
//        var total = 0
//        var excess = 0
//        var transfer = 0
//        var generation = 0
//        var drain = 0
        var globalGhg = 0.0
        var externalGhg = 0.0

        round.areas.foreach {
          case (a, ad) ⇒
            val external = world.areaById(a).get.external

            areas(a) += ad

            val ag = sourcesByAreaId(a).map(s ⇒ round.units(s.id).used * s.ghgPerCapacity).sum
            areas(a).ghg += ag

            if (external) {
              externalArea += ad
              externalGhg += ag
            } else {
              globalArea += ad
              globalGhg += ag
            }
        }

        unitsByType.foreach {
          case (t, units) ⇒
            val (ud, ghg) = units.foldLeft((UnitData(0, 0, 0), 0.0)) {
              case ((ud, ghg), u) ⇒ (
                ud + round.units(u),
                sourcesById.get(u) match {
                  case Some(s) ⇒ ghg + round.units(u).used * s.ghgPerCapacity
                  case None ⇒ ghg
                })
            }
            types(t) += ud
            types(t).ghg += ghg
            types(t).proportion += ud.used.toDouble / totalGenerated
        }

        global += globalArea
        global.ghg += globalGhg

        external += externalArea
        external.ghg += externalGhg

        round.units.foreach {
          case (s, sd) if sources.contains(s) ⇒
            sources(s) += sd
            sources(s).ghg += sd.used * sourcesById(s).ghgPerCapacity
            sources(s).proportion += sd.used.toDouble / totalGenerated

//            println(s"${sources(s).proportion * 100} ${sd.used} / $totalGenerated")
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
  def apply(world: World, history: Int = 0) =
    new SimulationCollector(world, history)

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
      f"    of total  ${s.proportion * 100}%s%%",
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
