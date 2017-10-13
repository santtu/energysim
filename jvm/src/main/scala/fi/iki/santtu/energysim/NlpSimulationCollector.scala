package fi.iki.santtu.energysim

import breeze.linalg._
import fi.iki.santtu.energysim.model.{Area, Drain, Line, Source, World}
import fi.iki.santtu.energysim.simulation.{AreaData, Simulation}

import scala.concurrent.{ExecutionContext, Future}


class NlpResult(val rounds: Int,
                val areas: DenseMatrix[Double],
                val drains: DenseMatrix[Double],
                val sources: DenseMatrix[Double],
                val lines: DenseMatrix[Double],
                val areaIndex: Map[Area, Int],
                val drainIndex: Map[Drain, Int],
                val sourceIndex: Map[Source, Int],
                val lineIndex: Map[Line, Int]) {

  override def toString: String = s"[${rounds} rounds, ${areaIndex.size} areas, ${drainIndex.size} drains, ${sourceIndex.size} sources, ${lineIndex.size} lines]\n" +
    s"--- AREAS -------------\n${areas}\n" +
    s"--- DRAINS ------------\n${drains}\n" +
    s"--- SOURCES------------\n${sources}\n" +
    s"--- LINES -------------\n${lines}\n" +
    s"-----------------------"

  private def slice(mat: DenseMatrix[Double], stride: Int, i: Int): DenseMatrix[Double] =
    mat(i * stride until (i + 1) * stride, ::).toDenseMatrix

  def areaRound = slice(areas, areaIndex.size, _: Int)
  def sourceRound = slice(sources, sourceIndex.size, _: Int)
  def drainRound = slice(drains, drainIndex.size, _: Int)
  def lineRound = slice(lines, lineIndex.size, _: Int)
}

/**
  * This uses NLP arrays to store the data from simulation rounds -- this
  * keeps memory consumption lower (running 10^5 rounds on SBT got me a
  * out of memory error). This also uses parallelism to speed things up.
  *
  */
object NlpSimulationCollector {
  def simulate(world: World, simulation: Simulation, rounds: Int)(implicit ec: ExecutionContext): Future[NlpResult] = {
    // we need indices for sources and drains from units
    val areaIndex = world.areas.zipWithIndex
    val sourceIndex = world.units.collect { case s: Source ⇒ s }.zipWithIndex
    val drainIndex = world.units.collect { case d: Drain ⇒ d }.zipWithIndex
    val lineIndex = world.units.collect { case l: Line ⇒ l }.zipWithIndex

    // Breeze has no 3d matrices -- since we have 2 dynamic dimensions
    // e.g. round and unit/area index, it would be nice to have those as
    // matrix dimensions but we can't do that. Instead we'll have
    // nareas x rounds (nsources etc.) and have an extra "key" columns that
    // gives the iteration and area (source, ...) index. The key makes it
    // possible later to slice through the table to find a particular
    // area (source, ...) or iteration data.
    //
    // This means the resulting matrix will be (for areas) like this:
    //
    // [ [1, 1, area1 round1 data],
    //   [1, 2, area2 round1 data],
    //   [2, 1, area1 round1 data], ... ]
    //
    // Each round is in a single block, e.g. stride for rounds is nareas.
    //
    // For drains we could use a 2d matrix, but to keep things consistent we
    // use the same structure.

    // (nareas * rounds) x [key, total, excess, generation, drain, transfer]
    val areas = DenseMatrix.zeros[Int](rounds * areaIndex.size, 7)

    // (ndrains * rounds): [key, used]
    val drains = DenseMatrix.zeros[Int](rounds * drainIndex.size, 3)

    // (nsources * rounds) x [key, used, excess, capacity]
    val sources = DenseMatrix.zeros[Int](rounds * sourceIndex.size, 5)

    // nlines x rounds x [key, used, excess, capacity]
    val lines = DenseMatrix.zeros[Int](rounds * lineIndex.size, 5)

    def areaRange(i: Int) = (i * areaIndex.size) until ((i + 1) * areaIndex.size)
    def drainRange(i: Int) = (i * drainIndex.size) until ((i + 1) * drainIndex.size)
    def sourceRange(i: Int) = (i * sourceIndex.size) until ((i + 1) * sourceIndex.size)
    def lineRange(i: Int) = (i * lineIndex.size) until ((i + 1) * lineIndex.size)

    Future.sequence {
      (0 until rounds).map {
        i ⇒
          Future {
            val round = simulation.simulate(world)
            if (areaIndex.nonEmpty)
              areas(areaRange(i), ::) := DenseMatrix(areaIndex.map {
                case (a, ai) ⇒ Seq(i, ai) ++ round.areas(a).toSeq
              }:_*)
            if (drainIndex.nonEmpty)
              drains(drainRange(i), ::) := DenseMatrix(drainIndex.map {
                case (d: Drain, di) ⇒ Seq(i, di, round.units(d).used)
              }:_*)
            if (sourceIndex.nonEmpty)
              sources(sourceRange(i), ::) := DenseMatrix(sourceIndex.map {
                case (s: Source, si) ⇒ Seq(i, si) ++ round.units(s).toSeq
              }:_*)
            if (lineIndex.nonEmpty)
              lines(lineRange(i), ::) := DenseMatrix(lineIndex.map {
                case (l: Line, li) ⇒ Seq(i, li) ++ round.units(l).toSeq
              }:_*)
            Unit
          }
      }
    } map {
      _ ⇒
        new NlpResult(rounds,
          convert(areas, Double), convert(drains, Double),
          convert(sources, Double), convert(lines, Double),
          areaIndex.toMap, drainIndex.toMap, sourceIndex.toMap, lineIndex.toMap)
    }
  }
}
