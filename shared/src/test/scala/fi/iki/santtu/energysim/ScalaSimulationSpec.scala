package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model._
import fi.iki.santtu.energysim.simulation.{AreaData, ScalaSimulation, Simulation, UnitData}
import org.scalatest.{FlatSpec, Matchers}

class ScalaSimulationSpec extends AbstractSimulationSpec {
  override def simulation = ScalaSimulation
}
