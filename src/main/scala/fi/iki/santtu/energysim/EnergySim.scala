import java.io.{BufferedInputStream, FileInputStream}
import java.nio.file.Files.readAllBytes
import java.nio.file.Paths

import breeze.linalg.DenseVector
import fi.iki.santtu.energysim.{JsonDecoder, Model, YamlDecoder}
import fi.iki.santtu.energysim.model.World

object EnergySim {
  def main(args: Array[String]): Unit = {
//    val data = readAllBytes(Paths.get("world.yml"))
//    val world = Model.from(data, YamlDecoder)
//    val data = readAllBytes(Paths.get("world.json"))

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
    val world = Model.from(data, JsonDecoder)

//    val world = Model.fromFile("world.yml")
//    val world = World("nameless")

    println(s"World: ${world}")
    println(s"units: ${world.units}")

//    import breeze.linalg._
    val x = DenseVector.zeros[Double](5)
    
  }
}
