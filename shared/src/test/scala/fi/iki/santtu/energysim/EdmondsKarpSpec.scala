package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.simulation.EdmondsKarp
import org.scalatest.{FlatSpec, Matchers}

class EdmondsKarpSpec extends FlatSpec with Matchers {
  "Edmonds-Karp algorithm" should "fail with empty graph" in {
    val ek = EdmondsKarp()
    an [IllegalArgumentException] should be thrownBy ek.solve(0, 0)
    an [IllegalArgumentException] should be thrownBy ek.solve(0, 1)
    an [IllegalArgumentException] should be thrownBy ek.solve(1, 1)
  }

  it should "find no flow if no capacity in graph" in {
    val ek = EdmondsKarp()
    ek.add(0, 1, 0)
    ek.add(1, 2, 0)

    ek.solve(0, 1).maxFlow shouldBe 0
    ek.solve(1, 2).maxFlow shouldBe 0
  }

  it should "find flow based on chokepoint" in {
    val ek = EdmondsKarp()
    ek.add(0, 1, 100000)
    ek.add(1, 2, 10)
    ek.add(2, 3, 10000)

    ek.solve(0, 1).maxFlow shouldBe 100000
    ek.solve(1, 2).maxFlow shouldBe 10
    ek.solve(2, 3).maxFlow shouldBe 10000
    ek.solve(0, 3).maxFlow shouldBe 10
  }

  it should "find flows through multiple paths" in {
    val ek = EdmondsKarp()
    ek.add(0, 1, 5000)
    ek.add(0, 2, 1110)
    ek.add(1, 3, 1000000)
    ek.add(2, 3, 1000000)

    ek.solve(0, 3).maxFlow shouldBe 6110
  }


  it should "handle cycles correctly" in {
    val ek = EdmondsKarp()
    ek.add(0, 1, 250)
    ek.add(1, 2, 70)
    ek.add(2, 1, 100)
    ek.add(2, 0, 10)

    ek.solve(0, 2).maxFlow shouldBe 70
  }
}
