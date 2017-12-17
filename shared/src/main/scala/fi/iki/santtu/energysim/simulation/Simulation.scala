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

import fi.iki.santtu.energysim.model.{Area, World, Unit}

case class UnitData(used: Int, excess: Int, capacity: Int) {
  require(used + excess == capacity,
    s"$used + $excess (=${used + excess}) != $capacity")

  def toSeq: Seq[Int] = Seq(used, excess, capacity)

  def +(o: UnitData) =
    UnitData(used + o.used, excess + o.excess, capacity + o.capacity)
}

case class AreaData(total: Int, excess: Int, generation: Int, drain: Int, transfer: Int) {
  require(total + excess == generation + drain + transfer,
    s"$total + $excess != $generation + $drain + $transfer (${total + excess} != ${generation + drain + transfer})")
  
  require(generation >= 0, "drain cannot be negative ($generation)")
  require(drain <= 0, "drain cannot be positive ($drain)")
  require(total <= 0, "total area balance cannot be positive ($total)")
  //      require(excess >= 0)   // this can be <0 when transfers are pending

  def toSeq: Seq[Int] = Seq(total, excess, generation, drain, transfer)

  def +(o: AreaData) =
    AreaData(total + o.total, excess + o.excess, generation + o.generation, drain + o.drain, transfer + o.transfer)
}

case class Round(areas: Map[String, AreaData],
                 units: Map[String, UnitData])

case class Result(rounds: Seq[Round])

trait Simulation {
  def simulate(world: World): Round
  def simulate(world: World, rounds: Int): Result
}