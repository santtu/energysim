package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model._
import fi.iki.santtu.energysim.simulation.{AreaData, ScalaSimulation, UnitData}
import org.scalatest.{FlatSpec, Matchers}

class ScalaSimulationSpec extends FlatSpec with Matchers {
  "Empty world" should "pass simulation" in {
    val world = World()
    val r = ScalaSimulation.simulate(world)

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
  def S(c: Int, ghg: Double = 0.0) = Source(capacityModel = ConstantCapacityModel(c), ghgPerCapacity = ghg)
  def L(c: Int, a1: Area, a2: Area) = Line(capacityModel = ConstantCapacityModel(c), areas=(a1, a2))

  "Single area" should "use local sources only" in {
    val world = W(A(D(100), S(90, 0.0), S(10, 1.0), S(1000, 10.0)))

    val r = ScalaSimulation.simulate(world)

    r.areas.size shouldBe 1
    r.units.size shouldBe 4

    val a = world.areas(0)

    r.areas(a) shouldBe AreaData(0, 0, 100, -100, 0)
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
}
