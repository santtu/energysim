/*
 * Copyright 2017 Santeri Paavolainen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package fi.iki.santtu.energysim

import java.nio.file.Files.readAllBytes
import java.nio.file.Paths

import fi.iki.santtu.energysim.simulation.ScalaSimulation
import scribe.Logger

object Command {
  case class Config(file: String = "world.yml", rounds: Int = 1,
                    verbose: Boolean = false,
                    printAreas: Boolean = true,
                    printGlobal: Boolean = true,
                    printLines: Boolean = true,
                    printTypes: Boolean = true)

  val parser = new scopt.OptionParser[Config]("energysim") {
    head("energysim", getClass.getPackage.getImplementationVersion)
    help("help").text("prints this usage text")

    opt[Int]('r', "rounds")
      .action((value, config) ⇒ config.copy(rounds = value))
      .text("number of rounds to simulate")

    opt[Unit]('v', "verbose")
      .action((_, config) ⇒ config.copy(verbose = true))
      .text("increase verbosity")

    opt[Unit]("no-areas")
      .action((_, config) ⇒ config.copy(printAreas = false))

    opt[Unit]("no-lines")
      .action((_, config) ⇒ config.copy(printLines = false))

    opt[Unit]("no-types")
      .action((_, config) ⇒ config.copy(printTypes = false))

    opt[Unit]("no-global")
      .action((_, config) ⇒ config.copy(printGlobal = false))

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

//    println(s"World: ${world}")
//    println(s"units: ${world.units.map(_.id).mkString(", ")}")

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

    if (config.printGlobal)
      println(SimulationCollector.globalSummary(collector).mkString("\n"))

    if (config.printTypes)
      println(SimulationCollector.typesSummary(collector).mkString("\n"))

    if (config.printAreas)
      println(SimulationCollector.areasSummary(collector).mkString("\n"))

    if (config.printLines)
      println(SimulationCollector.linesSummary(collector).mkString("\n"))
  }
}
