package fi.iki.santtu.energysim.model

import scala.annotation.tailrec
import scala.util.Random
import scala.math.{exp, log, pow}

class ConstantCapacityModel() extends CapacityModel {
  override def capacity(amount: Int) = Capacity(amount)
  override def toString: String = s"Constant()"

  override def equals(obj: scala.Any): Boolean =
    obj.isInstanceOf[ConstantCapacityModel]

  override def hashCode(): Int = 0
}

object ConstantCapacityModel extends ConstantCapacityModel {
  def apply() = this
}

class UniformCapacityModel(val low: Double = 0.0, val high: Double = 1.0) extends CapacityModel {
  require(low <= high)
  require(low >= 0.0 && low <= 1.0)
  require(high >= 0.0 && high <= 1.0)
  
  override def capacity(amount: Int) =
    Capacity((Random.nextDouble() * (high - low) * amount +
      low * amount).toInt)

  override def equals(obj: scala.Any): Boolean =
    obj match {
      case o: UniformCapacityModel ⇒ o.low == low && o.high == high
      case _ ⇒ false
    }

  override def hashCode(): Int = Seq(low, high).hashCode()

  override def toString: String = s"Uniform()"
}

object UniformCapacityModel extends UniformCapacityModel(0.0, 1.0) {
  def apply() = this
  def apply(low: Double) = new UniformCapacityModel(low, 1.0)
  def apply(low: Double, high: Double) = new UniformCapacityModel(low, high)
}

class BetaCapacityModel(val alpha: Double, val beta: Double) extends CapacityModel {
  override def capacity(amount: Int) = Capacity(
    (distributions.beta(alpha, beta) * amount).toInt)

  override def equals(obj: scala.Any): Boolean =
    obj match {
      case o: BetaCapacityModel ⇒ o.alpha == alpha && o.beta == beta
      case _ ⇒ false
    }

  override def hashCode(): Int = Seq(alpha, beta).hashCode()

  override def toString: String = s"Beta($alpha,$beta)"
}

object BetaCapacityModel {
  def apply(alpha: Double, beta: Double): BetaCapacityModel =
    new BetaCapacityModel(alpha, beta)
}

case class Step(probability: Double, low: Double, high: Double)


/**
  * The data is a seq of steps where prob is individual,
  * unscaled probability, and low/high are the uniform distribution portions
  * (e.g. 0 to 1 values).
  */

class StepCapacityModel(val steps: Seq[Step]) extends CapacityModel {
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
  override def capacity(amount: Int) = {
    val r = Random.nextDouble()
    val result = csteps.collectFirst { case (p, l, h) if r <= p => (l, h) } match {
      case Some(Tuple2(low, high)) ⇒ (Random.nextDouble() * (high - low) + low) * amount
      case None ⇒ 0.0
    }
    Capacity(result.toInt)
  }

  override def equals(obj: scala.Any): Boolean =
    obj match {
      case o: StepCapacityModel ⇒ o.steps == steps
      case _ ⇒ false
    }

  override def hashCode(): Int = steps.hashCode()

  override def toString: String = s"Step($steps)"
}

object StepCapacityModel {
  def apply(steps: Seq[Step]): StepCapacityModel =
    new StepCapacityModel(steps)
}


// re-use step capacity model's probability summing ...
class ScaledCapacityModel(val mean: Double, _steps: Seq[Step]) extends StepCapacityModel(_steps) {
  override def equals(obj: Any): Boolean =
    obj match {
      case o: ScaledCapacityModel ⇒ o.steps == steps && o.mean == mean
      case _ ⇒ false
    }

  override def hashCode(): Int = super.hashCode()

  override def capacity(amount: Int) = {
    Capacity((super.capacity(1).amount * (amount / mean)).toInt)
  }
  def dummy = 1
  override def toString: String = s"Scaled($mean,$steps)"
}

object ScaledCapacityModel {
  def apply(mean: Double, steps: Seq[Step]): ScaledCapacityModel =
    new ScaledCapacityModel(mean, steps)
}