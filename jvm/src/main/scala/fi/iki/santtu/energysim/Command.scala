package fi.iki.santtu.energysim

import java.nio.file.Files.readAllBytes
import java.nio.file.Paths

import fi.iki.santtu.energysim.simulation.ScalaSimulation
import scribe.Logger

object Command {
  case class Config(file: String = "world.yml", rounds: Int = 1,
                    verbose: Boolean = false)

  val parser = new scopt.OptionParser[Config]("energysim") {
    head("energysim", getClass.getPackage.getImplementationVersion)
    help("help").text("prints this usage text")

    opt[Int]('r', "rounds")
      .action((value, config) ⇒ config.copy(rounds = value))
      .text("number of rounds to simulate")

    opt[Unit]('v', "verbose")
      .action((_, config) ⇒ config.copy(verbose = true))
      .text("increase verbosity")

    arg[String]("FILE")
      .optional()
      .action((file, config) ⇒ config.copy(file = file))
      .text("world definition file")
  }

  def main(args: Array[String]): Unit = {
    val config = parser.parse(args, Config()) match {
      case Some(config) =>
        config
      case None =>
        sys.exit(2)
    }

    if (config.verbose)
      Logger.root.update { Logger.root.copy(multiplier = 2.0) }

    scribe.debug(s"parser=$parser config=$config")

    val decoder = config.file.split('.').last match {
      case "json" ⇒ JsonDecoder
      case "yml"|"yaml" ⇒ YamlDecoder
      case _ ⇒
        throw new Exception(s"Unrecognized suffix in file ${config.file}")
    }

    val data = readAllBytes(Paths.get(config.file))
    val world = Model.from(new String(data, "UTF-8"), decoder)

    println(s"World: ${world}")
    println(s"units: ${world.units.map(_.id).mkString(", ")}")

    val simulator = ScalaSimulation
    val collector = SimulationCollector(world)

    val t0 = System.nanoTime()
//    val result = simulator.simulate(world, config.rounds)
//    val fut = NlpSimulationCollector.simulate(world, simulator, config.rounds)
////    scribe.info(s"Waiting for future $fut")
//    val result = Await.result(fut, Duration.Inf)
//    val result = SimulationCollector.simulate(world, simulator, config.rounds)

    val result = simulator.simulate(world, config.rounds)
    val t1 = System.nanoTime()
    val ms = (t1 - t0).toDouble / 1e6
    collector += result

//    scribe.info(s"result:\n$result")

    println(f"Simulation with ${config.rounds} iterations took ${ms / 1000.0}%.3f s, ${ms / config.rounds}%.2f ms/round")

//    println(s"result: $result")
//    println(s"collector: $collector")

    println(SimulationCollector.summary(collector))

  }
}
