package fi.iki.santtu.energysim

import java.nio.file.Files.readAllBytes
import java.nio.file.Paths

import breeze.linalg._
import breeze.linalg.operators._
import breeze.stats._
import fi.iki.santtu.energysim.simulation.ScalaSimulation
import scribe.Logger

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

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

    val t0 = System.nanoTime()
//    val result = simulator.simulate(world, config.rounds)
    val fut = NlpSimulationCollector.simulate(world, simulator, config.rounds)
//    scribe.info(s"Waiting for future $fut")
    val result = Await.result(fut, Duration.Inf)
    val t1 = System.nanoTime()
    val ms = (t1 - t0).toDouble / 1e6

//    scribe.info(s"result:\n$result")

    println(f"Simulation with ${config.rounds} iterations took ${ms / 1000.0}%.3f s, ${ms / config.rounds}%.2f ms/round")

    def areaSummary(name: String, mat: DenseMatrix[Double], global: Boolean = false): Unit = {
      val losses = (mat(::, 2) <:< 0.0).activeSize
      val count = mat.rows

      // for global stats, we first sum each iteration and calculate
      // statistics from *that*
      val stats = if (global) {
        val xxx = (0 until result.rounds).map {
          i ⇒
            val rows = result.areaRound(i)
//            println(s"rows: $rows")
            val s = sum(rows(::, *)).t
//            println(s"sum: $s")
            s
        }
//        println(s"xxx: $xxx")
        val zzz = DenseMatrix(xxx:_*)
//        println(s"zzz: $zzz")
        val stats = meanAndVariance(zzz(::, *))
//        println(stats)
        stats
      } else {
        meanAndVariance(mat(::, *))
      }
      def ms(col: Int): String = {
        f"${stats(col).mean}%.0f +- ${stats(col).stdDev}%.0f"
      }

      def lh(col: Int): String = {
        val l = DescriptiveStats.percentile(
          mat(::, col).activeValuesIterator, .05)
        val h = DescriptiveStats.percentile(
          mat(::, col).activeValuesIterator, .95)
        f"[$l%.0f...$h%.0f]"
      }

      println(f"--- $name%s ------------------------------------------------------------")
      println(f"   outages          $losses%d (${100.0 * losses / count}%.1f%%)")
      println(f"   demand           ${ms(4)}%-15s ${lh(4)}")
      println(f"   generation       ${ms(3)}%-15s ${lh(3)}")
      println(f"   excess           ${ms(2)}%-15s ${lh(2)}")
      println(f"   transfer         ${ms(5)}%-15s ${lh(5)}")
    }

    val t2 = System.nanoTime()
    areaSummary("GLOBAL", result.areas, global = true)

    for (area ← world.areas) {
      val ai = result.areaIndex(area)
      val data = result.areas(result.areas(::, 1) :== ai.toDouble, ::)
      areaSummary(area.name, data.toDenseMatrix)
    }

    val t3 = System.nanoTime()
    val ms2 = (t3 - t2).toDouble / 1e6

    println(f"Summary generation took took ${ms2 / 1000.0}%.3f s")

  }
}
