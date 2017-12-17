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
import org.scalatest.{FlatSpec, Matchers}

class DistributionSpec extends FlatSpec with Matchers {
  "Constant capacity type" should "always return value" in {
    val d = ConstantDistributionType
    d.makeCapacity.amount(0) shouldBe 0
    d.makeCapacity.amount(1000) shouldBe 1000
  }

  "Constant capacity model" should "always return identity scale factor" in {
    ConstantDistributionModel.sampleScaleFactor() shouldBe 1.0
    ConstantDistributionModel().sampleScaleFactor() shouldBe 1.0
    (new ConstantDistributionModel).sampleScaleFactor() shouldBe 1.0
  }

  def rangetest(m: DistributionModel, ranges: Double*): scala.Unit = {
    val hls = ranges.grouped(2).toList

    (1 to 1000) foreach {
      _ ⇒
        val a = m.sampleScaleFactor()
        assert(hls.exists {
          case Seq(low, high) ⇒
            a >= low && a <= high
        },
          s"value $a is not within any of the ranges $hls")
    }
  }

  "Uniform capacity type" should "return values within ranges" in {
    rangetest(UniformDistributionType.model, 0, 1.0)
  }

  "Uniform capacity model" should "return values within ranges" in {
    // default is [0, 1]
    rangetest(UniformDistributionModel, 0, 1.0)
    rangetest(UniformDistributionModel(), 0, 1.0)
    rangetest(new UniformDistributionModel, 0, 1.0)

    rangetest(UniformDistributionModel(0.5), 0.5, 1.0)
    rangetest(new UniformDistributionModel(0.5), 0.5, 1.0)

    rangetest(UniformDistributionModel(0, .5), 0, 0.5)
    rangetest(new UniformDistributionModel(0, .5), 0, 0.5)

    rangetest(UniformDistributionModel(.5, .5), 0.5, 0.5)
    rangetest(new UniformDistributionModel(.5, .5), 0.5, 0.5)

    rangetest(UniformDistributionModel(.5, .75), 0.5, 0.75)
    rangetest(new UniformDistributionModel(.5, .75), 0.5, 0.75)
  }

  "Step capacity model" should "return values within ranges" in {
    StepDistributionModel(Seq()).sampleScaleFactor() shouldBe 0.0
    (new StepDistributionModel(Seq())).sampleScaleFactor() shouldBe 0.0

    rangetest(StepDistributionModel(Seq(Step(1.0, 1.0, 1.0))), 1.0, 1.0)
    rangetest(new StepDistributionModel(Seq(Step(1.0, 1.0, 1.0))), 1.0, 1.0)

    rangetest(StepDistributionModel(Seq(Step(1.0, 0.0, 0.0))), 0, 0)
    rangetest(new StepDistributionModel(Seq(Step(1.0, 0.0, 0.0))), 0, 0)

    rangetest(StepDistributionModel(
      Seq(Step(1.0, 0.1, 0.2), Step(1.0, 0.8, 0.9))),
      0.1, 0.2,
      0.8, 0.9)
    rangetest(new StepDistributionModel(
      Seq(Step(1.0, 0.1, 0.2), Step(1.0, 0.8, 0.9))),
      0.1, 0.2,
      0.8, 0.9)

    rangetest(StepDistributionModel(
      Seq(Step(0.0, 0.1, 0.2), Step(1.0, 0.8, 0.9))),
      0.8, 0.9)
    rangetest(new StepDistributionModel(
      Seq(Step(0.0, 0.1, 0.2), Step(1.0, 0.8, 0.9))),
      0.8, 0.9)

    rangetest(StepDistributionModel(
      Seq(
        Step(1, 0.1, 0.101),
        Step(1, 0.2, 0.2),
        Step(1, 0.5, 0.51),
        Step(1, 0.8, 0.9),
        Step(1, 0.99, 0.99),
        Step(1, 0.999, 1.000))),
      0.100, 0.101,
      0.200, 0.200,
      0.500, 0.510,
      0.800, 0.900,
      0.990, 0.990,
      0.999, 1.000)
  }

  def samplen(m: DistributionType, amount: Int = 1000, n: Int = 1000): Seq[Int] = {
    val c = m.makeCapacity
    (1 to n).map(_ ⇒ c.amount(amount))
  }

  // note that calculating the set size is actually a probabilistic test,
  // but given the amount of repetition the probability of having a false
  // negative is about the size of epsilon
  "Aggregated and independent distributions" should "be distinct or singular" in {
    samplen(DistributionType("id", None, false, UniformDistributionModel)).toSet.size should be > 1
    samplen(DistributionType("id", None, true, UniformDistributionModel)).toSet.size shouldBe 1

    val step = StepDistributionModel(Seq(Step(1, 0.0, 1.0)))
    samplen(DistributionType("id", None, false, step)).toSet.size should be > 1
    samplen(DistributionType("id", None, true, step)).toSet.size shouldBe 1
  }
}
