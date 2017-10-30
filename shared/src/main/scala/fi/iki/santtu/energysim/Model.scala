package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model._


trait ModelDecoder {
  def decode(data: String): World
  def encode(world: World): String
}

object Model  {
  def from(data: String, decoder: ModelDecoder): World = {
    decoder.decode(data)
  }
}
