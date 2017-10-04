package fi.iki.santtu.energysim.simulation
import fi.iki.santtu.energysim.model.World

import scala.concurrent.{ExecutionContext, Future}

object ScalaSimulation extends Simulation {
  /**
    * Simulate a single round, return the result.
    *
    * @param world
    * @return
    */
  private def simulate(world: World): Round = {
    val capacities = world.units.map(_.capacityModel.capacity())
    println(s"capacities=$capacities")

    Round(Seq.empty[Double], Seq.empty[Double], Seq.empty[Double], Seq.empty[Double])
  }

  override def simulate(world: World, nrounds: Int): Result = {
    Result(world, (1 to nrounds).map(i ⇒ simulate(world)))
  }
}
