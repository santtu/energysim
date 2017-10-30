package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model._
import org.scalatest.{FlatSpec, Matchers}

class ModelSpec extends FlatSpec with Matchers {
  "Equality tests" should "work for drains" in {
    Drain() shouldBe Drain()
    Drain(capacity = 1) should not be Drain()
    Drain(capacity = 1) shouldBe Drain(capacity = 1)
    Drain(name = "name") should not be Drain()
    Drain(name = "name") shouldBe Drain(name = "name")
    Drain(name = "name") should not be Drain(name = "other name")
    Drain(capacityType = ConstantCapacityType) shouldBe Drain(capacityType = ConstantCapacityType)
    Drain(capacityType = ConstantCapacityType) should not be Drain(capacityType = UniformCapacityType)
    Drain(capacityType = UniformCapacityType) should not be Drain(capacityType =
      CapacityType("uniform", 0, UniformCapacityModel(0.5)))
  }

  it should "work for sources" in {
    Source() shouldBe Source()
    Source(capacity = 1) should not be Source()
    Source(capacity = 1) shouldBe Source(capacity = 1)
    Source() should not be Source(ghgPerCapacity = 1.0)
    Source(ghgPerCapacity = 100.0) shouldBe Source(ghgPerCapacity = 100.0)
    Source(name = "name") should not be Source()
    Source(name = "name") shouldBe Source(name = "name")
    Source(name = "name") should not be Source(name = "other name")
    Source(capacityType = ConstantCapacityType) shouldBe Source(capacityType = ConstantCapacityType)
    Source(capacityType = ConstantCapacityType) should not be Source(capacityType = UniformCapacityType)
    Source(capacityType = UniformCapacityType) should not be Source(capacityType =
      CapacityType("uniform", 0, UniformCapacityModel(0.5)))
  }

  it should "work for areas" in {
    Area() shouldBe Area()
    Area("name") should not be Area()
    Area("name") shouldBe Area("name")
    Area(drains = Seq(Drain())) should not be Area()
    Area(drains = Seq(Drain())) shouldBe Area(drains = Seq(Drain()))
    Area(drains = Seq(Drain())) should not be Area(drains = Seq(Drain(capacity = 1)))
    Area(sources = Seq(Source())) should not be Area()
    Area(sources = Seq(Source())) shouldBe Area(sources = Seq(Source()))
    Area(sources = Seq(Source())) should not be Area(sources = Seq(Source(capacity = 1)))
  }

  it should "work for lines" in {
    val (a1, a2, a3) = (Area("1"), Area("2"), Area("3"))
    Line(area1 = a1, area2 = a2) shouldBe Line(area1 = a1, area2 = a2)
    Line(area1 = a1, area2 = a2) shouldBe Line(area1 = a2, area2 = a1)
    Line(area1 = a1, area2 = a2) should not be Line(area1 = a1, area2 = a3)
    Line(area1 = a1, area2 = a2) should not be Line(capacity = 1, area1 = a1, area2 = a2)
    Line(area1 = a1, area2 = a2) should not be Line("some line", area1 = a1, area2 = a2)
  }

  it  should "work for worlds" in {
    World() shouldBe World()
    World() should not be World("another name")
    World(areas = Seq(Area())) shouldBe World(areas = Seq(Area()))
    World(areas = Seq(Area())) should not be World()

    val (a1, a2, a3) = (Area("1"), Area("2"), Area("3"))

    World(areas = Seq(a1, a2)) shouldBe World(areas = Seq(a1, a2))
    World(areas = Seq(a1, a2)) shouldBe World(areas = Seq(a2, a1))

    val (l1, l2) = (Line(area1 = a1, area2 = a2), Line(area1 = a1, area2 = a3))

    World(areas = Seq(a1, a2, a3), lines = Seq(l1, l2)) shouldBe World(areas = Seq(a1, a2, a3), lines = Seq(l1, l2))
    World(areas = Seq(a1, a2, a3), lines = Seq(l1, l2)) shouldBe World(areas = Seq(a1, a2, a3), lines = Seq(l2, l1))

  }
}
