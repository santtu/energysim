package fi.iki.santtu.energysim.simulation
import fi.iki.santtu.energysim.model.{Drain, Line, Source, World}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

object ScalaSimulation extends Simulation {
  /**
    * Simulate a single round, return the result. This tries to "satisfy"
    * the area power needs and transfers with increasing the number of
    * sources used in the order of GHG per MW each source emits.
    *
    * @param world
    * @return
    */
  private def simulate(world: World): Round = {
    // first get capacities (that's the *only* random part here),
    // then prepare some data structures that help with graph
    // generation
    val capacities = world.units.map(_.capacityModel.capacity())
    val unitCapacities = capacities.zip(world.units).map { case (c, u) ⇒ u -> c.amount }.toMap

    // supersource is 0, supersink is 1, all others are +2,
    // generate area -> index map
    val areaIndex = world.areas.zipWithIndex.map { case (a, i) ⇒ a → (i + 2) }.toMap
    // maps of drains and sources to areas
    val unitArea = world.areas.flatMap { a ⇒ a.drains.map(_ → a) ++ a.sources.map(_ → a) }.toMap

    // calculate initial area power based on drain only, this is the
    // starting point for GHG/flow optimization and is retained
    // for later comparison
    val initialAreaPower = world.areas.map {
      area ⇒
        area → -area.drains.map(unitCapacities(_)).sum
    }.toMap

    // determine different GHG tiers
    val ghgLevels = world.areas.map { area => area.sources.map(_.ghgPerCapacity) }.flatten.distinct.sortWith(_ < _)

    // and all lines too
    val lineCapacity = world.lines.map {
      line ⇒
        line → unitCapacities(line)
    }.toMap

    scribe.debug(s"capacities=$capacities areaIndex=$areaIndex unitArea=$unitArea initialAreaPower=$initialAreaPower ghgLevels=$ghgLevels initialLinePower=$lineCapacity")

    // areaPower is updated until we run out of sources, or we have no
    // areas without power, for lines we calculate it from capacity
    val areaPower = mutable.Map() ++ initialAreaPower
    val linePower = mutable.Map() ++ world.lines.map(_ → 0).toMap
    var areaProduction = mutable.Map() ++ world.areas.map(_ → 0).toMap
    val sourceActive = mutable.Set[Source]()

    ghgLevels.foreach {
      case ghg if areaPower.values.forall(_ >= 0) ⇒
        scribe.info(s"Areas have power, GHG $ghg not needed.")
      case ghg ⇒
        scribe.debug(s"Some areas without power, GHG $ghg being used")

        // add sources for each area that match this ghg
        world.areas.foreach {
          area ⇒ area.sources.foreach {
            case source if source.ghgPerCapacity == ghg ⇒
              areaPower(area) += unitCapacities(source)
              areaProduction(area) += unitCapacities(source)
              sourceActive.add(source)
            case _ ⇒
          }
        }

        val graph = EdmondsKarp()

        // now we can add each area to either supersource or supersink
        areaPower.foreach {
          case (area, power) if power < 0 ⇒
            graph.add(areaIndex(area), 1, -power)
          case (area, power) if power >= 0 ⇒
            graph.add(0, areaIndex(area), power)
        }

        // XXX TODO we could optimize a lot of index lookups away
        // by having all of the information in vectors

        // and add lines .. remember that <0 linePower is from _2 to _1,
        // and >0 from _1 to _2
        world.lines.foreach {
          line ⇒
            val power = linePower(line)
            val capacity = lineCapacity(line)
            val (ai1, ai2) = (areaIndex(line.areas._1), areaIndex(line.areas._2))

            graph.add(ai1, ai2, capacity - power)
            graph.add(ai2, ai1, capacity + power)
        }

        val result = graph.solve(0, 1)
        scribe.debug(s"graph=$graph result=$result")

        // go through each line and update area power and line power
        // values
        world.lines.foreach {
          line ⇒
            val (a1, a2) = (line.areas._1, line.areas._2)
            val (ai1, ai2) = (areaIndex(a1), areaIndex(a2))
            val flow = result.flow(ai1, ai2)

            scribe.info(s"flow $a1->$a2=$flow prior: $a1=${areaPower(a1)} $a2=${areaPower(a2)} $line=${linePower(line)}")
            assert(flow == -result.flow(ai2, ai1))

            areaPower(a1) -= flow
            areaPower(a2) += flow

            // first->second
            linePower(line) += flow

            scribe.info(s"after update: $a1=${areaPower(a1)} $a2=${areaPower(a2)} $line=${linePower(line)}")
        }
    }

    // return format includes (used, capacity, excess) for each source,
    // (used, 0, 0) for drains, (used, capacity, excess) for lines
    // (where used + excess = capacity, and
    // used>0 if from first->second and used<0 if second->first).
    //
    // (total, generation, drain, excess, transfers) for areas (again,
    // total + excess = generation + drain + transfers) and

    case class UnitData(used: Int, excess: Int, capacity: Int) {
      require(used + excess == capacity)
    }
    case class AreaData(total: Int, excess: Int, generation: Int, drain: Int, transfer: Int) {
      require(total + excess == generation + drain + transfer)
      require(generation >= 0)
      require(drain <= 0)
//      require(excess >= 0)   // this can be <0 when transfers are pending
    }

    // initially it's all power drain and no generation
    val areaData = mutable.Map() ++ world.areas.map {
      area ⇒ area → AreaData(
        total = initialAreaPower(area),
        generation = 0,
        drain = initialAreaPower(area),
        excess = 0,
        transfer = 0)
    }.toMap

    val unitData = mutable.Map() ++ world.units.map {
      case unit: Drain ⇒ unit →
        UnitData(used = -unitCapacities(unit),
          capacity = -unitCapacities(unit),
          excess = 0)
      case unit ⇒ unit → UnitData(
        used = 0,
        capacity = unitCapacities(unit),
        excess = unitCapacities(unit))
    }.toMap

    // update transfers first as this is later used when calculating
    // source needs
    linePower.foreach {
      case (line, transfer) if transfer != 0 ⇒
        val (a1, a2) = (line.areas._1, line.areas._2)
        val (ad1, ad2) = (areaData(a1), areaData(a2))
        val ud = unitData(line)

        unitData(line) = ud.copy(
          used = ud.used + transfer,
          excess = ud.excess - transfer)

        scribe.debug(s"transfer out $a1: ad1=$ad1 transfer=$transfer")
        areaData(a1) = ad1.copy(
          transfer = ad1.transfer - transfer,
          excess = ad1.excess - transfer)

        scribe.debug(s"transfer in $a2: ad2=$ad2 transfer=$transfer")
        areaData(a2) = ad2.copy(
          total = ad2.total + transfer,
          transfer = ad2.transfer + transfer)

        scribe.debug(s"... $a1=${areaData(a1)} $a2=${areaData(a2)}")
        
      case _ ⇒
    }

    scribe.debug(s"areaProduction=$areaProduction")

    // update unitdata for each source for each active source --
    // we need to step through ghg levels as some of the most polluting
    // sources are not fully used
    ghgLevels.foreach {
      case ghg ⇒
        sourceActive.filter(_.ghgPerCapacity == ghg).foreach {
          source ⇒
            val area = unitArea(source)
            val ad = areaData(area)
            val ud = unitData(source)

            // this is the amount of power we know we have to generate,
            // and this includes local drain and transfers
            val missing = areaProduction(area) - ad.generation - ad.excess

            // this is the amount we need to satisfy any (not yet met)
            // local demand
            val need = -ad.total

            // this is the amount we use of this source for local
            // consumption (rest is left as excess)
            val used = math.min(need, ud.excess)

            // and this is excess capacity not used for local
            // consumption and available for transfers out of this area
            val excess = ud.excess - used

            scribe.debug(s"source $source in $area: ad=$ad ud=$ud missing=$missing need=$need used=$used excess=$excess")

            assert(need >= 0)

            areaData(area) = ad.copy(
              total = ad.total + used,
              generation = ad.generation + used + excess,
              excess = ad.excess + excess)

            unitData(source) = ud.copy(
              used = ud.used + used,
              excess = ud.excess - used
            )

            scribe.debug(s"... updated to ad=${areaData(area)} ud=${unitData(source)}")
        }
    }


    val loss = areaPower.values.filter(_ < 0).sum
    scribe.info(s"Power loss value is $loss")
    scribe.info(s"areaData=$areaData unitData=$unitData")

    Round(Seq.empty[Double], Seq.empty[Double], Seq.empty[Double], Seq.empty[Double])
  }

  override def simulate(world: World, nrounds: Int): Result = {
    Result(world, (1 to nrounds).map(i ⇒ simulate(world)))
  }
}
