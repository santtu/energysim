package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model._
import io.circe.parser.decode
import io.circe._
import org.scalatest.{FlatSpec, Matchers}

class JsonDecoderSpec extends FlatSpec with Matchers {
  def dec(str: String): World = {
    val w = JsonDecoder.decode(str)
    // json should always be roundtrippable, verify that
    JsonDecoder.decode(enc(w).toString()) shouldBe w
    w
  }

  def enc(w: World) = {
    decode[Json](JsonDecoder.encode(w)).right.get
  }

  val complexData =
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

  "JSON world decoder" should "work with empty input" in {
    val world = dec("{}")
    world.name should not be empty
    world.areas shouldBe empty
    world.lines shouldBe empty
    world.units shouldBe empty
  }

  it should "handle empty areas" in {
    val world = dec("""{"areas":{"a":{},"b":{},"c":{}}}""")
    world.areas.size shouldBe 3
    world.areas.forall(_.id.length > 0) shouldBe true
    world.lines shouldBe empty
    world.units shouldBe empty
  }

  it should "match lines and areas" in {
    val world = dec("""{"areas":{"a":{},"b":{}}, "lines":[{"areas":["a","b"]}]}""")
    world.areas.size shouldBe 2
    world.units.size shouldBe 1
    world.lines.size shouldBe 1
    world.units shouldBe world.lines
    val (a, b) = (world.areas(0), world.areas(1))
    a.id shouldBe "a"
    b.id shouldBe "b"
    world.lines(0).areas.productIterator.toSeq shouldBe Seq(a, b)
  }

  it should "handle complex case" in {
    val data = complexData
    val world = dec(data)

    world.areas.size shouldBe 3
    world.units.size shouldBe 11
    world.lines.size shouldBe 3
    world.areas.map(a ⇒ (a.drains.size, a.sources.size)).toSet shouldBe Set((1, 1), (2, 1), (1, 2))

    val (n, s, e) = (world.areas.find(_.id == "north").get,
      world.areas.find(_.id == "south").get,
      world.areas.find(_.id == "east").get)
    val Seq(ns, se, ne) = world.lines

    ns.areas shouldBe (n, s)
    se.areas shouldBe (s, e)
    ne.areas shouldBe (n, e)
  }


  it should "handle type specifications" in {
    val data = """{
                  |    "name": "simple model",
                  |    "types": {
                  |        "a": {
                  |            "model": "constant",
                  |            "size": 0
                  |        },
                  |        "b1": {
                  |            "model": "uniform",
                  |            "size": 100
                  |        },
                  |        "b2": {
                  |            "data": 0.8,
                  |            "model": "uniform",
                  |            "size": 100
                  |        },
                  |        "b3": {
                  |            "data": [
                  |                0,
                  |                0.9
                  |            ],
                  |            "model": "uniform",
                  |            "size": 100
                  |        },
                  |        "c": {
                  |            "data": [
                  |                [
                  |                    1,
                  |                    0
                  |                ],
                  |                [
                  |                    1,
                  |                    1
                  |                ]
                  |            ],
                  |            "model": "step"
                  |        }
                  |    }
                  |}""".stripMargin
    val world = dec(data)
    world.areas.size shouldBe 0
    world.units.size shouldBe 0
    world.types.size shouldBe 5
  }



  it should "recodnize disabled units" in {
    val data =
      """{
        |    "areas": {
        |        "a": {
        |            "drains": [
        |                {
        |                    "capacity": 1000,
        |                    "disabled": true,
        |                    "id": "a-2",
        |                    "type": "constant"
        |                }
        |            ],
        |            "sources": [
        |                {
        |                    "capacity": 1000,
        |                    "disabled": true,
        |                    "id": "a-1",
        |                    "type": "constant"
        |                }
        |            ]
        |        },
        |        "b": {
        |            "name": "b area"
        |        }
        |    },
        |    "lines": [
        |        {
        |            "areas": [
        |                "a",
        |                "b"
        |            ],
        |            "capacity": 1000,
        |            "disabled": true,
        |            "id": "l-1"
        |        }
        |    ]
        |}""".stripMargin

    val w = dec(data)
    w.units(0).disabled shouldBe true
    w.units(1).disabled shouldBe true
    w.lines(0).disabled shouldBe true
  }

  "JSON world encoder" should "serialize an empty world" in {
    val w = World(name="a world")
    val j = enc(w)
    j shouldBe Json.fromFields(Seq(
      "name" → Json.fromString("a world"),
      "areas" → Json.fromFields(Seq()),
      "lines" → Json.fromValues(Seq()),
      "types" → Json.fromFields(Seq())
    ))
    dec(j.toString()) shouldBe w
  }

  def d(ds: Double*) = Json.fromValues(ds.map(Json.fromDoubleOrNull) )

  it should "serialize types" in {
    val w = World(name="a world",
      types = Seq(
        CapacityType("t-const", None, 0, ConstantCapacityModel),
        CapacityType("t-uni", None, 100, UniformCapacityModel),
        CapacityType("t-step", None, 0, StepCapacityModel(Seq(Step(1, 0, .5), Step(2, .5, 1)))),
        CapacityType("t-scale", None, 0, ScaledCapacityModel(10.0,
          Seq(Step(1.0, 0.0, 10.0), Step(1.0, 20.0, 20.0))))
      ))
    val j = enc(w)

    j shouldBe Json.fromFields(Seq(
      "name" → Json.fromString("a world"),
      "areas" → Json.fromFields(Seq()),
      "lines" → Json.fromValues(Seq()),
      "types" → Json.fromFields(Seq(
        "t-const" → Json.fromFields(Seq(
          "name" → Json.Null,
          "size" → Json.fromInt(0),
          "model" → Json.fromString("constant"),
          "data" → Json.Null)) ,
        "t-uni" → Json.fromFields(Seq(
          "name" → Json.Null,
          "size" → Json.fromInt(100),
          "model" → Json.fromString("uniform"),
          "data" → d(0.0, 1.0))),
        "t-step" → Json.fromFields(Seq(
          "name" → Json.Null,
          "size" → Json.fromInt(0),
          "model" → Json.fromString("step"),
          "data" → Json.fromValues(Seq(
            d(1.0, 0.0, 0.5),
            d(2.0, 0.5, 1.0))))),
        "t-scale" → Json.fromFields(Seq(
          "name" → Json.Null,
          "size" → Json.fromInt(0),
          "model" → Json.fromString("scaled"),
          "data" → Json.fromFields(Seq(
            "mean" → Json.fromDoubleOrNull(10.0),
            "bins" → Json.fromValues(Seq(
              d(1.0, 0.0, 10.0),
              d(1.0, 20.0, 20.0)))))))
      ))
    ))

    // of course it should be roundtrippable
    dec(j.toString()) shouldBe w
  }

  it should "serialize areas" in {
    val w = World(name="a world",
      areas = Seq(
        Area("a"),
        Area("b", sources = Seq(Source("s", None, 100, ConstantCapacityType, 1.0))))
      )
    val j = enc(w)

    j shouldBe Json.fromFields(Seq(
      "name" → Json.fromString("a world"),
      "areas" → Json.fromFields(Seq(
        "a" → Json.fromFields(Seq(
          "name" → Json.Null,
          "sources" → Json.fromValues(Seq()),
          "drains" → Json.fromValues(Seq()))),
        "b" → Json.fromFields(Seq(
          "name" → Json.Null,
          "sources" → Json.fromValues(Seq(
            Json.fromFields(Seq(
              "id" → Json.fromString("s"),
              "name" → Json.Null,
              "capacity" → Json.fromInt(100),
              "type" → Json.fromString("constant"),
              "ghg" → Json.fromDoubleOrNull(1.0),
              "disabled" → Json.fromBoolean(false))))),
          "drains" → Json.fromValues(Seq()))))),
      "lines" → Json.fromValues(Seq()),
      "types" → Json.fromFields(Seq())))

    dec(j.toString()) shouldBe w
  }

  "Complex data" should "be roundtrippable" in {
    val w = dec(complexData)
    val j = enc(w)
    val w1 = dec(j.toString())
    w1 shouldBe w
  }
}
