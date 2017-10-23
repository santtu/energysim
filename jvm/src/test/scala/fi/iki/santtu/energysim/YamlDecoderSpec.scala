package fi.iki.santtu.energysim

import org.scalatest.{FlatSpec, Matchers}

class YamlDecoderSpec extends FlatSpec with Matchers {
  def decode(str: String) =  YamlDecoder.decode(str.getBytes("UTF-8"))
  "YAML world decoder" should "work with empty input" in {
    val world = decode("{}")
    world.name should not be empty
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
    world.areas.forall(_.name.length > 0) shouldBe true
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
    world.areas.map(_.name).toSet shouldBe Set("a", "b")
    world.lines(0).areas.productIterator.toSet shouldBe world.areas.toSet
  }

  it should "handle complex case" in {
    val data = """name: simple model
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
