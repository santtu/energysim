package fi.iki.santtu.energysim.model

import scala.annotation.tailrec
import scala.util.Random
import scala.math.{exp, log, pow}

class ConstantCapacityModel(val value: Int) extends CapacityModel {
  override def capacity() = Capacity(value)
  override def toString: String = s"Constant($value)"
}

object ConstantCapacityModel {
  def apply(value: Int) = new ConstantCapacityModel(value)
}

class UniformCapacityModel(val low: Int, val high: Int) extends CapacityModel {
  require(high >= low)

  override def capacity() =
    Capacity((Random.nextDouble() * (high - low) + low).toInt)

  override def toString: String = s"Uniform($low,$high)"
}

object UniformCapacityModel {
  def apply(min: Int, max: Int) =
    new UniformCapacityModel(min, max)
}

class BetaCapacityModel(val scale: Int, val alpha: Double, val beta: Double) extends CapacityModel {
  override def capacity() = Capacity(
    (distributions.beta(alpha, beta) * scale).toInt)

  override def toString: String = s"Beta($scale,$alpha,$beta)"
}

object BetaCapacityModel {
  def apply(scale: Int, alpha: Double, beta: Double): BetaCapacityModel =
    new BetaCapacityModel(scale, alpha, beta)
}

object NullCapacityModel extends ConstantCapacityModel(0) {
  def apply() = this
}

class StepCapacityModel(_steps: Seq[Seq[Double]]) extends CapacityModel {
  val steps = {
    val total = _steps.map(_(0)).sum
    var upto = 0.0

    _steps.map {
      case Seq(prob, low, high) =>
        val cprob = upto + (prob / total)
        //println(s"prob=$prob low=$low high=$high upto=$upto total=$total cprob=$cprob")
        upto = cprob
        (upto, UniformCapacityModel(low.toInt, high.toInt))
    }
  }
  override def capacity() = {
    val r = Random.nextDouble()
    val result = steps.collectFirst { case (p, m) if r <= p => m }.get.capacity()
    //println(s"steps=${steps.toSeq} r=$r result=$result")
    result
  }

  override def toString: String = s"Step(steps)"
}

object StepCapacityModel {
  def apply(steps: Seq[Seq[Double]]): StepCapacityModel =
    new StepCapacityModel(steps)
}
