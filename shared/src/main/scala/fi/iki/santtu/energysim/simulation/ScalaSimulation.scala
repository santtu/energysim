package fi.iki.santtu.energysim.simulation
import fi.iki.santtu.energysim.model._

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
  protected[energysim] def simulate(world: World): Round = {
    val units = mutable.Map(world.units.map {
      u ⇒
        (u, u.capacityModel.capacity().amount) match {
          case (d: Drain, c) ⇒ u → UnitData(-c, 0, -c)
          case (o, c) ⇒ u → UnitData(0, c, c)
        }
    }:_*)
    val areas = mutable.Map(world.areas.map {
      a ⇒
        val drain = a.drains.map { units(_).capacity }.sum
        a → AreaData(drain, 0, 0, drain, 0)
    }:_*)

    scribe.info(s"areas=$areas units=$units")

    // during flow calculation we continuously need to index areas, so
    // do an Area -> index map for later use. For simplicity we always
    // use 0 as supersource and 1 as supersink, so all indexes need to
    // have +2.
    val areaIndex = world.areas.zipWithIndex.map { case (a, i) ⇒ (a, i + 2) }.toMap

    // at this point areas.total.sum == units.used.sum
    assert(areas.values.map(_.total).sum == units.values.map(_.used).sum)

    // go through ghg levels, adding capacity at each step
    world.areas.flatMap(_.sources).map(_.ghgPerCapacity).distinct.foreach {

      // see if there is any need to actually add sources
      case ghg if areas.values.forall(_.total >= 0) ⇒
        scribe.info(s"GHG $ghg not needed, all areas have power")

      case ghg ⇒
        // pick area sources with this capacity, add them to the area,
        // fulfill local needs at this point (transfers come later)
        areas.foreach {
          case (a, ad) ⇒
            a.sources.collect { case s: Source if s.ghgPerCapacity == ghg ⇒ s }.foreach {
              s ⇒
                val ud = units(s)
                scribe.info(s"GHG $ghg source $s($ud) in $a($ad)")

                // if area has total < 0, it needs power (up to what
                // the source can provide), otherwise it won't need any
                // local power (we put it into excess)
                val need = math.min(ud.excess, math.max(0, -ad.total))

                //                require(total + excess == generation + drain + transfer)

                // add this to the generation capacity and excess in the area
                areas(a) = ad.copy(
                  total = ad.total + need,
                  generation = ad.generation + ud.excess,
                  excess = ad.excess + ud.excess - need)

                scribe.info(s"area now: ${areas(a)}")

                // all of this capacity is now (potentially) in use
                units(s) = ud.copy(
                  used = ud.used + ud.excess,
                  excess = 0)
            }
        }

        scribe.info(s"areas now: $areas")
        scribe.info(s"units now: $units")

        // after each update, the sum of used sources should match
        // the total in area generation
        assert(units.collect { case (s: Source, u) ⇒ u.used }.sum ==
          areas.values.map(_.generation).sum)

        // ----------------- flow calculation -----------------

        // Now any local drains have been filed with local sources as
        // much as possible, the next step is how we can use this GHG
        // level to satisfy others via power transfers. For that we
        // use the Edmonds-Karp maximum flow algorithm, taking any
        // previously used transfer line capacity into account.

        val graph = EdmondsKarp()

        // links all areas to either supersink or supersource
        // depending on their total power value
        areas.foreach {
          case (a, ad) ⇒
            val ai = areaIndex(a)

            if (ad.total < 0)
              graph.add(ai, 1, -ad.total)
            else
              graph.add(0, ai, ad.total)
        }

        // then transfer lines between areas
        units.foreach {
          case (l: Line, ld) ⇒
            scribe.info(s"adding line $l: $ld")
            val (ai1, ai2) = (areaIndex(l.areas._1), areaIndex(l.areas._2))

            // for lines we need to understand that "use" is calculated
            // as transfer from ai1 to ai2, thus positive "use" will
            // decrease capacity ai1->ai2 but it will **increase**
            // capacity on the reverse direction
            graph.add(ai1, ai2, ld.capacity - ld.used)
            graph.add(ai2, ai1, ld.capacity + ld.used)

          case _ ⇒
        }

        val result = graph.solve(0, 1)
        scribe.info(s"graph=$graph result=$result")

        // go through all transfers and update line and areas
        units.foreach {
          case (l: Line, ld) ⇒
            val (a1, a2) = (l.areas._1, l.areas._2)
            val (ai1, ai2) = (areaIndex(a1), areaIndex(a2))
            val (ad1, ad2) = (areas(a1), areas(a2))
            val (flow, flow2) = (result.flow(ai1, ai2), result.flow(ai2, ai1))
            assert(flow == -flow2) // sanity check

            scribe.info(s"line $l ($ld) flow: $flow; $a1($ad1) -> $a2($ad2)")

            // update area 1, reduce excess and transfers (negative is outflow)
            // and for area 2, increase total and transfers (positive is in)
            areas(a1) = ad1.copy(
              excess = ad1.excess - flow,
              transfer = ad1.transfer - flow)

            areas(a2) = ad2.copy(
              total = ad2.total + flow,
              transfer = ad2.transfer + flow)

            // update link capacity
            units(l) = ld.copy(
              used = ld.used + flow,
              excess = ld.excess - flow)

          case _ ⇒
        }
    }

    Round(areas.toMap, units.toMap)
  }

  override def simulate(world: World, nrounds: Int): Result = {
    Result(world, (1 to nrounds).map(i ⇒ simulate(world)))
  }
}
