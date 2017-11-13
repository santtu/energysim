package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model.{Area, ConstantDistributionModel, ConstantDistributionModel$, DistributionType, Drain, Line, Source, UniformDistributionModel, UniformDistributionType, Unit, World}
import fi.iki.santtu.energysim.simulation.{AreaData, ScalaSimulation, Simulation, UnitData}
import org.scalatest.{FlatSpec, Matchers}

abstract class AbstractSimulationSpec extends FlatSpec with Matchers {
  def simulation: Simulation

  "Empty world" should "pass simulation" in {
    val world = World()
    val r = simulation.simulate(world)

    r.areas shouldBe empty
    r.units shouldBe empty
  }

  var counter: Int = 0

  def makeId = {
    counter += 1
    s"id-${counter}"
  }

  def W(stuff: Any*) =
    World(areas = stuff.collect { case a: Area ⇒ a },
      lines = stuff.collect { case l: Line ⇒ l },
      types = stuff.collect { case t: DistributionType ⇒ t })
  def A(units: Unit*) =
    Area(makeId, drains = units.collect { case d: Drain ⇒ d },
      sources = units.collect { case s: Source ⇒ s})
  def An(id: String, units: Unit*) =
    Area(id = id,
      drains = units.collect { case d: Drain ⇒ d },
      sources = units.collect { case s: Source ⇒ s})
  val constantType = DistributionType("constant", None, false, ConstantDistributionModel)
  def D(c: Int, disabled: Boolean = false, capacityType: DistributionType = constantType) = Drain(makeId, capacity = c, capacityType = capacityType, disabled = disabled)
  def Dn(id: String, c: Int, disabled: Boolean = false, capacityType: DistributionType = constantType) = Drain(id = id, capacity = c, capacityType = capacityType, disabled = disabled)
  def S(c: Int, ghg: Double = 0.0, disabled: Boolean = false, capacityType: DistributionType = constantType) = Source(makeId, capacity = c, capacityType = capacityType, ghgPerCapacity = ghg, disabled = disabled)
  def Sn(id: String, c: Int, ghg: Double = 0.0, disabled: Boolean = false, capacityType: DistributionType = constantType) = Source(id = id, capacity = c, capacityType = capacityType, ghgPerCapacity = ghg, disabled = disabled)
  def L(c: Int, a1: Area, a2: Area, disabled: Boolean = false, capacityType: DistributionType = constantType) = Line(makeId, capacity = c, capacityType = capacityType, area1 = a1, area2 = a2, disabled = disabled)
  def Ln(id: String, c: Int, a1: Area, a2: Area, disabled: Boolean = false, capacityType: DistributionType = constantType) = Line(id = id, capacity = c, capacityType = capacityType, area1 = a1, area2 = a2, disabled = disabled)

  "Single area" should "use local sources only" in {
    val world = W(An("a", Dn("d", 100), Sn("s1", 90, 0.0), Sn("s2", 10, 1.0), Sn("s3", 1000, 10.0)))

    val r = ScalaSimulation.simulate(world)

    r.areas.size shouldBe 1
    r.units.size shouldBe 4

    val a = world.areas(0)

    r.areas("a") shouldBe AreaData(0, 1000, 1100, -100, 0)
    r.units("d") shouldBe UnitData(-100, 0, -100)
    r.units("s1") shouldBe UnitData(90, 0, 90)
    r.units("s2") shouldBe UnitData(10, 0, 10)
    r.units("s3") shouldBe UnitData(0, 1000, 1000)
  }

  "Two areas with interconnection" should "have no transfers if all can be locally powered" in {
    val (a1, a2) = (An("a", D(100), S(200, 0.0)), An("b", D(50), S(100)))
    val l = Ln("line", 1000, a1, a2)
    val world = W(a1, a2, l)

    val r = ScalaSimulation.simulate(world)

    r.areas("a") shouldBe AreaData(0, 100, 200, -100, 0)
    r.areas("b") shouldBe AreaData(0, 50, 100, -50, 0)
    r.units("line") shouldBe UnitData(0, 1000, 1000)
  }

  "Area without power" should "be able to get all power via transfer" in {
    val (a1, a2) = (An("a", S(100), S(100, 1.0)), An("b", D(150)))
    val l = Ln("line", 1000, a1, a2)
    val w = W(a1, a2, l)

    val r = ScalaSimulation.simulate(w)

    r.areas("a") shouldBe AreaData(0, 50, 200, 0, -150)
    r.areas("b") shouldBe AreaData(0, 0, 0, -150, 150)
    r.units("line") shouldBe UnitData(150, 850, 1000)
  }

  it should "work also in reverse" in {
    val (a1, a2) = (An("a", S(100), S(100, 1.0)), An("b", D(150)))
    val l = Ln("line", 1000, a2, a1)
    val w = W(a1, a2, l)

    val r = ScalaSimulation.simulate(w)

    r.areas("a") shouldBe AreaData(0, 50, 200, 0, -150)
    r.areas("b") shouldBe AreaData(0, 0, 0, -150, 150)
    r.units("line") shouldBe UnitData(-150, 1150, 1000)
  }

  it should "receive over multiple lines" in {
    val (a1, a2, a3, b) = (An("a1", S(50)), An("a2", S(50)), An("a3", S(50)), An("b", D(150)))
    val (l1, l2, l3) = (Ln("line-1", 60, a1, b), Ln("line-2", 60, b, a2), Ln("line-3", 60, a3, b))
    val w = W(a1, a2, a3, b, l1, l2, l3)

    val r = ScalaSimulation.simulate(w)

    r.areas("a1") shouldBe AreaData(0, 0, 50, 0, -50)
    r.areas("a2") shouldBe AreaData(0, 0, 50, 0, -50)
    r.areas("a3") shouldBe AreaData(0, 0, 50, 0, -50)
    r.areas("b") shouldBe AreaData(0, 0, 0, -150, 150)

    // lines do not have preference, we can really only check for the
    // total sum over all
    val ld = r.units.filterKeys(_.startsWith("line-")).values

    ld.map(_.used.abs).sum shouldBe 150
    ld.map(_.excess).sum shouldBe 130 // not 30 because one of the lines goes in reverse and has excess of 110
  }

  "Underpowered network" should "prefer local power" in {
    val (a1, a2, a3) = (An("a", D(100), S(50)), An("b", D(10), S(5)), An("c", D(1000), S(900)))
    val (l1, l2, l3) = (Ln("line-1", 10000, a1, a2), Ln("line-2", 10000, a2, a3), Ln("line-3", 10000, a1, a3))
    val w = W(a1, a2, a3, l1, l2, l3)

    val r = ScalaSimulation.simulate(w)

    r.areas("a") shouldBe AreaData(-50, 0, 50, -100, 0)
    r.areas("b") shouldBe AreaData(-5, 0, 5, -10, 0)
    r.areas("c") shouldBe AreaData(-100, 0, 900, -1000, 0)

    r.units("line-1") shouldBe UnitData(0, 10000, 10000)
    r.units("line-2") shouldBe UnitData(0, 10000, 10000)
    r.units("line-3") shouldBe UnitData(0, 10000, 10000)
  }

  "Transfers through an area" should "not affect between area's totals" in {
    val (a1, a2, a3) = (An("a", D(1000)), An("b", S(1000)), An("c"))
    val (l1, l2) = (Ln("line-1", 1000, a3, a1), Ln("line-2", 1, a3, a2))
    val w = W(a1, a2, a3, l1, l2)

    val r = ScalaSimulation.simulate(w)

    r.areas("a") shouldBe AreaData(-999, 0, 0, -1000, 1)
    r.areas("b") shouldBe AreaData(0, 999, 1000, 0, -1)
    r.areas("c") shouldBe AreaData(0, 0, 0, 0, 0)

    r.units("line-1") shouldBe UnitData(1, 999, 1000)
    r.units("line-2") shouldBe UnitData(-1, 2, 1) // remember a2->a3 dir
  }

  "Unused capacity" should "be attributed back to sources" in {
    // one area that meets all with ghg = 0, cannot transfer all
    // capacity out, another that needs high ghg but has excess of that
    val (a1, a2) = (An("a", D(10), Sn("source-a", 100)),
      An("b", D(100), Sn("source-b", 200, 1.0)))
    val l = Ln("line-1", 50, a1, a2)
    val w = W(a1, a2, l)
    val r = ScalaSimulation.simulate(w)

    r.areas("a") shouldBe AreaData(0, 40, 100, -10, -50)
    r.areas("b") shouldBe AreaData(0, 150, 200, -100, 50)
    r.units("line-1") shouldBe UnitData(50, 0, 50)
    r.units("source-a") shouldBe UnitData(60, 40, 100)
    r.units("source-b") shouldBe UnitData(50, 150, 200)
  }

  "Areas with identically named sources and drains" should "not be mixed together" in {
    // note: there was a regression here which occurred only if area "a"
    // had sources at same GHG level -- any difference caused the
    // problem not to show up; so keep the a's source GHGs the same
    val (a1, a2) = (
      An("a", Dn("drain-a-1", 10), Dn("drain-a-2", 100),
        Sn("source-a-1", 5, 1.0), Sn("source-a-2", 100, 1.0)),
      An("b", Dn("drain-b-1", 10), Dn("drain-b-2", 100),
        Sn("source-b-1", 5), Sn("source-b-2", 200, 2.0)))
    val l = Ln("line-1", 50, a1, a2)
    val w = W(a1, a2, l)
    val r = ScalaSimulation.simulate(w)

    // area "a" has -110 drain and +105 sources, -5 total
    // area "b" has -110 drain and +5 on first ghg, and +200 on second ghg,
    // total +95 final

    r.areas("a") shouldBe AreaData(0, 0, 105, -110, 5)
    r.areas("b") shouldBe AreaData(0, 90, 205, -110, -5)
    r.units("line-1") shouldBe UnitData(-5, 55, 50)
    r.units("source-a-1") shouldBe UnitData(5, 0, 5)
    r.units("source-a-2") shouldBe UnitData(100, 0, 100)
    r.units("source-b-1") shouldBe UnitData(5, 0, 5)
    r.units("source-b-2") shouldBe UnitData(110, 90, 200)
  }

  "Disabled resources" should "have no effect in results" in {
    val w1 = W(An("a", Dn("a1", 1000, disabled = true),
      Sn("a2", 1000, disabled = true), Dn("a3", 10), Sn("a4", 10)))
    val r1 = ScalaSimulation.simulate(w1)
    r1.areas("a") shouldBe AreaData(0, 0, 10, -10, 0)
    r1.units("a1") shouldBe UnitData(0, 0, 0)
    r1.units("a2") shouldBe UnitData(0, 0, 0)
    r1.units("a3") shouldBe UnitData(-10, 0, -10)
    r1.units("a4") shouldBe UnitData(10, 0, 10)

    val (a1, a2) = (An("a", Dn("a1", 1000)), An("b", Sn("b1", 1000)))
    val l = Ln("l", 1000, a1, a2, disabled = true)
    val w2 = W(a1, a2, l)
    val r2 = ScalaSimulation.simulate(w2)
    r2.areas("a") shouldBe AreaData(-1000, 0, 0, -1000, 0)
    r2.areas("b") shouldBe AreaData(0, 1000, 1000, 0, 0)
    r2.units("a1") shouldBe UnitData(-1000, 0, -1000)
    r2.units("b1") shouldBe UnitData(0, 1000, 1000)
    r2.units("l") shouldBe UnitData(0, 0, 0)
  }

  "Aggregate types in world" should "result in identical capacities" in {
    val t = DistributionType("uniform-aggregate", None, true, UniformDistributionModel)
    val w = W(An("a", Dn("d", 1000, capacityType = t), Sn("s1", 500, capacityType = t), Sn("s2", 500, capacityType = t)),
      An("b",
        (1 to 100).map { i ⇒
          Sn(s"b$i", 1000, capacityType = UniformDistributionType)
        }:_*))

    val r = ScalaSimulation.simulate(w)

    // area a has only aggregated types in use
    val ad = r.areas("a")
    ad.drain shouldBe -ad.generation +- 1 // rounding off by one is possible

    val (dd, s1d, s2d) = (r.units("d"), r.units("s1"), r.units("s2"))

    s1d.capacity shouldBe s2d.capacity
    -(s1d.capacity + s2d.capacity) shouldBe dd.capacity +- 1  // rounding can cause off by one

    // whereas b has only non-aggregated
    w.areaById("b").get.sources.map { s ⇒ r.units(s.id).capacity }.toSet.size should be > 1
  }

  "Different sources" should "be prioritised by lowest GHG emitter" in {
    def t(drain: Int, used: Seq[String]) = {
      // sources by ghg order: a1 (0), b1 (1), a2 (2), b2 (3), b3 (100)
      val (a, b) = (
        An("a",
          D(drain),
          Sn("a1", 100, 0),
          Sn("a2", 500, 2)),
        An("b",
          Sn("b1", 100, 1),
          Sn("b2", 300, 3),
          Sn("b3", 1000, 100)))
      val l = Ln("l", 10000, a, b)
      val w = W(a, b, l)
      val r = ScalaSimulation.simulate(w)

      val actuallyUsed = w.units.flatMap {
        case s: Source if r.units(s.id).used > 0 ⇒ Seq(s.id)
        case _ ⇒ Seq()
      }

      used.toSet shouldBe actuallyUsed.toSet
    }

    t(0, Seq())
    t(90, Seq("a1"))
    t(100, Seq("a1"))
    t(150, Seq("a1", "b1"))
    t(200, Seq("a1", "b1"))
    t(250, Seq("a1", "b1", "a2"))
    t(700, Seq("a1", "b1", "a2"))
    t(750, Seq("a1", "b1", "a2", "b2"))
    t(1000, Seq("a1", "b1", "a2", "b2"))
    t(1050, Seq("a1", "b1", "a2", "b2", "b3"))
    t(2000, Seq("a1", "b1", "a2", "b2", "b3"))
    t(2050, Seq("a1", "b1", "a2", "b2", "b3")) // drain over all capacity
  }
}
