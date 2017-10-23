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
    val world = decode("""{"areas":{"a":{},"b":{},"c":{}}}""")
    world.areas.size shouldBe 3
    world.areas.forall(_.name.length > 0) shouldBe true
    world.lines shouldBe empty
    world.units shouldBe empty
  }

  it should "match lines and areas" in {
    val world = decode("""{"areas":{"a":{},"b":{}}, "lines":[{"areas":["a","b"]}]}""")
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
        |    "areas": {
        |        "east": {
        |            "drains": [
        |                {
        |                    "capacity": 250,
        |                    "name": "variant"
        |                }
        |            ],
        |            "sources": [
        |                {
        |                    "capacity": 1000,
        |                    "name": "wind",
        |                    "type": "uniform"
        |                },
        |                {
        |                    "capacity": 1000,
        |                    "ghg": 1,
        |                    "name": "gas"
        |                }
        |            ]
        |        },
        |        "north": {
        |            "drains": [
        |                {
        |                    "capacity": 500,
        |                    "name": "variant"
        |                }
        |            ],
        |            "sources": [
        |                {
        |                    "capacity": 10000,
        |                    "ghg": 0,
        |                    "name": "hydro",
        |                    "type": "uniform"
        |                }
        |            ]
        |        },
        |        "south": {
        |            "drains": [
        |                {
        |                    "capacity": 5000,
        |                    "name": "general",
        |                    "type": "uniform"
        |                },
        |                {
        |                    "capacity": 5000,
        |                    "name": "variant"
        |                }
        |            ],
        |            "sources": [
        |                {
        |                    "capacity": 1000,
        |                    "ghg": 10,
        |                    "name": "coal",
        |                    "type": "uniform"
        |                }
        |            ]
        |        }
        |    },
        |    "lines": [
        |        {
        |            "areas": [
        |                "north",
        |                "south"
        |            ],
        |            "capacity": 5000
        |        },
        |        {
        |            "areas": [
        |                "south",
        |                "east"
        |            ],
        |            "capacity": 2000
        |        },
        |        {
        |            "areas": [
        |                "north",
        |                "east"
        |            ],
        |            "capacity": 500
        |        }
        |    ],
        |    "name": "simple model"
        |}""".stripMargin
    val world = decode(data)

    world.areas.size shouldBe 3
    world.units.size shouldBe 11
    world.lines.size shouldBe 3
    world.areas.map(a ⇒ (a.drains.size, a.sources.size)).toSet shouldBe Set((1, 1), (2, 1), (1, 2))

    val (n, s, e) = (world.areas.find(_.name == "north").get,
      world.areas.find(_.name == "south").get,
      world.areas.find(_.name == "east").get)
    val Seq(ns, se, ne) = world.lines

    ns.areas shouldBe (n, s)
    se.areas shouldBe (s, e)
    ne.areas shouldBe (n, e)
  }
}
