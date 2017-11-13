package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model._
import org.scalatest.{FlatSpec, Matchers}

class ModelSpec extends FlatSpec with Matchers {
  "Equality tests" should "work for types and models" in {
    ConstantDistributionType shouldBe ConstantDistributionType
    UniformDistributionType shouldBe UniformDistributionType

    ConstantDistributionType shouldBe DistributionType("constant", Some("Constant"), false, ConstantDistributionModel)
    ConstantDistributionType.hashCode() shouldBe DistributionType("constant", Some("Constant"), false, ConstantDistributionModel).hashCode()
    UniformDistributionType shouldBe DistributionType("uniform", Some("Uniform 0-1"), false, UniformDistributionModel)
    UniformDistributionType.hashCode() shouldBe DistributionType("uniform", Some("Uniform 0-1"), false, UniformDistributionModel).hashCode()

    DistributionType("test", None, false, ConstantDistributionModel) should not be DistributionType("jest", None, false, ConstantDistributionModel)
    DistributionType("test", None, false, ConstantDistributionModel) should not be DistributionType("test", None, false, UniformDistributionModel)
    DistributionType("test", None, false, ConstantDistributionModel) should not be DistributionType("test", None, true, ConstantDistributionModel)
    DistributionType("test", None, false, ConstantDistributionModel) should not be DistributionType("test", Some("name"), false, ConstantDistributionModel)

    DistributionType("test", None, false, StepDistributionModel(Seq(Step(1, 0, 1), Step(1, 1, 2)))) shouldBe DistributionType("test", None, false, StepDistributionModel(Seq(Step(1, 0, 1), Step(1, 1, 2))))
    DistributionType("test", None, false, StepDistributionModel(Seq(Step(1, 0, 1), Step(1, 1, 2)))).hashCode() shouldBe DistributionType("test", None, false, StepDistributionModel(Seq(Step(1, 0, 1), Step(1, 1, 2)))).hashCode()
    DistributionType("test", None, false, StepDistributionModel(Seq(Step(1, 0, 1), Step(1, 1, 2)))) should not be DistributionType("test", None, false, StepDistributionModel(Seq(Step(1, 0, 1), Step(1, 0, 2))))
  }

  it should "work for drains" in {
    Drain("id-1") shouldBe Drain("id-1")
    Drain("id-1") should not be Drain("id-1", disabled = true)
    Drain("id-1", capacity = 1) should not be Drain("id-1")
    Drain("id-1", capacity = 1) shouldBe Drain("id-1", capacity = 1)
    Drain("id-1", name = Some("name")) should not be Drain("id-1")
    Drain("id-1", name = Some("name")) shouldBe Drain("id-1", name = Some("name"))
    Drain("id-1", name = Some("name")) should not be Drain("id-1", name = Some("other name"))
    Drain("id-1", capacityType = ConstantDistributionType) shouldBe Drain("id-1", capacityType = ConstantDistributionType)
    Drain("id-1", capacityType = ConstantDistributionType) should not be Drain("id-1", capacityType = UniformDistributionType)
    Drain("id-1", capacityType = UniformDistributionType) should not be Drain("id-1", capacityType =
      DistributionType("uniform", Some("uniform"), false, UniformDistributionModel(0.5)))
  }

  it should "work for sources" in {
    Source("id-1") shouldBe Source("id-1")
    Source("id-1") should not be Source("id-1", disabled = true)
    Source("id-1", capacity = 1) should not be Source("id-1")
    Source("id-1", capacity = 1) shouldBe Source("id-1", capacity = 1)
    Source("id-1") should not be Source("id-1", ghgPerCapacity = 1.0)
    Source("id-1", ghgPerCapacity = 100.0) shouldBe Source("id-1", ghgPerCapacity = 100.0)
    Source("id-1", name = Some("name")) should not be Source("id-1")
    Source("id-1", name = Some("name")) shouldBe Source("id-1", name = Some("name"))
    Source("id-1", name = Some("name")) should not be Source("id-1", name = Some("other name"))
    Source("id-1", capacityType = ConstantDistributionType) shouldBe Source("id-1", capacityType = ConstantDistributionType)
    Source("id-1", capacityType = ConstantDistributionType) should not be Source("id-1", capacityType = UniformDistributionType)
    Source("id-1", capacityType = UniformDistributionType) should not be Source("id-1", capacityType =
      DistributionType("uniform", Some("uniform"), false, UniformDistributionModel(0.5)))
  }

  it should "work for areas" in {
    Area("id-1") shouldBe Area("id-1")
    Area("id-1", name = Some("name")) should not be Area("id-1")
    Area("id-1", name = Some("name")) shouldBe Area("id-1", name = Some("name"))
    Area("id-1", drains = Seq(Drain("id-1"))) should not be Area("id-1")
    Area("id-1", drains = Seq(Drain("id-1"))) shouldBe Area("id-1", drains = Seq(Drain("id-1")))
    Area("id-1", drains = Seq(Drain("id-1"))) should not be Area("id-1", drains = Seq(Drain("id-1", capacity = 1)))
    Area("id-1", sources = Seq(Source("id-1"))) should not be Area("id-1")
    Area("id-1", sources = Seq(Source("id-1"))) shouldBe Area("id-1", sources = Seq(Source("id-1")))
    Area("id-1", sources = Seq(Source("id-1"))) should not be Area("id-1", sources = Seq(Source("id-1", capacity = 1)))
  }

  it should "work for lines" in {
    val (a1, a2, a3) = (Area("id-1"), Area("id-2"), Area("id-3"))
    Line("id-4", area1 = a1, area2 = a2) shouldBe Line("id-4", area1 = a1, area2 = a2)
    Line("id-4", area1 = a1, area2 = a2) should not be Line("id-4", area1 = a1, area2 = a2, disabled = true)
    Line("id-4", area1 = a1, area2 = a2) shouldBe Line("id-4", area1 = a2, area2 = a1)
    Line("id-4", area1 = a1, area2 = a2) should not be Line("id-4", area1 = a1, area2 = a3)
    Line("id-4", area1 = a1, area2 = a2) should not be Line("id-4", capacity = 1, area1 = a1, area2 = a2)
    Line("id-4", area1 = a1, area2 = a2) should not be Line("id-5", area1 = a1, area2 = a2)
  }

  it  should "work for worlds" in {
    World() shouldBe World()
    World() should not be World("another name")
    World(areas = Seq(Area("id-1"))) shouldBe World(areas = Seq(Area("id-1")))
    World(areas = Seq(Area("id-1"))) should not be World()

    val (a1, a2, a3) = (Area("1"), Area("2"), Area("3"))

    World(areas = Seq(a1, a2)) shouldBe World(areas = Seq(a1, a2))
    World(areas = Seq(a1, a2)) shouldBe World(areas = Seq(a2, a1))

    val (l1, l2) = (Line("id-1", area1 = a1, area2 = a2), Line("id-2", area1 = a1, area2 = a3))

    World(areas = Seq(a1, a2, a3), lines = Seq(l1, l2)) shouldBe World(areas = Seq(a1, a2, a3), lines = Seq(l1, l2))
    World(areas = Seq(a1, a2, a3), lines = Seq(l1, l2)) shouldBe World(areas = Seq(a1, a2, a3), lines = Seq(l2, l1))

    World(types = Seq(UniformDistributionType, ConstantDistributionType)) shouldBe World(types = Seq(UniformDistributionType, ConstantDistributionType))
    World(types = Seq(UniformDistributionType, ConstantDistributionType)) shouldBe World(types = Seq(ConstantDistributionType, UniformDistributionType))
    World(types = Seq(UniformDistributionType, ConstantDistributionType)) should not be World(types = Seq(UniformDistributionType))

    // this is from a regression:
    val w1 = World(name = "simple model",
      areas = Seq(),
      types = Seq(
        DistributionType("b2", None, false, UniformDistributionModel),
        DistributionType("b3", None, false, UniformDistributionModel),
        DistributionType("a", None, false, ConstantDistributionModel),
        DistributionType("b1", None, false, UniformDistributionModel),
        DistributionType("c", None, true, StepDistributionModel(Seq(
          Step(1.0,0.0,0.0),
          Step(1.0,1.0,1.0))))),
      lines = Seq())
    
    val w2 = World(name = "simple model",
      areas = Seq(),
      types = Seq(
        DistributionType("b1", None, false, UniformDistributionModel),
        DistributionType("b2", None, false, UniformDistributionModel),
        DistributionType("a", None, false, ConstantDistributionModel),
        DistributionType("b3", None, false, UniformDistributionModel),
        DistributionType("c", None, true, StepDistributionModel(Seq(
          Step(1.0,0.0,0.0),
          Step(1.0,1.0,1.0))))),
      lines = Seq())

    w1 shouldBe w2
  }
}
