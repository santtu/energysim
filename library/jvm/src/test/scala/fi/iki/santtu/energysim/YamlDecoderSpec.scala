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

import fi.iki.santtu.energysim.model.{ConstantDistributionModel$, StepDistributionModel, UniformDistributionModel$}
import org.scalatest.{FlatSpec, Matchers}

class YamlDecoderSpec extends FlatSpec with Matchers {
  def decode(str: String) =  YamlDecoder.decode(str)
  "YAML world decoder" should "work with empty input" in {
    val world = decode("{}")
    world.name should not be empty
    world.version shouldBe 1
    world.areas shouldBe empty
    world.lines shouldBe empty
    world.units shouldBe empty
  }

  it should "handle empty areas" in {
    val world = decode(
      """areas:
        |  a: {}
        |  b: {}
        |  c: {}""".stripMargin)
    world.areas.size shouldBe 3
    world.areas.forall(_.id.length > 0) shouldBe true
    world.lines shouldBe empty
    world.units shouldBe empty
  }

  it should "match lines and areas" in {
    val world = decode(
      """areas:
        |  a: {}
        |  b: {}
        |lines:
        |  - areas: [a, b]""".stripMargin)
    world.areas.size shouldBe 2
    world.units.size shouldBe 1
    world.lines.size shouldBe 1
    world.units shouldBe world.lines
    // ordering is not guaranteed to be stable
    world.areas.map(_.id).toSet shouldBe Set("a", "b")
    world.lines(0).areas.productIterator.toSet shouldBe world.areas.toSet
  }

  it should "handle complex case" in {
    val data = """name: simple model
                 |version: 2
                 |areas:
                 |  north:
                 |    sources:
                 |      - name: hydro
                 |        type: uniform
                 |        capacity: 10000
                 |        ghg: 0
                 |    drains:
                 |      - name: variant
                 |        capacity: 500
                 |  south:
                 |    drains:
                 |      - name: general
                 |        type: uniform
                 |        capacity: 5000
                 |      - name: variant
                 |        capacity: 5000
                 |    sources:
                 |      - name: coal
                 |        type: uniform
                 |        capacity: 1000
                 |        ghg: 10
                 |  east:
                 |    drains:
                 |      - name: variant
                 |        capacity: 250
                 |    sources:
                 |      - name: wind
                 |        capacity: 1000
                 |        type: uniform
                 |      - name: gas
                 |        capacity: 1000
                 |        ghg: 1
                 |lines:
                 |  - areas: [north, south]
                 |    capacity: 5000
                 |  - areas: [south, east]
                 |    capacity: 2000
                 |  - areas: [north, east]
                 |    capacity: 500
                 |""".stripMargin
    val world = decode(data)

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
    val data = """name: simple model
                 |types:
                 |  a:
                 |    model: constant
                 |  b1:
                 |    model: uniform
                 |  b2:
                 |    model: uniform
                 |    aggregate: true
                 |    data: .8
                 |  b3:
                 |    model: uniform
                 |    data: [0, .9]
                 |  c:
                 |    model: step
                 |    aggregate: true
                 |    data: [[1, 0], [1, 1]]
                 |""".stripMargin
    val world = decode(data)
    world.areas.size shouldBe 0
    world.units.size shouldBe 0
    world.types.size shouldBe 5
  }

  it should "recognize disabled units" in {
    val data =
      """areas:
        |  a:
        |    sources:
        |      - id: a-1
        |        type: constant
        |        capacity: 1000
        |        disabled: true
        |    drains:
        |      - id: a-2
        |        type: constant
        |        capacity: 1000
        |        disabled: true
        |  b:
        |     name: "b area"
        |lines:
        |  - id: l-1
        |    areas: [a, b]
        |    capacity: 1000
        |    disabled: true
      """.stripMargin
    val w = decode(data)

    w.units(0).disabled shouldBe true
    w.units(1).disabled shouldBe true
    w.lines(0).disabled shouldBe true
  }
}
