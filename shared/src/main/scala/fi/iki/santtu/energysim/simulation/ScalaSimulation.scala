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

package fi.iki.santtu.energysim.simulation
import fi.iki.santtu.energysim.model._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import math.{min, max}

object ScalaSimulation extends Simulation {
  case class WorkingAreaData()
  /**
    * Simulate a single round, return the result. This tries to "satisfy"
    * the area power needs and transfers with increasing the number of
    * sources used in the order of GHG per MW each source emits.
    *
    * @param world
    * @return
    */
  def simulate(world: World): Round = {
    // each capacity type gets makeCapacity called only once, then
    // the individual capacities can be used multiple times to
    // get the actual unit value -- this is because makeCapacity
    // generates a (potentially) *stateful* capacity, whileas
    // calls to makeCapacity are guaranteed to be for independent
    // runs.
    val typeCapacity = mutable.Map.empty[DistributionType, Capacity]
    def unitCapacity(unit: Unit) =
      typeCapacity.getOrElseUpdate(unit.capacityType, unit.capacityType.makeCapacity)

    val units = mutable.Map(world.units.map {
      case u if u.disabled ⇒ u → UnitData(0, 0, 0)
      case u ⇒
        (u, unitCapacity(u).amount(u.unitCapacity)) match {
          case (d: Drain, c) ⇒ u → UnitData(-c, 0, -c)
          case (o, c) ⇒ u → UnitData(0, c, c)  // line or source
        }
    }:_*)
    val areas = mutable.Map(world.areas.map {
      a ⇒
        val drain = a.drains.map { units(_).capacity }.sum
        a → AreaData(drain, 0, 0, drain, 0)
    }:_*)

    scribe.debug(s"areas=$areas units=$units")

    // during flow calculation we continuously need to index areas, so
    // do an Area -> index map for later use. For simplicity we always
    // use 0 as supersource and 1 as supersink, so all indexes need to
    // have +2.
    val areaIndex = world.areas.zipWithIndex.map { case (a, i) ⇒ (a, i + 2) }.toMap

    // at this point areas.total.sum == units.used.sum
    assert(areas.values.map(_.total).sum == units.values.map(_.used).sum)

    // go through ghg levels, adding capacity at each step
    val ghgOrder = world.areas.flatMap(_.sources).map(_.ghgPerCapacity).distinct.sorted

    ghgOrder.foreach {
      // see if there is any need to actually add sources
      case ghg if areas.values.forall(_.total >= 0) ⇒
        scribe.debug(s"GHG $ghg not needed, all areas have power")

      case ghg ⇒
        // pick area sources with this capacity, add them to the area,
        // fulfill local needs at this point (transfers come later)
        areas.foreach {
          case (a, _) ⇒
            a.sources.collect { case s: Source if s.ghgPerCapacity == ghg ⇒ s }.foreach {
              s ⇒
                val ad = areas(a) // note: cannot use ad from earlier, since it may be modified
                val ud = units(s)
                scribe.debug(s"GHG $ghg source $s($ud) in $a($ad)")

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

                scribe.debug(s"area now: ${areas(a)}")

                // all of this capacity is now (potentially) in use
                units(s) = ud.copy(
                  used = ud.used + ud.excess,
                  excess = 0)
            }

            // each area's power generation should always be in balance
            // with its source's used power generation capacity
            assert(areas(a).generation == a.sources.map(units(_).used).sum)
        }

        scribe.debug(s"areas now: $areas")
        scribe.debug(s"units now: $units")

        // after each update, the sum of used sources should match
        // the total in area generation
        assert(units.collect { case (s: Source, u) ⇒ u.used }.sum ==
          areas.values.map(_.generation).sum)

        // ----------------- flow calculation setup -----------------

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

            if (ad.total < 0) {
              graph.add(ai, 1, -ad.total)
              scribe.debug(s"area $a($ad) into supersink, capacity ${-ad.total}")
            } else {
              graph.add(0, ai, ad.excess)
              scribe.debug(s"area $a($ad) into supersource, capacity ${ad.excess}")
            }
        }

        // then transfer lines between areas
        units.foreach {
          case (l: Line, ld) ⇒
            scribe.debug(s"adding line $l: $ld")
            val (ai1, ai2) = (areaIndex(l.area1), areaIndex(l.area2))

            // for lines we need to understand that "use" is calculated
            // as transfer from ai1 to ai2, thus positive "use" will
            // decrease capacity ai1->ai2 but it will **increase**
            // capacity on the reverse direction
            graph.add(ai1, ai2, ld.capacity - ld.used)
            graph.add(ai2, ai1, ld.capacity + ld.used)

            scribe.debug(s"$ai1->$ai2 = ${ld.capacity - ld.used}, $ai2->$ai1 = ${ld.capacity + ld.used}")

          case _ ⇒
        }

        // ----------------- flow calculation -----------------
        val result = graph.solve(0, 1)
        scribe.debug(s"graph=$graph result=$result")

        // ----------------- state updates from flow -----------------
        // go through all transfers and update line and areas
        units.foreach {
          case (l: Line, ld) ⇒
            val (a1, a2) = (l.area1, l.area2)
            val (ai1, ai2) = (areaIndex(a1), areaIndex(a2))
            val (ad1, ad2) = (areas(a1), areas(a2))
            val (flow, flow2) = (result.flow(ai1, ai2), result.flow(ai2, ai1))
            assert(flow == -flow2) // sanity check

            scribe.debug(s"line $l ($ld) flow: $flow; $a1($ad1) -> $a2($ad2)")

            // first of all, to simplify, ensure that we have always a
            // positive from from "from" to "to" for area calculations
            val (f, fd, t, td, transfer) = if (flow >= 0) (a1, ad1, a2, ad2, flow) else (a2, ad2, a1, ad1, -flow)

            // there are three cases:
            //
            // 1) we are transferring power to the area, meeting its demands
            // 2) we are transferring power *through* the area, it has no demand
            // 3) both, e.g. transfer through and local demand

            // to simplify, we'll first see what is the `to` area demand
            // and satisfy it (e.g. transfer), then any excess is put into
            // excess, not transfer

            // similarly the `from` area needs to be able to handle the two
            // situations, e.g. power is transferred through or from (or both),
            // so we'll first check what we can satistfy from local power
            // generation in the `from` area, then use excess/transfer to
            // balance it (if we are doing through-transfer it is possible
            // that we handle the *transfer out* line first)

          {
            // td.total <= 0
            val demand = min(transfer, -td.total)
            assert(demand <= transfer)
            val excess = transfer - demand

            scribe.debug(s"transfer=$transfer: $t demand=$demand $f->$t excess=$excess")

            areas(t) = td.copy(
              // part satisfying local demand
              total = td.total + demand,

              // this wasn't needed locally, so it is now counted
              // as excess
              excess = td.excess + excess,

              // total amount transferred into this area
              transfer = td.transfer + transfer
            )
          }

          {
            // this is the `from` part, we know we have transferred
            // `transfer` amount of power from here, so ..  transfer
            // (which can be temporarily negative) .. so split into
            // part that we can produce locally, and part that still
            // needs to be balanced later on transfer through total
            // (note that we should never hae a negative excess value,
            // so anything "missing" must be temporarily be marked as
            // out-of-power total in this area to get balances work
            // out correctly)

            val met = min(fd.excess, transfer)
            val unmet = transfer - met

            scribe.debug(s"transfer=$transfer: $f excess=${fd.excess} met=$met unmet=$unmet")
            require(unmet >= 0)
            require(met >= 0)

            areas(f) = fd.copy(
              total = fd.total - unmet,

              // regardless where the power comes, it must be counted
              // via excess (which can turn negative temporarily)
              excess = fd.excess - met,

              // anything not supplied locally needs to be loaned
              // from transfers
              transfer =  fd.transfer - transfer
            )
          }

            // update link capacity, this is simple since this works correctly
            // with the original `flow` value sign
            units(l) = ld.copy(
              used = ld.used + flow,
              excess = ld.excess - flow)

            scribe.debug(s"flow $flow updated: $a1(${areas(a1)}) $a2(${areas(a2)}) $l(${units(l)})")

          case _ ⇒
        }

        // ----------------- excess back-attribution -----------------

        // finally attribute all unused capacity back to local sources --
        // at this point all areas should have excess >= 0 (e.g. all
        // transfer "loans" paid back)
        assert(areas.values.forall(_.excess >= 0))

        // note that since we have *decremented* the available transfer
        // capacity, this means that any *excess* in any area now
        // can not be used later, so we can push back any capacity here
        // (instead of doing it at the end of the simulation) -- doing
        // it here means we do not have to do ghg sorting later
        areas.foreach {
          case (a, ad) if ad.excess > 0 ⇒
            var excess = ad.excess

            scribe.debug(s"area $a has excess $excess, distributing back to sources")

            a.sources.collect { case s: Source if s.ghgPerCapacity == ghg ⇒ s }.foreach {
              s ⇒
                val ud = units(s)
                val unused = min(ud.used, excess)

                scribe.debug(s"source $s($ud) unused attribution $unused")

                units(s) = ud.copy(
                  used = ud.used - unused,
                  excess = ud.excess + unused)

                excess -= unused
            }

            assert(excess == 0)

            // update area generation down, it is invariant during
            // ghg rounds -- it'll get updated later to final value
            areas(a) = ad.copy(
              generation = ad.generation - ad.excess,
              excess = 0)

          case (a, ad) ⇒
            scribe.debug(s"area $a($ad) has no excess, no need to distribute back")
        }
    }

    // finally we update each area's generation (capacity) and excess
    // to match sum of its sources
    areas.transform {
      case (a, ad) ⇒
        assert(ad.excess == 0)
        assert(a.sources.map(units(_).used).sum == ad.generation)

        val excess = a.sources.map(units(_).excess).sum
        val nad = ad.copy(
          excess = excess,
          generation = ad.generation + excess
        )
        assert(a.sources.map(units(_).capacity).sum == nad.generation)
        nad
    }

    val x = areas.map { case (a, ad) ⇒ a.id → ad }.toMap

    Round(areas.map { case (a, ad) ⇒ a.id → ad } toMap,
      units.map { case (u, ud) ⇒ u.id → ud } toMap)
  }

  override def simulate(world: World, nrounds: Int): Result = {
    Result((1 to nrounds).map(i ⇒ simulate(world)))
  }
}
