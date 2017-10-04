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
  require(high > low)

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