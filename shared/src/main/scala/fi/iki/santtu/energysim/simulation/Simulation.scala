package fi.iki.santtu.energysim.simulation

import fi.iki.santtu.energysim.model.World

case class Round(
                  /**
                    * Total power balance of an area, where == 0 meaning the
                    * area consumes exactly the amount it produces and
                    * transfers in/out, if >0 then the area has excess
                    * capacity and if <0 then the area is having a black-out.
                    */
                  areaPower: Seq[Double],

                  /**
                    * The amount of power *generated in this area* that is
                    * transferred elsewhere. By necessity this is the sum
                    * of all lines connected to this area, but is
                    * separately told here. If == 0, then the area's transfer
                    * input and output are equal, if <0 then power is
                    * transferred out of this area and if >0 this area is
                    * a net importer.
                    */
                  areaTransfer: Seq[Double],

                  /**
                    * The power attributed to each unit. The interpretation
                    * depends on the unit type:
                    *
                    * - lines: the capacity the line has for transfers
                    * - sources: the power capacity available for generation
                    * - drains: the amount of power required by the drain
                    */
                  unitPower: Seq[Double],

                  /**
                    * The amount of power transferred in/out of this unit,
                    * with interpretation depending on the unit type:
                    *
                    * - lines: the amount of capacity used, if >0 this means
                    *   power was transferred from first to second area,
                    *   if <0 then the opposite (to first from second)
                    * - sources: the amount of power used from the source
                    * - drains: the amount of power consumed by this drain
                    */
                  unitTransfer: Seq[Double])

case class Result(world: World, rounds: Seq[Round])

trait Simulation {
  def simulate(world: World, rounds: Int): Result
}