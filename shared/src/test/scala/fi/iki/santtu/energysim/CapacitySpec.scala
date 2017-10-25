package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model._
import org.scalatest.{FlatSpec, Matchers}

class CapacitySpec extends FlatSpec with Matchers {
  "Constant capacity type" should "always return value" in {
    ConstantCapacityType.model.capacity(0).amount shouldBe 0
    ConstantCapacityType.model.capacity(1000).amount shouldBe 1000
  }

  "Constant capacity model" should "always return value" in {
    ConstantCapacityModel.capacity(0).amount shouldBe 0
    ConstantCapacityModel.capacity(1000).amount shouldBe 1000

    ConstantCapacityModel().capacity(0).amount shouldBe 0
    ConstantCapacityModel().capacity(1000).amount shouldBe 1000

    (new ConstantCapacityModel).capacity(0).amount shouldBe 0
    (new ConstantCapacityModel).capacity(1000).amount shouldBe 1000
  }

  def rangetest(m: CapacityModel, ranges: Int*): scala.Unit = {
    val hls = ranges.grouped(2).toList

    m.capacity(0).amount shouldBe 0

    (1 to 1000) foreach {
      _ ⇒
        val a = m.capacity(1000).amount
        assert(hls.exists {
          case Seq(low, high) ⇒
            a >= low && a <= high
        },
          s"value $a is not within any of the ranges $hls")
    }
  }

  "Uniform capacity type" should "return values within ranges" in {
    rangetest(UniformCapacityType.model, 0, 1000)
  }

  "Uniform capacity model" should "return values within ranges" in {
    // default is [0, 1]
    rangetest(UniformCapacityModel, 0, 1000)
    rangetest(UniformCapacityModel(), 0, 1000)
    rangetest(new UniformCapacityModel, 0, 1000)

    rangetest(UniformCapacityModel(0.5), 500, 1000)
    rangetest(new UniformCapacityModel(0.5), 500, 1000)

    rangetest(UniformCapacityModel(0, .5), 0, 500)
    rangetest(new UniformCapacityModel(0, .5), 0, 500)

    rangetest(UniformCapacityModel(.5, .5), 500, 500)
    rangetest(new UniformCapacityModel(.5, .5), 500, 500)

    rangetest(UniformCapacityModel(.5, .75), 500, 750)
    rangetest(new UniformCapacityModel(.5, .75), 500, 750)
  }

  "Step capacity model" should "return values within ranges" in {
    StepCapacityModel(Seq()).capacity(1000).amount shouldBe 0
    (new StepCapacityModel(Seq())).capacity(1000).amount shouldBe 0

    rangetest(StepCapacityModel(Seq(Step(1.0, 1.0, 1.0))), 1000, 1000)
    rangetest(new StepCapacityModel(Seq(Step(1.0, 1.0, 1.0))), 1000, 1000)

    rangetest(StepCapacityModel(Seq(Step(1.0, 0.0, 0.0))), 0, 0)
    rangetest(new StepCapacityModel(Seq(Step(1.0, 0.0, 0.0))), 0, 0)

    rangetest(StepCapacityModel(
      Seq(Step(1.0, 0.1, 0.2), Step(1.0, 0.8, 0.9))),
      100, 200,
      800, 900)
    rangetest(new StepCapacityModel(
      Seq(Step(1.0, 0.1, 0.2), Step(1.0, 0.8, 0.9))),
      100, 200,
      800, 900)

    rangetest(StepCapacityModel(
      Seq(Step(0.0, 0.1, 0.2), Step(1.0, 0.8, 0.9))),
      800, 900)
    rangetest(new StepCapacityModel(
      Seq(Step(0.0, 0.1, 0.2), Step(1.0, 0.8, 0.9))),
      800, 900)

    rangetest(StepCapacityModel(
      Seq(
        Step(1, 0.1, 0.101),
        Step(1, 0.2, 0.2),
        Step(1, 0.5, 0.51),
        Step(1, 0.8, 0.9),
        Step(1, 0.99, 0.99),
        Step(1, 0.999, 1.000))),
      100, 101,
      200, 200,
      500, 510,
      800, 900,
      990, 990,
      999, 1000)
  }
}
