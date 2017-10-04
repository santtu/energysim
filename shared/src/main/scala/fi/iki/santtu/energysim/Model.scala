package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model._


private object CapacityConverter {
  def convert(name: String, params: Seq[Double]): CapacityModel = {
    (name, params) match {
      case ("constant", Seq(value)) ⇒ ConstantCapacityModel(value.toInt)
      case ("uniform", Seq(min, max)) ⇒ UniformCapacityModel(min.toInt, max.toInt)
      case ("beta", Seq(scale, alpha, beta)) ⇒ BetaCapacityModel(scale.toInt, alpha, beta)

      case (typeName, _) ⇒
        throw new Exception(s"capacity model ${typeName} not recognized")
    }
  }
}

trait ModelDecoder {
  def decode(data: Array[Byte]): World
  def encode(world: World): Array[Byte]
}

object Model  {
  def from(data: Array[Byte], decoder: ModelDecoder): World = {
    decoder.decode(data)
  }
}
