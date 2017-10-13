package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model.{Area, ConstantCapacityModel, Drain, Line, Source, Unit, World}
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

  def W(stuff: Any*) =
    World(areas = stuff.collect { case a: Area ⇒ a },
      lines = stuff.collect { case l: Line ⇒ l })
  def A(units: Unit*) =
    Area(drains = units.collect { case d: Drain ⇒ d },
      sources = units.collect { case s: Source ⇒ s})
  def An(name: String, units: Unit*) =
    Area(name = name,
      drains = units.collect { case d: Drain ⇒ d },
      sources = units.collect { case s: Source ⇒ s})
  def D(c: Int) = Drain(capacityModel = ConstantCapacityModel(c))
  def Dn(name: String, c: Int) = Drain(name = name, capacityModel = ConstantCapacityModel(c))
  def S(c: Int, ghg: Double = 0.0) = Source(capacityModel = ConstantCapacityModel(c), ghgPerCapacity = ghg)
  def Sn(name: String, c: Int, ghg: Double = 0.0) = Source(name = name, capacityModel = ConstantCapacityModel(c), ghgPerCapacity = ghg)
  def L(c: Int, a1: Area, a2: Area) = Line(capacityModel = ConstantCapacityModel(c), areas=(a1, a2))
  def Ln(name: String, c: Int, a1: Area, a2: Area) = Line(name = name, capacityModel = ConstantCapacityModel(c), areas=(a1, a2))

  "Single area" should "use local sources only" in {
    val world = W(A(D(100), S(90, 0.0), S(10, 1.0), S(1000, 10.0)))

    val r = ScalaSimulation.simulate(world)

    r.areas.size shouldBe 1
    r.units.size shouldBe 4

    val a = world.areas(0)

    r.areas(a) shouldBe AreaData(0, 1000, 1100, -100, 0)
    r.units(a.drains(0)) shouldBe UnitData(-100, 0, -100)
    r.units(a.sources(0)) shouldBe UnitData(90, 0, 90)
    r.units(a.sources(1)) shouldBe UnitData(10, 0, 10)
    r.units(a.sources(2)) shouldBe UnitData(0, 1000, 1000)
  }

  "Two areas with interconnection" should "have no transfers if all can be locally powered" in {
    val (a1, a2) = (An("a", D(100), S(200, 0.0)), An("b", D(50), S(100)))
    val l = L(1000, a1, a2)
    val world = W(a1, a2, l)

    val r = ScalaSimulation.simulate(world)

    r.areas(a1) shouldBe AreaData(0, 100, 200, -100, 0)
    r.areas(a2) shouldBe AreaData(0, 50, 100, -50, 0)
    r.units(l) shouldBe UnitData(0, 1000, 1000)
  }

  "Area without power" should "be able to get all power via transfer" in {
    val (a1, a2) = (An("a", S(100), S(100, 1.0)), An("b", D(150)))
    val l = L(1000, a1, a2)
    val w = W(a1, a2, l)

    val r = ScalaSimulation.simulate(w)

    r.areas(a1) shouldBe AreaData(0, 50, 200, 0, -150)
    r.areas(a2) shouldBe AreaData(0, 0, 0, -150, 150)
    r.units(l) shouldBe UnitData(150, 850, 1000)
  }

  it should "work also in reverse" in {
    val (a1, a2) = (An("a", S(100), S(100, 1.0)), An("b", D(150)))
    val l = L(1000, a2, a1)
    val w = W(a1, a2, l)

    val r = ScalaSimulation.simulate(w)

    r.areas(a1) shouldBe AreaData(0, 50, 200, 0, -150)
    r.areas(a2) shouldBe AreaData(0, 0, 0, -150, 150)
    r.units(l) shouldBe UnitData(-150, 1150, 1000)
  }

  it should "receive over multiple lines" in {
    val (a1, a2, a3, b) = (An("a1", S(50)), An("a2", S(50)), An("a3", S(50)), An("b", D(150)))
    val (l1, l2, l3) = (L(60, a1, b), L(60, b, a2), L(60, a3, b))
    val w = W(a1, a2, a3, b, l1, l2, l3)

    val r = ScalaSimulation.simulate(w)

    r.areas(a1) shouldBe AreaData(0, 0, 50, 0, -50)
    r.areas(a2) shouldBe AreaData(0, 0, 50, 0, -50)
    r.areas(a3) shouldBe AreaData(0, 0, 50, 0, -50)
    r.areas(b) shouldBe AreaData(0, 0, 0, -150, 150)

    // lines do not have preference, we can really only check for the
    // total sum over all
    val ld = r.units.collect { case (l: Line, ld) ⇒ ld }

    ld.map(_.used.abs).sum shouldBe 150
    ld.map(_.excess).sum shouldBe 130 // not 30 because one of the lines goes in reverse and has excess of 110
  }

  "Underpowered network" should "prefer local power" in {
    val (a1, a2, a3) = (An("a", D(100), S(50)), An("b", D(10), S(5)), An("c", D(1000), S(900)))
    val (l1, l2, l3) = (L(10000, a1, a2), L(10000, a2, a3), L(10000, a1, a3))
    val w = W(a1, a2, a3, l1, l2, l3)

    val r = ScalaSimulation.simulate(w)

    r.areas(a1) shouldBe AreaData(-50, 0, 50, -100, 0)
    r.areas(a2) shouldBe AreaData(-5, 0, 5, -10, 0)
    r.areas(a3) shouldBe AreaData(-100, 0, 900, -1000, 0)

    r.units(l1) shouldBe UnitData(0, 10000, 10000)
    r.units(l2) shouldBe UnitData(0, 10000, 10000)
    r.units(l3) shouldBe UnitData(0, 10000, 10000)
  }

  "Transfers through an area" should "not affect between area's totals" in {
    val (a1, a2, a3) = (An("a", D(1000)), An("b", S(1000)), An("c"))
    val (l1, l2) = (L(1000, a3, a1), L(1, a3, a2))
    val w = W(a1, a2, a3, l1, l2)

    val r = ScalaSimulation.simulate(w)

    r.areas(a1) shouldBe AreaData(-999, 0, 0, -1000, 1)
    r.areas(a2) shouldBe AreaData(0, 999, 1000, 0, -1)
    r.areas(a3) shouldBe AreaData(0, 0, 0, 0, 0)

    r.units(l1) shouldBe UnitData(1, 999, 1000)
    r.units(l2) shouldBe UnitData(-1, 2, 1) // remember a2->a3 dir
  }

  "Unused capacity" should "be attributed back to sources" in {
    // one area that meets all with ghg = 0, cannot transfer all
    // capacity out, another that needs high ghg but has excess of that
    val (a1, a2) = (An("a", D(10), S(100)), An("b", D(100), S(200, 1.0)))
    val l = L(50, a1, a2)
    val w = W(a1, a2, l)
    val r = ScalaSimulation.simulate(w)

    r.areas(a1) shouldBe AreaData(0, 40, 100, -10, -50)
    r.areas(a2) shouldBe AreaData(0, 150, 200, -100, 50)
    r.units(l) shouldBe UnitData(50, 0, 50)
    r.units(a1.sources(0)) shouldBe UnitData(60, 40, 100)
    r.units(a2.sources(0)) shouldBe UnitData(50, 150, 200)
  }

  "Areas with identically named sources and drains" should "not be mixed together" in {
    // note: there was a regression here which occurred only if area "a"
    // had sources at same GHG level -- any difference caused the
    // problem not to show up; so keep the a's source GHGs the same
    val (a1, a2) = (
      An("a", Dn("drain", 10), Dn("drain", 100),
        Sn("source", 5, 1.0), Sn("source", 100, 1.0)),
      An("b", Dn("drain", 10), Dn("drain", 100),
        Sn("source", 5), Sn("source", 200, 2.0)))
    val l = L(50, a1, a2)
    val w = W(a1, a2, l)
    val r = ScalaSimulation.simulate(w)

    // area "a" has -110 drain and +105 sources, -5 total
    // area "b" has -110 drain and +5 on first ghg, and +200 on second ghg,
    // total +95 final

    r.areas(a1) shouldBe AreaData(0, 0, 105, -110, 5)
    r.areas(a2) shouldBe AreaData(0, 90, 205, -110, -5)
    r.units(l) shouldBe UnitData(-5, 55, 50)
    r.units(a1.sources(0)) shouldBe UnitData(5, 0, 5)
    r.units(a1.sources(1)) shouldBe UnitData(100, 0, 100)
    r.units(a2.sources(0)) shouldBe UnitData(5, 0, 5)
    r.units(a2.sources(1)) shouldBe UnitData(110, 90, 200)
  }
}
