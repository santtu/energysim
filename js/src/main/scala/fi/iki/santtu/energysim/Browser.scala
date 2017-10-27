package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model.World
import fi.iki.santtu.energysim.simulation.ScalaSimulation

import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel, JSGlobal, JSImport}

@JSImport("bootstrap", JSImport.Namespace)
@js.native
object bootstrap extends js.Object {
}

// @JSExportTopLevel("EnergySim")
// object Browser {
//   val data = """{
//                |    "areas": {
//                |        "east": {
//                |            "drains": [
//                |                {
//                |                    "capacity": 250,
//                |                    "name": "variant",
//                |                    "type": "city"
//                |                }
//                |            ],
//                |            "sources": [
//                |                {
//                |                    "capacity": 1000,
//                |                    "name": "wind",
//                |                    "type": "uniform"
//                |                },
//                |                {
//                |                    "capacity": 1000,
//                |                    "ghg": 1,
//                |                    "name": "gas",
//                |                    "type": "uniform"
//                |                }
//                |            ]
//                |        },
//                |        "north": {
//                |            "drains": [
//                |                {
//                |                    "capacity": 500,
//                |                    "name": "variant",
//                |                    "type": "city"
//                |                }
//                |            ],
//                |            "sources": [
//                |                {
//                |                    "capacity": 10000,
//                |                    "ghg": 0,
//                |                    "name": "hydro",
//                |                    "type": "hydro"
//                |                }
//                |            ]
//                |        },
//                |        "south": {
//                |            "drains": [
//                |                {
//                |                    "capacity": 5000,
//                |                    "name": "general",
//                |                    "type": "uniform"
//                |                },
//                |                {
//                |                    "capacity": 5000,
//                |                    "name": "variant",
//                |                    "type": "city"
//                |                }
//                |            ],
//                |            "sources": [
//                |                {
//                |                    "capacity": 1000,
//                |                    "ghg": 10,
//                |                    "name": "coal",
//                |                    "type": "uniform"
//                |                }
//                |            ]
//                |        }
//                |    },
//                |    "lines": [
//                |        {
//                |            "areas": [
//                |                "north",
//                |                "south"
//                |            ],
//                |            "capacity": 5000
//                |        },
//                |        {
//                |            "areas": [
//                |                "south",
//                |                "east"
//                |            ],
//                |            "capacity": 2000
//                |        },
//                |        {
//                |            "areas": [
//                |                "north",
//                |                "east"
//                |            ],
//                |            "capacity": 500
//                |        }
//                |    ],
//                |    "name": "simple model",
//                |    "types": {
//                |        "city": {
//                |            "data": [
//                |                3,
//                |                5
//                |            ],
//                |            "model": "beta",
//                |            "size": 0
//                |        },
//                |        "hydro": {
//                |            "data": [
//                |                [
//                |                    0.1,
//                |                    0,
//                |                    0
//                |                ],
//                |                [
//                |                    0.9,
//                |                    1,
//                |                    1
//                |                ]
//                |            ],
//                |            "model": "step",
//                |            "size": 25
//                |        }
//                |    }
//                |}""".stripMargin.getBytes("UTF-8")

//   @JSExport("defaultWorld")
//   val world = Model.from(data, JsonDecoder)

//   @JSExport
//   def run(rounds: Int, world: World = world): Unit = {
//     val simulator = ScalaSimulation
//     val collector = SimulationCollector(world)

//     val t0 = System.nanoTime()
//     val result = simulator.simulate(world, rounds)
//     val t1 = System.nanoTime()
//     val ms = (t1 - t0).toDouble / 1e6
//     collector += result

//     println(f"Simulation ${rounds} took ${ms / 1000.0}%.3f s, ${ms / rounds}%.2f ms/round")

//     print(SimulationCollector.summary(collector))
//   }

//   @JSExport
//   def main(args: Array[String]): Unit = {
//     println(s"bootstrap: $bootstrap")
//     println(s"World: ${world}")
//     println(s"units: ${world.units}")

//     run(1000)
//   }
// }
