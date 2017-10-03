package fi.iki.santtu.energysim.model

class ConstantCapacityModel(val value: Int) extends CapacityModel {
  override def capacity() = Capacity(value)
  override def toString: String = s"Constant($value)"
}

object ConstantCapacityModel {
  def apply(value: Int) = new ConstantCapacityModel(value)
}

class UniformCapacityModel(val min: Int, val max: Int) extends CapacityModel {
  override def capacity() = {
    ???
  }
  override def toString: String = s"Uniform($min,$max)"

}

object UniformCapacityModel {
  def apply(min: Int, max: Int) =
    new UniformCapacityModel(min, max)
}


class BetaCapacityModel(val scale: Int, val alpha: Double, val beta: Double) extends CapacityModel {
  override def capacity() = ???
  override def toString: String = s"Beta($scale,$alpha,$beta)"
}

object BetaCapacityModel {
  def apply(scale: Int, alpha: Double, beta: Double): BetaCapacityModel =
    new BetaCapacityModel(scale, alpha, beta)
}