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
    val world = decode("""areas: [{}, {}, {}]""")
    world.areas.size shouldBe 3
    world.areas.forall(_.name.length > 0) shouldBe true
    world.lines shouldBe empty
    world.units shouldBe empty
  }

  it should "match lines and areas" in {
    val world = decode(
      """areas:
        |  - name: a
        |  - name: b
        |lines:
        |  - areas: [a, b]""".stripMargin)
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
    val data = """name: simple model
                 |areas:
                 |  - name: north
                 |    sources:
                 |      - name: hydro
                 |        capacity: [uniform, 4000, 10000]
                 |        ghg: 0
                 |    drains:
                 |      - name: variant
                 |        capacity: [beta, 500, 3, 5]
                 |  - name: south
                 |    drains:
                 |      - name: general
                 |        capacity: [uniform, 3000, 5000]
                 |      - name: variant
                 |        capacity: [beta, 5000, 3, 5]
                 |    sources:
                 |      - name: coal
                 |        capacity: [uniform, 750, 1000]
                 |        ghg: 10
                 |  - name: east
                 |    drains:
                 |      - name: variant
                 |        capacity: [beta, 250, 3, 5]
                 |    sources:
                 |      - name: wind
                 |        capacity: [uniform, 200, 1000]
                 |      - name: gas
                 |        capacity: [beta, 1000, 10, 2]
                 |        ghg: 1
                 |lines:
                 |  - areas: [north, south]
                 |    capacity: [constant, 5000]
                 |  - areas: [south, east]
                 |    capacity: [constant, 2000]
                 |  - areas: [north, east]
                 |    capacity: [constant, 500]
                 |""".stripMargin
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
