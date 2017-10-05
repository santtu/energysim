package fi.iki.santtu.energysim

import org.scalatest.{FlatSpec, Matchers}

class JsonDecoderSpec extends FlatSpec with Matchers {
  def decode(str: String) =  JsonDecoder.decode(str.getBytes("UTF-8"))
  "JSON world decoder" should "work with empty input" in {
    val world = decode("{}")
    world.name should not be empty
    world.areas shouldBe empty
    world.lines shouldBe empty
    world.units shouldBe empty
  }

  it should "handle empty areas" in {
    val world = decode("""{"areas":[{},{},{}]}""")
    world.areas.size shouldBe 3
    world.areas.forall(_.name.length > 0) shouldBe true
    world.lines shouldBe empty
    world.units shouldBe empty
  }

  it should "match lines and areas" in {
    val world = decode("""{"areas":[{"name":"a"},{"name":"b"}],"lines":[{"areas":["a","b"]}]}""")
    world.areas.size shouldBe 2
    world.units.size shouldBe 1
    world.lines.size shouldBe 1
    world.units shouldBe world.lines
    val (a, b) = (world.areas(0), world.areas(1))
    a.name shouldBe "a"
    b.name shouldBe "b"
    world.lines(0).areas.productIterator.toSeq shouldBe Seq(a, b)
  }

  it should "handle complex case" in {
    val data =
      """{
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
                 |}""".stripMargin
    val world = decode(data)

    world.areas.size shouldBe 3
    world.units.size shouldBe 11
    world.lines.size shouldBe 3
    world.areas.map(a ⇒ (a.drains.size, a.sources.size)) shouldBe Seq((1, 1), (2, 1), (1, 2))

    val Seq(n, s, e) = world.areas
    val Seq(ns, se, ne) = world.lines

    ns.areas shouldBe (n, s)
    se.areas shouldBe (s, e)
    ne.areas shouldBe (n, e)
  }
}
