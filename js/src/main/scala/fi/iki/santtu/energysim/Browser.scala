package fi.iki.santtu.energysim

import org.scalajs.dom

object Browser {
  def main(args: Array[String]): Unit = {
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

    println(s"World: ${world}")
    println(s"units: ${world.units}")
  }
}
