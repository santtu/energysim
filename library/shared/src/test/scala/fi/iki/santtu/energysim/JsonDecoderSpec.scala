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

package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model._
import io.circe.parser.decode
import io.circe._
import io.circe.parser.parse
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
      |    "name": "simple model",
      |    "version": 2
      |}""".stripMargin

  "JSON world decoder" should "work with empty input" in {
    val world = dec("{}")
    world.name should not be empty
    world.version shouldBe 1
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

    world.name shouldBe "simple model"
    world.version shouldBe 2
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
      "version" -> Json.fromInt(1),
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
        DistributionType("t-const", None, false, ConstantDistributionModel),
        DistributionType("t-uni", None, false, UniformDistributionModel),
        DistributionType("t-step", None, true, StepDistributionModel(Seq(Step(1, 0, .5), Step(2, .5, 1)))),
      ))
    val j = enc(w)

    j shouldBe Json.fromFields(Seq(
      "name" → Json.fromString("a world"),
      "version" -> Json.fromInt(1),
      "areas" → Json.fromFields(Seq()),
      "lines" → Json.fromValues(Seq()),
      "types" → Json.fromFields(Seq(
        "t-const" → Json.fromFields(Seq(
          "name" → Json.Null,
          "aggregated" → Json.fromBoolean(false),
          "model" → Json.fromString("constant"),
          "data" → Json.Null)) ,
        "t-uni" → Json.fromFields(Seq(
          "name" → Json.Null,
          "aggregated" → Json.fromBoolean(false),
          "model" → Json.fromString("uniform"),
          "data" → d(0.0, 1.0))),
        "t-step" → Json.fromFields(Seq(
          "name" → Json.Null,
          "aggregated" → Json.fromBoolean(true),
          "model" → Json.fromString("step"),
          "data" → Json.fromValues(Seq(
            d(1.0, 0.0, 0.5),
            d(2.0, 0.5, 1.0))))),
      ))))

    // of course it should be roundtrippable
    dec(j.toString()) shouldBe w
  }

  it should "serialize areas" in {
    val w = World(name="a world",
      areas = Seq(
        Area("a"),
        Area("b", sources = Seq(Source("s", None, 100, ConstantDistributionType, 1.0))))
      )
    val j = enc(w)

    j shouldBe Json.fromFields(Seq(
      "name" → Json.fromString("a world"),
      "version" -> Json.fromInt(1),
      "areas" → Json.fromFields(Seq(
        "a" → Json.fromFields(Seq(
          "name" → Json.Null,
          "sources" → Json.fromValues(Seq()),
          "drains" → Json.fromValues(Seq()),
          "external" → Json.fromBoolean(false))),
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
          "drains" → Json.fromValues(Seq()),
          "external" → Json.fromBoolean(false))))),
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

  // test of changes encoding and decoding
  "Change encoder" should "handle no changes" in {
    val j = decode[Json](JsonDecoder.encode(Changes("world", 2))).right.get
    j shouldBe parse("""{"name": "world", "version": 2, "changes": []}""").right.get
  }

  it should "encode changes correctly" in {
    val j = decode[Json](JsonDecoder.encode(
      Changes("world", 1,
        Seq(Change("id-1", None, Some(10)),
          Change("id-2", Some(false), None),
          Change("id-3", Some(true), Some(5)))))).right.get
    j shouldBe parse("""{
                "name": "world",
                "version": 1,
                "changes": [
                        {"id": "id-1", "enabled": null, "capacity": 10},
                        {"id": "id-2", "enabled": false, "capacity": null},
                        {"id": "id-3", "enabled": true, "capacity": 5}
                ]
        }""").right.get
  }

  "Change decoder" should "handle no changes" in {
    val c = JsonDecoder.decodeChanges("""{"name":"world","version":3,"changes":[]}""")
    c shouldBe Changes("world", 3, Seq.empty)
  }

  "Change decoder" should "decode changes correctly" in {
    val c = JsonDecoder.decodeChanges("""{
                "name": "world",
                "version": 1,
                "changes": [
                        {"id": "id-1", "enabled": null, "capacity": 10},
                        {"id": "id-2", "enabled": false, "capacity": null},
                        {"id": "id-3", "enabled": true, "capacity": 5}
                ]
        }""")
    c shouldBe Changes("world", 1,
      Seq(Change("id-1", None, Some(10)),
        Change("id-2", Some(false), None),
        Change("id-3", Some(true), Some(5))))
  }
}
