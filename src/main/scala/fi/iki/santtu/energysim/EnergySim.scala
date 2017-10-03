import fi.iki.santtu.energysim.Model

object EnergySim {
  def main(args: Array[String]): Unit = {
    println("Hello, world!")

    val world = Model.fromFile("world.yml")

    println(s"World: ${world}")
    println(s"units: ${world.units}")
  }
}
