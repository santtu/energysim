package fi.iki.santtu.energysim.simulation
import fi.iki.santtu.energysim.model.{Drain, Line, Source, World}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

object ScalaSimulation extends Simulation {
  /**
    * Simulate a single round, return the result.
    *
    * @param world
    * @return
    */
  private def simulate(world: World): Round = {
    val graph = EdmondsKarp()
    val capacities = world.units.map(_.capacityModel.capacity())
    scribe.debug(s"capacities=$capacities")

    // supersource is 0, supersink is 1, all others are +2,
    // generate area -> index map
    val areaIndex = world.areas.zipWithIndex.map { case (a, i) ⇒ a → (i + 2) }.toMap
    val areaPower = mutable.Map(world.areas.map { _ → 0 }:_*)

    // maps of drains and sources to areas
    val unitArea = world.areas.flatMap { a ⇒ a.drains.map(_ → a) ++ a.sources.map(_ → a) }.toMap

    capacities.zipWithIndex.foreach {
      case (capacity, i) ⇒
        world.units(i) match {
          case drain: Drain ⇒ areaPower(unitArea(drain)) -= capacity.amount
          case source: Source ⇒ areaPower(unitArea(source)) += capacity.amount
          case _ ⇒
        }
    }

    scribe.debug(s"areaIndex=$areaIndex areaPower=$areaPower")

    // connect areas to either supersource or supersink
    areaPower.foreach {
      case (area, power) if power < 0 ⇒
        graph.add(areaIndex(area), 1, -power)
      case (area, power) if power >= 0 ⇒
        graph.add(0, areaIndex(area), power)
    }

    // lines
    capacities.zipWithIndex.foreach {
      case (capacity, i) ⇒
        world.units(i) match {
          case line: Line ⇒
            assert(world.areas.contains(line.areas._1))
            assert(world.areas.contains(line.areas._2))

            val (ai1, ai2) = (areaIndex(line.areas._1), areaIndex(line.areas._2))

            graph.add(ai1, ai2, capacity.amount)
            graph.add(ai2, ai1, capacity.amount)

          case _ ⇒
        }
    }

    val result = graph.solve(0, 1)

    scribe.debug(s"graph=$graph result=$result")

    // XXX NOT FULL IMPLEMENTATION

    Round(Seq.empty[Double], Seq.empty[Double], Seq.empty[Double], Seq.empty[Double])
  }

  override def simulate(world: World, nrounds: Int): Result = {
    Result(world, (1 to nrounds).map(i ⇒ simulate(world)))
  }
}
