package fi.iki.santtu.energysim.model

import scala.annotation.tailrec
import scala.util.Random
import scala.math.{exp, log, pow}

class ConstantCapacityModel() extends CapacityModel {
  override def capacity(amount: Int) = Capacity(amount)
  override def toString: String = s"Constant()"
}

object ConstantCapacityModel extends ConstantCapacityModel {
}

class UniformCapacityModel() extends CapacityModel {
  override def capacity(amount: Int) =
    Capacity((Random.nextDouble() * amount).toInt)

  override def toString: String = s"Uniform()"
}

object UniformCapacityModel extends UniformCapacityModel {
}

//class BetaCapacityModel(val scale: Int, val alpha: Double, val beta: Double) extends CapacityModel {
//  override def capacity() = Capacity(
//    (distributions.beta(alpha, beta) * scale).toInt)
//
//  override def toString: String = s"Beta($scale,$alpha,$beta)"
//}
//
//object BetaCapacityModel {
//  def apply(scale: Int, alpha: Double, beta: Double): BetaCapacityModel =
//    new BetaCapacityModel(scale, alpha, beta)
//}
//
//object NullCapacityModel extends ConstantCapacityModel(0) {
//  def apply() = this
//}

case class Step(probability: Double, low: Double, high: Double)


/**
  * The data is a seq of steps where prob is individual,
  * unscaled probability, and low/high are the uniform distribution portions
  * (e.g. 0 to 1 values).
  *
  * @param _steps
  */

class StepCapacityModel(_steps: Seq[Step]) extends CapacityModel {
  // this will convert the unscaled probability into scaled accumulated
  // probability value, e.g. value is 0 to 1 so that each element can be
  // checked in order to see if its prob <= random, if true, then that is
  // picked.
  val steps = {
    val total = _steps.map(_.probability).sum
    var upto = 0.0

    _steps.map {
      step ⇒
        val cprob = upto + (step.probability / total)
        //println(s"prob=$prob low=$low high=$high upto=$upto total=$total cprob=$cprob")
        upto = cprob
        (upto, step.low, step.high)
    }
  }
  override def capacity(amount: Int) = {
    val r = Random.nextDouble()
    val result = steps.collectFirst { case (p, l, h) if r <= p => (l, h) } match {
      case Some(Tuple2(low, high)) ⇒ (Random.nextDouble() * (high - low) + low) * amount
      case None ⇒ 0.0
    }
    Capacity(result.toInt)
  }

  override def toString: String = s"Step(steps)"
}

object StepCapacityModel {
  def apply(steps: Seq[Step]): StepCapacityModel =
    new StepCapacityModel(steps)
}
