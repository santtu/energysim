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

package fi.iki.santtu.energysim.simulation

import fi.iki.santtu.energysim.simulation.EdmondsKarp.Graph

import scala.collection.{LinearSeq}
import scala.collection.mutable.{ArrayBuffer, ListBuffer, ArraySeq, Queue}

class EdmondsKarp(initialGraph: Graph) {
  type Matrix = IndexedSeq[IndexedSeq[Int]]
  class Result(val maxFlow: Int, flows: Matrix, residuals: Matrix) {
    def flow(from: Int, to: Int) = flows(from)(to)
    def residual(from: Int, to: Int) = residuals(from)(to)

    override def toString: String = s"$maxFlow"
  }

  // Turn the input graph into a mutable graph
  private val graph = ArrayBuffer[ListBuffer[(Int, Int)]](
      (initialGraph.map(ListBuffer[(Int, Int)](_:_*))):_*)

  require(initialGraph.forall(
    l ⇒ l.forall(l ⇒ l._1 >= 0 && l._1 < initialGraph.length)))

  /**
    * Add a new link between nodes with the given capacity.
    *
    * @param from
    * @param to
    * @param capacity
    */
  def add(from: Int, to: Int, capacity: Int): Unit = {
    require(capacity >= 0 && from >= 0 && to >= 0)
    val end = math.max(from, to)

    scribe.trace(s"add: from=$from to=$to capacity=$capacity size=${graph.size} end=$end")

    if (end >= graph.size)
      graph ++= Seq.fill(end - graph.size + 1){ ListBuffer[(Int, Int)]() }

    val lb = graph(from)
    scribe.trace(s"size now ${graph.size} lb=$lb")
    require(!lb.exists(_ match { case (t, _) ⇒ t == to }),
      s"a connection from $from to $to already exists")
    lb.append((to, capacity))
  }

  /**
    * Search for a path from the source to sink with the given
    * residual capacities, returning the path.
    * If no path was found, the returned list is empty.
    */
  private def search(source: Int, sink: Int, residuals: IndexedSeq[IndexedSeq[Int]]): Seq[Int] = {
    val visited = ArraySeq.fill[Boolean](graph.size)(false)
    val parent = ArraySeq.fill[Int](graph.size)(-1)
    val queue = Queue[Int]()

    visited(source) = true
    queue += source

    while (queue.nonEmpty) {
      val from = queue.dequeue()

      graph(from).foreach {
        case (to, _) ⇒
          scribe.trace(s"($from,$to) visited ${visited(to)} residual ${residuals(from)(to)}")
          if (!visited(to) && residuals(from)(to) > 0) {
            scribe.trace(s"search: adding visit to $to")
            visited(to) = true
            queue += to
            parent(to) = from
          }
      }
    }

    def path(i: Int, acc: Seq[Int]): Seq[Int] = {
      scribe.trace(s"path: i=$i acc=$acc parent(i)=${parent.lift(i)}")
      if (i == -1 || !visited(i))
        acc.reverse
      else
        path(parent(i), acc :+ i)
    }

    scribe.trace(s"search: source=$source sink=$sink visited=$visited parent=$parent queue=$queue")

    // look for path from the sink via the parent chain
    path(sink, Seq.empty[Int])
  }

  /**
    * Solve the current graph from given source to sink.
    *
    * @param source
    * @param sink
    * @return If no flow could be generated, fails, otherwise the result is
    *         a tuple of maximum flow and sequence of flows between nodes.
    */
  def solve(source: Int, sink: Int): Result = {
    if (source >= graph.size || sink >= graph.size)
      throw new IllegalArgumentException("source or sink out of bounds")

    // residual graph
    val residuals = {
      val g = ArraySeq.fill[Int](graph.size, graph.size)(0)
      graph.zipWithIndex.foreach {
        case (edges, from) ⇒
          edges.foreach { case (to, capacity) ⇒ g(from)(to) = capacity }
      }
      g
    }
    val flows = ArraySeq.fill[Int](graph.size, graph.size)(0)

//    println(s"solve: initial residuals:")
//    for (r ← residuals) {
//      for (c ← r)
//        print(s"$c\t")
//      println("")
//    }

    var maxFlow: Int = 0

    // keep going on until no path is available
    var path: Seq[Int] = Seq.empty[Int]

    // yes, we could do a tail recursion here to be functional, but
    // this is way more readable with a simple while loop
    while ( {
      path = search(source, sink, residuals)
      path.nonEmpty
    }) {
      assert(path(0) == source && path.last == sink)

      scribe.trace(s"path=$path, determining flow")

      // calculate flow and as a side effect, reduce residuals
      // and update path flows, and again, we could use foldLeft
      // or recursion here, but this should again be readable
      var flow = Int.MaxValue

      val pathPairs = path.dropRight(1).zip(path.drop(1))

      pathPairs.foreach {
        case (parent, child) ⇒
          val residual = residuals(parent)(child)
          scribe.trace(s"$parent $child $residual")
          flow = math.min(flow, residual)
      }

      pathPairs.foreach {
        case (parent, child) ⇒
          residuals(parent)(child) -= flow
          residuals(child)(parent) += flow

          flows(parent)(child) += flow
          flows(child)(parent) -= flow
      }

      scribe.debug(s"found path, $path, flow=$flow")

      maxFlow += flow
    }

    scribe.debug(s"no more paths, max flow: $maxFlow flows=$flows residuals=$residuals")

    new Result(maxFlow, flows, residuals)
  }
}

object EdmondsKarp {
  /**
    * A graph is indexable list that contains another list
    * with target, capacity values.
    */
  type Graph = IndexedSeq[LinearSeq[(Int, Int)]]

  private val emptyGraph = Vector.empty[LinearSeq[(Int, Int)]]

  def apply(graph: Graph): EdmondsKarp =
    new EdmondsKarp(graph)

  def apply(): EdmondsKarp =
    new EdmondsKarp(emptyGraph)
}


