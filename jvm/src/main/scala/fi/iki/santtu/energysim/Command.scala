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
    val world = Model.from(data, decoder)

    println(s"World: ${world}")
    println(s"units: ${world.units.map(_.name).mkString(", ")}")

    val simulator = ScalaSimulation
    val collector = SimulationCollector(world)

    val t0 = System.nanoTime()
//    val result = simulator.simulate(world, config.rounds)
//    val fut = NlpSimulationCollector.simulate(world, simulator, config.rounds)
////    scribe.info(s"Waiting for future $fut")
//    val result = Await.result(fut, Duration.Inf)
//    val result = SimulationCollector.simulate(world, simulator, config.rounds)

    val result = simulator.simulate(world, config.rounds)
    collector += result
    val t1 = System.nanoTime()
    val ms = (t1 - t0).toDouble / 1e6

//    scribe.info(s"result:\n$result")

    println(f"Simulation with ${config.rounds} iterations took ${ms / 1000.0}%.3f s, ${ms / config.rounds}%.2f ms/round")

//    println(s"result: $result")
//    println(s"collector: $collector")

    def areaSummary(name: String, a: AreaStatistics) = {
      println(s"==== $name ${"=" * (65 - name.length)}")
      println(f"  loss        ${a.loss.positive}%d / ${a.loss.percentage}%.1f%%")
      println(f"  total       ${a.total}%s MW")
      println(f"  excess      ${a.excess}%s MW")
      println(f"  generation  ${a.generation}%s MW")
      println(f"  drain       ${a.drain}%s MW")
      println(f"  transfer    ${a.transfer}%s MW")
      println(f"  ghg         ${a.ghg / 1e3 * 365 * 24}%s t/a")
    }

    def sourceSummary(name: String, s: SourceStatistics) = {
      println(s"  > $name")
      println(f"    maxed     ${s.atCapacity.percentage}%.1f%%")
      println(f"    used      ${s.used}%s MW")
      println(f"    excess    ${s.excess}%s MW")
      println(f"    capacity  ${s.capacity}%s MW")
      println(f"    ghg       ${s.ghg / 1e3 * 365 * 24}%s t/a")
    }

    def drainSummary(name: String, d: DrainStatistics) = {
      println(f"  < $name%-9s ${d.used}%s MW")

    }

    def lineSummary(name: String, leftName: String, rightName: String, l: LineStatistics) = {
      println(s"---- $name ($leftName ↔︎ $rightName) ${"-" * (65 - name.length - 7 - leftName.length - rightName.length)}")
      println(f"  maxed       ${l.atCapacity.percentage}%.1f%%")
      println(f"  transfer    ${l.transfer} MW")
      println(f"  unused      ${l.unused} MW")
      println(f" →$leftName%-11s ${l.left} MW")
      println(f" →$rightName%-11s ${l.right} MW")
    }


    areaSummary(world.name, collector.global)

    collector.areas.foreach {
      case (a, s) ⇒
        areaSummary(a.name, s)
        a.sources.foreach(s ⇒ sourceSummary(s.name, collector.sources(s)))
        a.drains.foreach(d ⇒ drainSummary(d.name, collector.drains(d)))
    }
    collector.lines.foreach {
      case (l, s) ⇒
        lineSummary(l.name, l.areas._1.name, l.areas._2.name, s)
    }
  }
}
