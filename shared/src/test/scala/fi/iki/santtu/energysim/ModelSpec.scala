package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model._
import org.scalatest.{FlatSpec, Matchers}

class ModelSpec extends FlatSpec with Matchers {
  def eq(a: Any, b: Any) = {
    a shouldBe b
    a.hashCode() shouldBe b.hashCode()
  }

  def ne(a: Any, b: Any) = {
    a should not be b
    a.hashCode() should not be b.hashCode()
  }

  "Equality tests" should "work for types and models" in {
    eq(ConstantDistributionType, ConstantDistributionType)
    eq(UniformDistributionType, UniformDistributionType)

    eq(ConstantDistributionType, DistributionType("constant", Some("Constant"), false, ConstantDistributionModel))
    eq(ConstantDistributionType.hashCode(), DistributionType("constant", Some("Constant"), false, ConstantDistributionModel).hashCode())
    eq(UniformDistributionType, DistributionType("uniform", Some("Uniform 0-1"), false, UniformDistributionModel))
    eq(UniformDistributionType.hashCode(), DistributionType("uniform", Some("Uniform 0-1"), false, UniformDistributionModel).hashCode())

    ne(DistributionType("test", None, false, ConstantDistributionModel), DistributionType("jest", None, false, ConstantDistributionModel))
    ne(DistributionType("test", None, false, ConstantDistributionModel), DistributionType("test", None, false, UniformDistributionModel))
    ne(DistributionType("test", None, false, ConstantDistributionModel), DistributionType("test", None, true, ConstantDistributionModel))
    ne(DistributionType("test", None, false, ConstantDistributionModel), DistributionType("test", Some("name"), false, ConstantDistributionModel))

    eq(DistributionType("test", None, false, StepDistributionModel(Seq(Step(1, 0, 1), Step(1, 1, 2)))), DistributionType("test", None, false, StepDistributionModel(Seq(Step(1, 0, 1), Step(1, 1, 2)))))
    eq(DistributionType("test", None, false, StepDistributionModel(Seq(Step(1, 0, 1), Step(1, 1, 2)))).hashCode(), DistributionType("test", None, false, StepDistributionModel(Seq(Step(1, 0, 1), Step(1, 1, 2)))).hashCode())
    ne(DistributionType("test", None, false, StepDistributionModel(Seq(Step(1, 0, 1), Step(1, 1, 2)))), DistributionType("test", None, false, StepDistributionModel(Seq(Step(1, 0, 1), Step(1, 0, 2)))))
  }

  it should "work for drains" in {
    eq(Drain("id-1"), Drain("id-1"))
    ne(Drain("id-1"), Drain("id-1", disabled = true))
    ne(Drain("id-1", capacity = 1), Drain("id-1"))
    eq(Drain("id-1", capacity = 1), Drain("id-1", capacity = 1))
    ne(Drain("id-1", name = Some("name")), Drain("id-1"))
    eq(Drain("id-1", name = Some("name")), Drain("id-1", name = Some("name")))
    ne(Drain("id-1", name = Some("name")), Drain("id-1", name = Some("other name")))
    eq(Drain("id-1", capacityType = ConstantDistributionType), Drain("id-1", capacityType = ConstantDistributionType))
    ne(Drain("id-1", capacityType = ConstantDistributionType),
      Drain("id-1", capacityType = UniformDistributionType))
    ne(Drain("id-1", capacityType = UniformDistributionType),
      Drain("id-1", capacityType = DistributionType("uniform", Some("uniform"), false, UniformDistributionModel(0.5))))
  }

  it should "work for sources" in {
    eq(Source("id-1"), Source("id-1"))
    ne(Source("id-1"), Source("id-1", disabled = true))
    ne(Source("id-1", capacity = 1), Source("id-1"))
    eq(Source("id-1", capacity = 1), Source("id-1", capacity = 1))
    ne(Source("id-1"), Source("id-1", ghgPerCapacity = 1.0))
    eq(Source("id-1", ghgPerCapacity = 100.0), Source("id-1", ghgPerCapacity = 100.0))
    ne(Source("id-1", name = Some("name")), Source("id-1"))
    eq(Source("id-1", name = Some("name")), Source("id-1", name = Some("name")))
    ne(Source("id-1", name = Some("name")), Source("id-1", name = Some("other name")))
    eq(Source("id-1", capacityType = ConstantDistributionType), Source("id-1", capacityType = ConstantDistributionType))
    ne(Source("id-1", capacityType = ConstantDistributionType), Source("id-1", capacityType = UniformDistributionType))
    ne(Source("id-1", capacityType = UniformDistributionType), Source("id-1", capacityType =
      DistributionType("uniform", Some("uniform"), false, UniformDistributionModel(0.5))))
  }

  it should "work for areas" in {
    eq(Area("id-1"), Area("id-1"))
    ne(Area("id-1", name = Some("name")), Area("id-1"))
    eq(Area("id-1", name = Some("name")), Area("id-1", name = Some("name")))
    ne(Area("id-1", drains = Seq(Drain("id-1"))), Area("id-1"))
    eq(Area("id-1", drains = Seq(Drain("id-1"))), Area("id-1", drains = Seq(Drain("id-1"))))
    ne(Area("id-1", drains = Seq(Drain("id-1"))), Area("id-1", drains = Seq(Drain("id-1", capacity = 1))))
    ne(Area("id-1", sources = Seq(Source("id-1"))), Area("id-1"))
    eq(Area("id-1", sources = Seq(Source("id-1"))), Area("id-1", sources = Seq(Source("id-1"))))
    ne(Area("id-1", sources = Seq(Source("id-1"))), Area("id-1", sources = Seq(Source("id-1", capacity = 1))))
  }

  it should "work for lines" in {
    val (a1, a2, a3) = (Area("id-1"), Area("id-2"), Area("id-3"))
    eq(Line("id-4", area1 = a1, area2 = a2), Line("id-4", area1 = a1, area2 = a2))
    ne(Line("id-4", area1 = a1, area2 = a2), Line("id-4", area1 = a1, area2 = a2, disabled = true))
    eq(Line("id-4", area1 = a1, area2 = a2), Line("id-4", area1 = a2, area2 = a1))
    ne(Line("id-4", area1 = a1, area2 = a2), Line("id-4", area1 = a1, area2 = a3))
    ne(Line("id-4", area1 = a1, area2 = a2), Line("id-4", capacity = 1, area1 = a1, area2 = a2))
    ne(Line("id-4", area1 = a1, area2 = a2), Line("id-5", area1 = a1, area2 = a2))
  }

  it  should "work for worlds" in {
    eq(World(), World())
    ne(World(), World("another name"))
    eq(World(areas = Seq(Area("id-1"))), World(areas = Seq(Area("id-1"))))
    ne(World(areas = Seq(Area("id-1"))), World())

    val (a1, a2, a3) = (Area("1"), Area("2"), Area("3"))

    eq(World(areas = Seq(a1, a2)), World(areas = Seq(a1, a2)))
    eq(World(areas = Seq(a1, a2)), World(areas = Seq(a2, a1)))

    val (l1, l2) = (Line("id-1", area1 = a1, area2 = a2), Line("id-2", area1 = a1, area2 = a3))

    eq(World(areas = Seq(a1, a2, a3), lines = Seq(l1, l2)), World(areas = Seq(a1, a2, a3), lines = Seq(l1, l2)))
    eq(World(areas = Seq(a1, a2, a3), lines = Seq(l1, l2)), World(areas = Seq(a1, a2, a3), lines = Seq(l2, l1)))

    eq(World(types = Seq(UniformDistributionType, ConstantDistributionType)), World(types = Seq(UniformDistributionType, ConstantDistributionType)))
    eq(World(types = Seq(UniformDistributionType, ConstantDistributionType)), World(types = Seq(ConstantDistributionType, UniformDistributionType)))
    ne(World(types = Seq(UniformDistributionType, ConstantDistributionType)), World(types = Seq(UniformDistributionType)))

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

    eq(w1, w2)
  }
}
