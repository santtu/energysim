package fi.iki.santtu.energysim.simulation

import fi.iki.santtu.energysim.model.{Area, World, Unit}

case class UnitData(used: Int, excess: Int, capacity: Int) {
  require(used + excess == capacity)

  def toSeq: Seq[Int] = Seq(used, excess, capacity)
}

case class AreaData(total: Int, excess: Int, generation: Int, drain: Int, transfer: Int) {
  require(total + excess == generation + drain + transfer,
    s"$total + $excess != $generation + $drain + $transfer (${total + excess} != ${generation + drain + transfer})")
  
  require(generation >= 0)
  require(drain <= 0)
  require(total <= 0)
  //      require(excess >= 0)   // this can be <0 when transfers are pending

  def toSeq: Seq[Int] = Seq(total, excess, generation, drain, transfer)
}

case class Round(areas: Map[Area, AreaData],
                 units: Map[Unit, UnitData])

case class Result(rounds: Seq[Round])

trait Simulation {
  def simulate(world: World): Round
  def simulate(world: World, rounds: Int): Result
}