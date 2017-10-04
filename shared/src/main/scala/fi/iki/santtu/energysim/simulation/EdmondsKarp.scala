package fi.iki.santtu.energysim.simulation

import fi.iki.santtu.energysim.simulation.EdmondsKarp.Graph

import scala.collection.{LinearSeq}
import scala.collection.mutable.{ArrayBuffer, ListBuffer, ArraySeq, Queue}

class EdmondsKarp(initialGraph: Graph) {
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
    val lb = graph.lift(from) match {
      case Some(buf) ⇒ buf
      case None ⇒
        val buf = ListBuffer[(Int, Int)]()
        graph.update(from, buf)
        buf
    }

    require(!lb.exists(_ match { case (t, _) ⇒ t == to }))
    lb.append((to, capacity))
  }

  /**
    * Solve the current graph from given source to sink.
    *
    * @param source
    * @param sink
    * @return If no flow could be generated, fails, otherwise the result is
    *         a tuple of maximum flow and sequence of flows between nodes.
    */
  def solve(source: Int, sink: Int): Int = {
    // residual graph
    val residuals = {
      val g = Array.ofDim[Int](graph.size, graph.size)
      graph.zipWithIndex.foreach {
        case (l, i) ⇒
          l.foreach { case (t, c) ⇒ g(i)(t) = c }
      }
      g
    }

    var maxFlow: Int = 0

    /**
      * Search for a path from the source to sink, returning the path.
      * If no path was found, the returned list is empty.
      */
    def search(): Seq[Int] = {
      val visited = ArraySeq.fill[Boolean](graph.size)(false)
      val parent = ArraySeq.fill[Int](graph.size)(-1)
      val queue = Queue[Int]()

      visited(source) = true
      queue += source

      while (queue.nonEmpty) {
        val from = queue.dequeue()

        graph(from).foreach {
          case (to, _) ⇒
            if (!visited(to) && residuals(from)(to) > 0) {
              visited(to) = true
              queue += to
              parent(to) = from
            }
        }
      }

      def path(i: Int, acc: Seq[Int]): Seq[Int] =
        if (i == -1)
          acc
        else
          path(parent(i), acc :+ i)

      path(sink, Seq.empty[Int])
    }

    def optimize(): Int =
      search() match {
        case Nil ⇒
          println(s"no path from $source to $sink found")
          maxFlow
        case path ⇒
          println(s"found path, $path")
          -1
      }

    optimize()
  }
}

object EdmondsKarp {
  /**
    * A graph is indexable list that contains another list
    * with target, capacity values.
    */
  type Graph = IndexedSeq[LinearSeq[(Int, Int)]]

  private val emptyGraph = Array.empty[LinearSeq[(Int, Int)]]

  def apply(graph: Graph): EdmondsKarp =
    new EdmondsKarp(graph)

  def apply(): EdmondsKarp =
    new EdmondsKarp(emptyGraph)
}


