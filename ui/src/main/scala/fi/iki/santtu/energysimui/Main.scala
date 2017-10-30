package fi.iki.santtu.energysimui

import fi.iki.santtu.energysim.{JsonDecoder, Model, SimulationCollector}
import fi.iki.santtu.energysim.model.{Area, Drain, Line, Source, World}
import fi.iki.santtu.energysim.simulation.ScalaSimulation

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel, JSGlobal, JSImport}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.Router
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import japgolly.scalajs.react.extra.router._
import com.github.marklister.base64.Base64._
import japgolly.scalajs.react.vdom.TagOf
import org.scalajs.dom.html.Div


// @JSImport("bootstrap", JSImport.Namespace)
// @js.native
// object bootstrap extends js.Object {
// }

@JSExportTopLevel("EnergySim")
object Main {
  @JSExport("defaultWorld")
  val defaultWorld = Model.from(Data.finland, JsonDecoder)

  @JSExport
  def run(rounds: Int, world: World = defaultWorld): Unit = {
    val simulator = ScalaSimulation
    val collector = SimulationCollector(world)

    val t0 = System.nanoTime()
    val result = simulator.simulate(world, rounds)
    val t1 = System.nanoTime()
    val ms = (t1 - t0).toDouble / 1e6
    collector += result

    println(f"Simulation ${rounds} took ${ms / 1000.0}%.3f s, ${ms / rounds}%.2f ms/round")

    print(SimulationCollector.summary(collector))
  }

  case class State(world: World,
                   focused: Option[Either[Area,Line]] = None,
                   playing: Boolean = false) {
  }

  object State {
    val default = State(defaultWorld)
  }

  class Interface($: BackendScope[Props, State]) {
    val start = {
      $.modState(_.copy(playing = true)) >>
        Callback.log("START CLICKED!")
    }
    val stop = {
      $.modState(_.copy(playing = false)) >>
        Callback.log("STOP CLICKED!")
    }
//
//    def source(s: Source)(implicit a: Area) =
//      <.div(
//        ^.className := "source",
//        s.name, <.br,
//        s.unitCapacity.toString, " MW")
//
//    def remove(a: Area, d: Drain) = {
//      $.props |> (_.ctl) >>= {
//        ctl ⇒
//          $.state |> (_.world) >>= {
//            w ⇒
//              CallbackTo {
//                (ctl, w.remove(a, d))
//              }
//          }
//      } >>= {
//        case (ctl, w) ⇒
//          $.modState {
//            s ⇒
////              println(s"XXX ctl=$ctl w=$w s=$s")
//              s.copy(world = w)
//          } >> ctl.set(WithWorld(w))
//      }
//    }
//
//    def drain(d: Drain)(implicit a: Area) =
//      <.div(
//        ^.className := "drain",
//        d.name, <.br,
//        d.unitCapacity.toString, " MW", <.br,
//        <.button(
//          ^.`type` := "button",
//          ^.className := "btn btn-danger",
//          ^.onClick --> remove(a, d).void,
//          <.i(^.className := "fa fa-trash-o")))

    def isAreaFocused(area: Area)(implicit s: State): Boolean =
      s.focused.contains(Left(area))

    def isLineFocused(line: Line)(implicit s: State): Boolean =
      s.focused.contains(Right(line))

    def selectArea(a: Area): Callback =
      $.modState(s ⇒ s.copy(focused = Some(Left(a))))

    def selectLine(l: Line): Callback =
      $.modState(s ⇒ s.copy(focused = Some(Right(l))))

    def selectNone: Callback =
      $.modState(s ⇒ s.copy(focused = None))

    def area(a: Area, data: AreaData)(implicit s: State) =
      <.div(^.className := "area",
        (^.className := "focused").when(isAreaFocused(a)),
        (^.onClick --> selectArea(a)).when(!isAreaFocused(a)),
        (^.onClick --> selectNone).when(isAreaFocused(a)),
        <.span(^.className := "name", data.name))

    def line(l: Line, data: LineData)(implicit s: State) =
      <.div(^.className := "line",
        (^.className := "focused").when(isLineFocused(l)),
        (^.onClick --> selectLine(l)).when(!isLineFocused(l)),
        (^.onClick --> selectNone).when(isLineFocused(l)),
          <.span(^.className := "name", data.name))

    /**
      * Container for UI-related things for each area, e.g. visible name,
      * description etc.
      */
    case class AreaData(name: String)

    val areas: Map[String, AreaData] = Map(
      "north" → AreaData("Pohjoinen"),
      "south" → AreaData("Eteläinen"),
      "east" → AreaData("Itäinen"),
      "west" → AreaData("Läntinen"),
      "central" → AreaData("Keskinen"),
    )

    def dataFor(a: Area) = areas(a.name)

    /**
      * Container for UI-related stuff for lines.
      */

    case class LineData(name: String)

    val lines: Map[String, LineData] = Map(
      "west-south" → LineData("I-E"),
      "west-central" → LineData("L-K"),
      "south-east" → LineData("E-I"),
      "south-central" → LineData("E-K"),
      "east-central" → LineData("I-K"),
      "central-north" → LineData("K-P")
    )

    def dataFor(l: Line) = lines(l.name)

    def render(p: Props, s: State): VdomElement = {
      implicit val state = s
      println(s"render: p=$p s=$s")
      <.div(
        // header contains controllers and graphs
        <.header(^.className := "row pb-3",
          // controls (play stop) are here
          <.div(^.className := "col-2",
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-primary",
              ^.onClick --> start,
              "PLAY").when(!s.playing),
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-primary",
              ^.onClick --> stop,
              "STOP").when(s.playing)),

          // graphs come here
          <.div(^.className := "col",
            "GRAPHS HERE"),

          // summary information
          <.div(^.className := "col-2",
//            s"World: ${s.world.name}",
//            <.br,
//            s"Units: ${s.world.units.length}", <.br,
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-warning",
              ^.onClick --> p.ctl.set(defaultPage),
              ^.disabled := (s.playing || s.world == defaultWorld),
              "RESET"))),

        <.div(
          // next comes actual world information
          <.div(^.className := "row",
            <.div(
              ^.className := "col-lg-8",
              areas.toVdomArray(p ⇒
                <.div(
                  ^.key := p._1,
                  ^.className := "col-md",
                  area(s.world.areaByName(p._1).get, p._2))),
              lines.toVdomArray(p ⇒
                <.div(
                  ^.key := p._1,
                  ^.className := "col-md",
                  line(s.world.lineByName(p._1).get, p._2)))),
            <.div(
              ^.className := "col-lg-4 row",
              s.focused match {
                case Some(Left(area)) ⇒
                  require(area.drains.size == 1)
                  val drain = area.drains.head
                  val sourceSum = area.sources.map(_.unitCapacity).sum
                  <.div(
                    s"AREA: ${dataFor(area).name}", <.br,
                    s"Drain: ${drain.unitCapacity} MW", <.br,
                    s"Drain model: ${drain.capacityType.name}", <.br,
                    s"Sources: ${sourceSum} MW", <.br,
                    <.ul(
                      area.sources.toVdomArray(source ⇒
                        <.li(
                          ^.key := source.name,
                          s"${source.name}", <.br,
                          s"Maximum capacity: ${source.unitCapacity} MW", <.br,
                          s"Capacity model: ${source.capacityType.name}"))),
                    s"Connections:", <.br,
                    <.ul(
                    s.world.linesForArea(area).toVdomArray(l ⇒
                      <.li(
                        ^.key := l.name,
                        <.span(
                          ^.className := "text-primary",
                          ^.onClick --> selectLine(l),
                          dataFor(l).name)))))
                case Some(Right(line)) ⇒
                  <.div(
                    s"LINE: ${dataFor(line).name}", <.br,
                    s"Between ",
                    <.span(
                      ^.className := "text-primary",
                      ^.onClick --> selectArea(line.area1),
                      dataFor(line.area1).name),
                    " and ",
                    <.span(
                      ^.className := "text-primary",
                      ^.onClick --> selectArea(line.area2),
                      dataFor(line.area2).name), <.br,
                    s"Maximum capacity: ${line.unitCapacity} MW", <.br,
                    s"Capacity model: ${line.capacityType.name}")
                case None ⇒
                  <.div(
                    "No area or line selected")
              }
          ))),

//        // followed by the terminating footer
//        <.footer(^.className := "row",
//          "FOOTER HERE")
      )
    }
  }

  sealed trait Pages
  case object Home extends Pages
  case class WithWorld(encoded: String) extends Pages {
    def world = WithWorld.from(this)
  }
  object WithWorld {
    private def encode(w: World): String = JsonDecoder.encode(w).toBase64
    private def decode(s: String): World = JsonDecoder.decode(s.toByteArray)
    def apply(w: World): WithWorld = WithWorld(encode(w))
    def from(ww: WithWorld): World = decode(ww.encoded)
  }

  val defaultPage = WithWorld(defaultWorld)

  case class Props(ctl: RouterCtl[Pages])

  val routerConfig = RouterConfigDsl[Pages].buildConfig {
    dsl =>
      import dsl._

      val worldRoute =
        dynamicRouteCT("#" / string(".+").caseClass[WithWorld]) ~>
          dynRenderR((w, ctl) => {
            val ui = UserInterfaceComponent(w.world, ctl)
            println(s"worldroute: w=$w ctl=$ctl ui=$ui")
            ui
          })


      ( emptyRule
        | worldRoute
        ).notFound(redirectToPage(defaultPage)(Redirect.Replace))
  }

  def UserInterfaceComponent(w: World, ctl: RouterCtl[Pages]) = {
    val ctor = ScalaComponent.builder[Props]("Interface")
      .initialState(State(w))
      .renderBackend[Interface]
      .build
    ctor(Props(ctl))
  }

  @JSExport
  def main(args: Array[String]): Unit = {
    val router = Router(BaseUrl.until_#, routerConfig)

    router().renderIntoDOM(dom.document.getElementById("playground"))

    println(s"World: ${defaultWorld}")
    println(s"units: ${defaultWorld.units}")
  }
}
