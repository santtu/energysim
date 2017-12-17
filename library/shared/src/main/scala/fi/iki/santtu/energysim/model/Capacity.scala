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

package fi.iki.santtu.energysim.model

class ConstantDistributionModel(scale: Double = 1.0) extends DistributionModel {
  override def sampleScaleFactor() = scale

  override def toString: String = s"Constant($scale)"

  override def equals(obj: scala.Any): Boolean =
    obj.isInstanceOf[ConstantDistributionModel]

  override def hashCode(): Int = 0
}

object ConstantDistributionModel extends ConstantDistributionModel(1.0) {
  def apply() = this
  def apply(scale: Double) = new ConstantDistributionModel(scale)
}

class UniformDistributionModel(val low: Double = 0.0, val high: Double = 1.0) extends DistributionModel {
  require(low <= high)
  require(low >= 0.0 && low <= 1.0)
  require(high >= 0.0 && high <= 1.0)

  override def sampleScaleFactor(): Double =
    distributions.uniform(low, high)

  override def equals(obj: scala.Any): Boolean =
    obj match {
      case o: UniformDistributionModel ⇒ o.low == low && o.high == high
      case _ ⇒ false
    }

  override def hashCode(): Int = Seq(low, high).hashCode()

  override def toString: String = s"Uniform()"
}

object UniformDistributionModel extends UniformDistributionModel(0.0, 1.0) {
  def apply() = this
  def apply(low: Double) = new UniformDistributionModel(low, 1.0)
  def apply(low: Double, high: Double) = new UniformDistributionModel(low, high)
}

class BetaDistributionModel(val alpha: Double, val beta: Double) extends DistributionModel {
  override def sampleScaleFactor(): Double =
    distributions.beta(alpha, beta)

  override def equals(obj: scala.Any): Boolean =
    obj match {
      case o: BetaDistributionModel ⇒ o.alpha == alpha && o.beta == beta
      case _ ⇒ false
    }

  override def hashCode(): Int = Seq(alpha, beta).hashCode()

  override def toString: String = s"Beta($alpha,$beta)"
}

object BetaDistributionModel {
  def apply(alpha: Double, beta: Double): BetaDistributionModel =
    new BetaDistributionModel(alpha, beta)
}

case class Step(probability: Double, low: Double, high: Double)


/**
  * The data is a seq of steps where prob is individual,
  * unscaled probability, and low/high are the uniform distribution portions
  * (e.g. 0 to 1 values).
  */

class StepDistributionModel(val steps: Seq[Step]) extends DistributionModel {
  // this will convert the unscaled probability into scaled accumulated
  // probability value, e.g. value is 0 to 1 so that each element can be
  // checked in order to see if its prob <= random, if true, then that is
  // picked.
  private val csteps = {
    val total = steps.map(_.probability).sum
    var upto = 0.0

    steps.map {
      step ⇒
        val cprob = upto + (step.probability / total)
        //println(s"prob=$prob low=$low high=$high upto=$upto total=$total cprob=$cprob")
        upto = cprob
        (upto, step.low, step.high)
    }
  }

  override def sampleScaleFactor(): Double = {
    val r = distributions.uniform()
    csteps.collectFirst { case (p, l, h) if r <= p => (l, h) } match {
      case Some(Tuple2(low, high)) ⇒ distributions.uniform(low, high)
      case None ⇒ 0.0
    }
  }

  override def equals(obj: scala.Any): Boolean =
    obj match {
      case o: StepDistributionModel ⇒ o.steps == steps
      case _ ⇒ false
    }

  override def hashCode(): Int = steps.hashCode()

  override def toString: String = s"Step($steps)"
}

object StepDistributionModel {
  def apply(steps: Seq[Step]): StepDistributionModel =
    new StepDistributionModel(steps)
}
