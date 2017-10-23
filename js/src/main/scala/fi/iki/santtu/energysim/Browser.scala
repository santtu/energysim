package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model.World
import fi.iki.santtu.energysim.simulation.ScalaSimulation

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@JSExportTopLevel("EnergySim")
object Browser {
  val data = """{
               |    "areas": [
               |        {
               |            "drains": [
               |                {
               |                    "capacity": [
               |                        "beta",
               |                        500,
               |                        3,
               |                        5
               |                    ],
               |                    "name": "variant"
               |                }
               |            ],
               |            "name": "north",
               |            "sources": [
               |                {
               |                    "capacity": [
               |                        "uniform",
               |                        4000,
               |                        10000
               |                    ],
               |                    "ghg": 0,
               |                    "name": "hydro"
               |                }
               |            ]
               |        },
               |        {
               |            "drains": [
               |                {
               |                    "capacity": [
               |                        "uniform",
               |                        3000,
               |                        5000
               |                    ],
               |                    "name": "general"
               |                },
               |                {
               |                    "capacity": [
               |                        "beta",
               |                        5000,
               |                        3,
               |                        5
               |                    ],
               |                    "name": "variant"
               |                }
               |            ],
               |            "name": "south",
               |            "sources": [
               |                {
               |                    "capacity": [
               |                        "uniform",
               |                        750,
               |                        1000
               |                    ],
               |                    "ghg": 10,
               |                    "name": "coal"
               |                }
               |            ]
               |        },
               |        {
               |            "drains": [
               |                {
               |                    "capacity": [
               |                        "beta",
               |                        250,
               |                        3,
               |                        5
               |                    ],
               |                    "name": "variant"
               |                }
               |            ],
               |            "name": "east",
               |            "sources": [
               |                {
               |                    "capacity": [
               |                        "uniform",
               |                        200,
               |                        1000
               |                    ],
               |                    "name": "wind"
               |                },
               |                {
               |                    "capacity": [
               |                        "beta",
               |                        1000,
               |                        10,
               |                        2
               |                    ],
               |                    "ghg": 1,
               |                    "name": "gas"
               |                }
               |            ]
               |        }
               |    ],
               |    "lines": [
               |        {
               |            "areas": [
               |                "north",
               |                "south"
               |            ],
               |            "capacity": [
               |                "constant",
               |                5000
               |            ]
               |        },
               |        {
               |            "areas": [
               |                "south",
               |                "east"
               |            ],
               |            "capacity": [
               |                "constant",
               |                2000
               |            ]
               |        },
               |        {
               |            "areas": [
               |                "north",
               |                "east"
               |            ],
               |            "capacity": [
               |                "constant",
               |                500
               |            ]
               |        }
               |    ],
               |    "name": "simple model"
               |}""".stripMargin.getBytes("UTF-8")

  @JSExport("defaultWorld")
  val world = Model.from(data, JsonDecoder)

  @JSExport
  def run(rounds: Int, world: World = world): Unit = {
    val simulator = ScalaSimulation
    val collector = SimulationCollector(world)

    val t0 = System.nanoTime()
    val result = simulator.simulate(world, rounds)
    val t1 = System.nanoTime()
    val ms = (t1 - t0).toDouble / 1e6
    collector += result

    println(f"Simulation ${rounds} took ${ms / 1000.0}%.3f s, ${ms / rounds}%.2f ms/round")

    print(SimulationCollector.summary(collector))
  }

  def main(args: Array[String]): Unit = {
    println(s"World: ${world}")
    println(s"units: ${world.units}")

    run(1000)
 }
}
