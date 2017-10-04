package fi.iki.santtu.energysim

import java.nio.file.Paths
import java.nio.file.Files.readAllBytes

object Command {
  def main(args: Array[String]): Unit = {
    val file = args.lift(0) match {
      case Some(value) ⇒ value
      case None ⇒ "world.yml"
    }
    val decoder = file.split('.').last match {
      case "json" ⇒ JsonDecoder
      case "yml"|"yaml" ⇒ YamlDecoder
      case _ ⇒
        throw new Exception(s"Unrecognized suffix in file $file")
    }

    val data = readAllBytes(Paths.get(file))
    val world = Model.from(data, decoder)

    println(s"World: ${world}")
    println(s"units: ${world.units}")
  }
}
