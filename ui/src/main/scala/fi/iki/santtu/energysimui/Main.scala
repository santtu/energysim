package fi.iki.santtu.energysimui

import com.github.marklister.base64.Base64._
import fi.iki.santtu.energysim._
import fi.iki.santtu.energysim.model.{Area, Line, World}
import fi.iki.santtu.energysimworker.WorkerState._
import fi.iki.santtu.energysimworker.{Message, Reply, WorkerOperation, WorkerState}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.{Router, _}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import org.scalajs.dom.raw.{MessageEvent, Worker}

import scala.scalajs.js.annotation._


@JSExportTopLevel("EnergySim")
object Main {
  val defaultWorld = Model.from(Data.finland, JsonDecoder)

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

  case class State(world: World,
                   selected: Selection = None,
                   playing: Boolean = false,
                   running: Boolean = false,
                   iterations: Int = 0,
                   runningTime: Double = 0.0,
                   collector: SimulationCollector) {
  }

  type Selection = Option[Either[Area, Line]]

  case class InterfaceProps(ctl: RouterCtl[Pages])

  class InterfaceBackend($: BackendScope[InterfaceProps, State]) {
    val worker = new Worker("worker.js")

    worker.onmessage = { (event: MessageEvent) ⇒
      val reply = event.data.asInstanceOf[Reply]
      println(s"Reply from worker: ${WorkerState(reply.state)}")

      WorkerState(reply.state) match {
        case Started ⇒
          println("Worker has started")
          $.modState(s ⇒ s.copy(running = true)).runNow()
        case Stopped =>
          println("Worker has stopped")
          $.modState(s ⇒ s.copy(running = false)).runNow()
        case Result ⇒
          println("Worker gave result")
          val result = io.circe.scalajs.convertJsToJson(reply.result) match {
            case Left(error) ⇒ throw error
            case Right(json) ⇒ JsonDecoder.decodeResultFromJson(json)
          }
          // this is a bit evil, but simulation collector is an object,
          // not a case class, so we modify it directly ...
          $.modState(s ⇒ {
            s.collector += result
            s.copy(iterations = s.iterations + reply.rounds,
              runningTime = s.runningTime + reply.interval)
          }).runNow()
      }
    }

    def send(m: Message): Unit = {
      worker.postMessage(m)
      println(s"Sent worker message: ${WorkerOperation(m.op)}")
    }

    def init: Callback = {
      $.state |> { s ⇒
        println(s"init called, state=$s")

        val worldJs = io.circe.scalajs.convertJsonToJs(JsonDecoder.encodeAsJson(s.world))
        send(Message(WorkerOperation.SetWorld, world = worldJs))
      }
    }

    def uninit: Callback = {
      $.state |> { s ⇒
        println(s"uninit called, state=$s")
        worker.terminate()
      }
    }

    val start = {
      $.modState(_.copy(playing = true)) >>
        Callback { send(Message(WorkerOperation.Start)) } >>
        Callback.log("START CLICKED!")
    }
    val stop = {
      $.modState(_.copy(playing = false)) >>
        Callback { send(Message(WorkerOperation.Stop)) } >>
        Callback.log("STOP CLICKED!")
    }

    def isAreaFocused(area: Area)(implicit s: State): Boolean =
      s.selected.contains(Left(area))

    def isLineFocused(line: Line)(implicit s: State): Boolean =
      s.selected.contains(Right(line))

    def selectArea(a: Area): Callback =
      $.modState(s ⇒ s.copy(selected = Some(Left(a))))

    def selectLine(l: Line): Callback =
      $.modState(s ⇒ s.copy(selected = Some(Right(l))))

    def selectNone: Callback =
      $.modState(s ⇒ s.copy(selected = None))

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


    def render(p: InterfaceProps, s: State): VdomElement = {
      implicit val state = s
//      println(s"render: p=$p s=$s")
      <.div(
        // header contains controllers and graphs
        <.header(^.className := "row pb-3",
          // controls (play stop) are here
          <.div(^.className := "col-2",
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-primary",
              ^.onClick --> start,
              ^.disabled := s.running,
              "PLAY").when(!s.playing),
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-primary",
              ^.onClick --> stop,
              ^.disabled := !s.running,
              "STOP").when(s.playing),
          ),

          // graphs come here
          <.div(^.className := "col",
            <.small(
              ^.className := "text-muted",
              s"${s.iterations} iterations ",
              f"in ${s.runningTime}%.1f seconds".when(s.runningTime > 0.0)),
            <.br,
            <.small(
              f"Loss: ${s.collector.global.loss.percentage}%.1f%%", <.br,
              f"Generation: ${s.collector.global.generation.mean}%.0f±${s.collector.global.generation.dev}%.0f MW", <.br
            ).when(s.collector.rounds > 0)),

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
//            <.div(
//              ^.className := "col-md-8 row",
//              areas.toVdomArray { case (id, info) ⇒
//                val a = s.world.areaById(id).get
//                <.div(
//                  ^.key := id,
//                  ^.className := "col",
//                  area(a, info)
//                )
//              },
//              lines.toVdomArray(p ⇒
//                <.div(
//                  ^.key := p._1,
//                  ^.className := "col",
//                  line(s.world.lineById(p._1).get, p._2)))),
            <.div(^.className := "col-md-8 row",
              AreasMap(AreasMap.Props(
                world = s.world,
                areaData = s.world.areas.map(area ⇒ area → areas(area.id)).toMap,
                lineData = s.world.lines.map(line ⇒ line → lines(line.id)).toMap,
                selected = s.selected,
                updateSelection = sel => $.modState(_.copy(selected = sel))))
            ),
            <.div(
              ^.className := "col-md-4 main-right",
              // if rounds > 0 we have statistics to show, either
              // running or paused
              if (s.collector.rounds > 0)
                s.selected match {
                  case Some(Left(area)) ⇒
                    val data = s.collector.areas(area.id)
                    <.div(
                      ^.className := "stats",
                      AreaStats((area, data)))
                  case Some(Right(line)) ⇒
                    val data = s.collector.lines(line.id)
                    <.div(
                      ^.className := "stats",
                      LineStats((line, data)))
                  case None ⇒ EmptyVdom // maybe global stats instead?
                }
              else
                EmptyVdom,

              <.div(
                ^.className := "info",
                s.selected match {
                  case Some(Left(area)) ⇒
                    AreaInfo(area)

                  //                    require(area.drains.size == 1)
//                    val drain = area.drains.head
//                    val sourceSum = area.sources.map(_.unitCapacity).sum
                  //                      s"AREA: ${dataFor(area).name} (connected to ",
                  //                      s.world.linesForArea(area).toVdomArray( line ⇒ {
                  //                        val other = line.areasSeq.filter(_ != area).head
                  //                        <.span(
                  //                          ^.key := line.id,
                  //                          <.span(
                  //                            ^.className := "text-primary",
                  //                            ^.onClick --> selectArea(other),
                  //                            dataFor(other).name), " ")
                  //                      }), ")",
                  //                      <.br,
                  //                      s"Drain: ${drain.unitCapacity} MW", <.br,
                  //                      s"Drain model: ${drain.capacityType.name}", <.br,
                  //                      s"Sources: ${sourceSum} MW", <.br,
                  //                      <.ul(
                  //                        area.sources.toVdomArray(source ⇒
                  //                          <.li(
                  //                            ^.key := source.id,
                  //                            s"${source.name}", <.br,
                  //                            s"Maximum capacity: ${source.unitCapacity} MW", <.br,
                  //                            s"Capacity model: ${source.capacityType.name}"))),
                  //                      s"Connections:", <.br,
                  //                      <.ul(
                  //                        s.world.linesForArea(area).toVdomArray(line ⇒
                  //                          <.li(
                  //                            ^.key := line.id,
                  //                            <.span(
                  //                              ^.className := "text-primary",
                  //                              ^.onClick --> selectLine(line),
                  //                              dataFor(line).name))))
                  //                  )
                  case Some(Right(line)) ⇒
                    LineInfo(line)
                  //                      s"LINE: ${dataFor(line).name}", <.br,
                  //                      s"Between ",
                  //                      <.span(
                  //                        ^.className := "text-primary",
                  //                        ^.onClick --> selectArea(line.area1),
                  //                        dataFor(line.area1).name),
                  //                      " and ",
                  //                      <.span(
                  //                        ^.className := "text-primary",
                  //                        ^.onClick --> selectArea(line.area2),
                  //                        dataFor(line.area2).name), <.br,
                  //                      s"Maximum capacity: ${line.unitCapacity} MW", <.br,
                  //                      s"Capacity model: ${line.capacityType.name}"
                  //                    )
                  case None ⇒
                    "No area or line selected"
                }
              )
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
    private def encode(w: World): String = JsonDecoder.encode(w).getBytes("UTF-8").toBase64
    private def decode(s: String): World = JsonDecoder.decode(new String(s.toByteArray, "UTF-8"))
    def apply(w: World): WithWorld = WithWorld(encode(w))
    def from(ww: WithWorld): World = decode(ww.encoded)
  }

  val defaultPage = WithWorld(defaultWorld)

  val routerConfig = RouterConfigDsl[Pages].buildConfig {
    dsl =>
      import dsl._

      val worldRoute =
        dynamicRouteCT("#" / string(".+").caseClass[WithWorld]) ~>
          dynRenderR((w: WithWorld, ctl) => {
            val ui = UserInterfaceComponent(w.world, ctl)
            ui
          })


      ( emptyRule
        | worldRoute
        ).notFound(redirectToPage(defaultPage)(Redirect.Replace))
  }

  def UserInterfaceComponent(w: World, ctl: RouterCtl[Pages]) = {
    val ctor = ScalaComponent.builder[InterfaceProps]("Interface")
      .initialState(State(w, collector = SimulationCollector(w)))
      .renderBackend[InterfaceBackend]
      .componentWillMount(_.backend.init)
      .componentWillUnmount(_.backend.uninit)
      .build
    ctor(InterfaceProps(ctl))
  }

  @JSExport
  def main(args: Array[String]): Unit = {
    val router = Router(BaseUrl.until_#, routerConfig)

    router().renderIntoDOM(dom.document.getElementById("playground"))

    println(s"World: ${defaultWorld}")
    println(s"units: ${defaultWorld.units}")

    // would need to wait for ready


//    println(s"posted [SetWorld, $defaultWorld]")
  }
}
