package fi.iki.santtu.energysimui

import fi.iki.santtu.energysim.model.World
import fi.iki.santtu.energysim.{JsonDecoder, SimulationCollector}
import fi.iki.santtu.energysimui.Main._
import fi.iki.santtu.energysimworker.WorkerState.{Result, Started, Stopped}
import fi.iki.santtu.energysimworker.{Message, Reply, WorkerOperation, WorkerState}
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.VdomElement
import org.scalajs.dom.raw.MessageEvent
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import org.scalajs.dom.raw.Worker
import japgolly.scalajs.react.vdom.html_<^._

import scala.util.Random


object UserInterface {
  val roundsPerUpdate = 1
  val historySize = 100

  case class Props(world: World,
                   defaultWorld: World,
                   // actually svg
                   worldMap: String,
                   controller: RouterCtl[Pages])

  case class State(selected: Selection = NoSelection,
                   playing: Boolean = false,
                   running: Boolean = false,
                   iterations: Int = 0,
                   runningTime: Double = 0.0,
                   collector: Option[SimulationCollector] = None)

  class Backend($: BackendScope[Props, State]) {
    val worker = new Worker("worker.js")

    def receive(event: MessageEvent): Unit = {
      val reply = event.data.asInstanceOf[Reply]
      //      println(s"Reply from worker: ${WorkerState(reply.state)}")

      WorkerState(reply.state) match {
        case Started ⇒
          println("Worker has started")
          $.modState(s ⇒ s.copy(running = true)).runNow()
        case Stopped =>
          println("Worker has stopped")
          $.modState(s ⇒ s.copy(running = false)).runNow()
        case Result ⇒
          //          println("Worker gave result")
          val result = io.circe.scalajs.convertJsToJson(reply.result) match {
            case Left(error) ⇒ throw error
            case Right(json) ⇒ JsonDecoder.decodeResultFromJson(json)
          }
          // this is a bit evil, but simulation collector is an object,
          // not a case class, so we modify it directly ...
//          (($.props |> { p ⇒ p.collector += result }) >>
            $.modState(s ⇒ {
              s.collector.get += result
              s.copy(
                iterations = s.iterations + reply.rounds,
                runningTime = s.runningTime + reply.interval)
            }).runNow()
      }
    }

    worker.onmessage = receive

    def init: Callback =
      $.props >>= {
        p ⇒
          $.modState(s ⇒ s.copy(collector =
            Some(SimulationCollector(p.world, historySize))))
      }

    def uninit: Callback =
      Callback {
        worker.terminate()
      }

    def send(m: Message): Unit = {
      worker.postMessage(m)
      println(s"Sent worker message: ${WorkerOperation(m.op)}")
    }

    def sendWorld(w: World) = {
      send(Message(WorkerOperation.SetWorld,
        value = io.circe.scalajs.convertJsonToJs(JsonDecoder.encodeAsJson(w))))
    }


    val startSimulation = {
      $.modState(_.copy(playing = true)) >>
        ($.props |> { p ⇒ sendWorld(p.world) }) >>
        Callback { send(Message(WorkerOperation.Start, roundsPerUpdate)) }
    }

    val stopSimulation = {
      $.modState(_.copy(playing = false)) >>
        Callback { send(Message(WorkerOperation.Stop)) }
    }

    // this is called when the world is changed **after** navigation
    // has occurred (e.g. if user initiates, use changeWorld instead) --
    // this needs to really only check if we are running a simulation
    // right now in which case we need to send the world to simulator
    // (and also to clear the collector)
    def updateWorld(newWorld: World): Callback = {
//      println(s"updateWorld: new=${newWorld.hashCode()}")
      // we need to get both the props and old state
      ($.state |> {
        case state if state.playing ⇒
          sendWorld(newWorld)
        case _ ⇒
      }) >> $.forceUpdate
    }

    // update the current page for the new world (if it differs
    // from the old) -- this will cause props change to propagate
    // to updateWorld later
    def changeWorld(world: World): Callback = {
//      println(s"changeWorld: new=${world.hashCode()}")

//      val p = WorldPage(world)
//      val p2 = WorldPage(p.world)
//      assert(p == p2)   // json serialization is not stable, strings can differ
//      assert(world == p.world, s"world and page world differ:\n$world\n${p.world}")
//      assert(world == p2.world, "re-paged world and world differ")

      $.props >>= {
        case props if props.world != world ⇒ props.controller.set(WorldPage(world))
        case _ ⇒ Callback.empty
      }
    }

    def render(s: State, p: Props): VdomElement = {
      val world = p.world
      val collector: SimulationCollector = s.collector.get

      <.div(
        // header contains controllers and graphs
        <.header(^.className := "row pb-3",
          // controls (play stop) are here
          <.div(^.className := "col-2",
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-primary start-stop",
              ^.onClick --> startSimulation,
              ^.disabled := s.running,
              "START").when(!s.playing),
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-primary start-stop",
              ^.onClick --> stopSimulation,
              ^.disabled := !s.running,
              "STOP").when(s.playing)),

          // graphs come here
          <.div(^.className := "col",
            <.div(
              <.small(
                ^.className := "text-muted",
                s"${s.iterations} iterations ",
                f"in ${s.runningTime}%.1f seconds"),
//              <.br,
//              <.small(
//                f"Loss: ${p.collector.global.loss.percentage}%.1f%%", <.br,
//                f"Generation: ${p.collector.global.generation.mean}%.0f±${p.collector.global.generation.dev}%.0f MW", <.br,
//                f"GHG: ${p.collector.global.ghg.mean * ghgScaleFactor}%.0f±${p.collector.global.ghg.dev * ghgScaleFactor}%.0f t/a"
//              )
            ).when(s.iterations > 0),
            <.img(^.className := "starting",
              ^.src := "images/loading.svg").when(s.iterations == 0 && s.playing)),

          // summary information
          <.div(^.className := "col-2",
            //            s"World: ${s.world.name}",
            //            <.br,
            //            s"Units: ${s.world.units.length}", <.br,
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-warning",
              //|              ^.onClick --> p.ctl.set(defaultPage),
              ^.onClick --> changeWorld(p.defaultWorld),
              ^.disabled := (s.playing || world == p.defaultWorld),
              "RESET"))),

        <.div(^.className := "row",
          <.div(^.className := "col-md-8",
            WorldMap(p.worldMap,
              selected ⇒ $.modState(s ⇒ s.copy(selected = selected)))),

          <.div(
            ^.className := "col-md-4 main-right",
            // if rounds > 0 we have statistics to show, either
            // running or paused
            if (collector.rounds > 0)
              s.selected match {
                case AreaSelection(id) ⇒
                  val area = world.areaById(id).get
                  val data = collector.areas(id)
//                  <.div(
//                    ^.className := "stats",
//                    AreaStats((area, data)))
                  <.div(
                    ^.className := "statistics",
                    "Power balance", <.br,
                    <.span(^.className := "current",
                      f"${data.total / 1e3}%.1s GW"),
                    Sparklines(data = data.total.toSeq)(
                      SparklinesLine(style = Map("fill" → "none"))(),
                      SparklinesSpots()(),
                      SparklinesReferenceLine(`type` = "custom", value = 0.0)(),
                    ),
                    "Production",  <.br,
                    <.span(^.className := "current",
                      f"${data.generation / 1000}%.1s GW"),
                    Sparklines(data = data.generation.toSeq, min = 0.0)(
                      SparklinesLine(color = "#253e56")(),
                      SparklinesSpots()(),
                      SparklinesReferenceLine(`type` = "custom", value = data.generation.mean)(),
                    ),
                    "Transfer",  <.br,
                    <.span(^.className := "current",
                      f"${data.transfer / 1000}%.1s GW"),
                    Sparklines(data = data.transfer.toSeq)(
                      SparklinesLine(style = Map("fill" → "none"))(),
                      SparklinesSpots()(),
                      SparklinesReferenceLine(`type` = "custom", value = 0.0)(),
                    ),
                  ).when(collector.rounds > 0)

                case LineSelection(id) ⇒
                  val line = world.lineById(id).get
                  val data = collector.lines(id)
                  <.div(
                    ^.className := "stats",
                    LineStats((line, data)))
                case NoSelection ⇒
                  <.div(
                    ^.className := "statistics",
                    "Power balance", <.br,
                    <.span(^.className := "current",
                      f"${collector.global.total / 1e3}%.1s GW"),
                    Sparklines(data = collector.global.total.toSeq)(
                      SparklinesLine(style = Map("fill" → "none"))(),
                      SparklinesSpots()(),
                      SparklinesReferenceLine(`type` = "custom", value = 0.0)(),
                    ),
                    "Production", <.br,
                    <.span(^.className := "current",
                      f"${collector.global.generation / 1e3}%.1s GW"),
                    Sparklines(data = collector.global.generation.toSeq, min = 0.0)(
                      SparklinesLine()(),
                      SparklinesSpots()(),
                      SparklinesReferenceLine(`type` = "custom", value = collector.global.generation.mean)(),
                    ),
                    "CO2 emissions",  <.br,
                    <.span(^.className := "current",
                      f"${collector.global.ghg * Main.ghgScaleFactor / 1e6}%.1s Mt/a"),
                    Sparklines(data = collector.global.ghg.toSeq, min = 0.0)(
                      SparklinesLine(color = "#253e56")(),
                      SparklinesSpots()(),
                      SparklinesReferenceLine(`type` = "custom", value = collector.global.ghg.mean)(),
                    ),
                    "Import", <.br,
                    <.span(^.className := "current",
                      f"${collector.external.transfer / -1e3}%.1s GW"),
                    Sparklines(data = (collector.external.transfer * -1).toSeq, min = 0.0)(
                      SparklinesLine()(),
                      SparklinesSpots()(),
                      SparklinesReferenceLine(`type` = "custom", value = -collector.external.transfer.mean)(),
                    ),
                  ).when(collector.rounds > 0)
              }
            else
              EmptyVdom,

            <.div(
              ^.className := "info",
              s.selected match {
                case AreaSelection(id) ⇒
                  AreaInfo(AreaInfo.Props(world.areaById(id).get,
                    { newArea ⇒
                      changeWorld(world.update(newArea))
                    }))
                case LineSelection(id) ⇒
                  val line = world.lineById(id).get
                  LineInfo(LineInfo.Props(line,
                    { newLine ⇒
                      println(s"old line: ${line.unitCapacity} ${line.disabled} new ${newLine.unitCapacity} ${newLine.disabled}")
                      changeWorld(world.update(newLine))
                    }))
                case NoSelection ⇒
                  // show global statistics here
//                  "No area or line selected"
//                  val data = Seq.fill(20)(Random.nextDouble() * 20000.0 - 10000.0)
//                  println(s"data=$data")
                  EmptyVdom // maybe global stats instead?
              }
            ))))
    }
  }

  val component = ScalaComponent.builder[Props]("UserInterface")
    .initialState(State())
    .renderBackend[Backend]
    .componentWillMount(_.backend.init)
    .componentWillUnmount(_.backend.uninit)
    .componentWillReceiveProps(i ⇒ i.backend.updateWorld(i.nextProps.world))
    .build

  def apply(world: World, defaultWorld: World, worldMap: String, ctl: RouterCtl[Pages]) = {
    component(Props(world = world,
      defaultWorld = defaultWorld,
      worldMap = worldMap,
//      collector = SimulationCollector(world, historySize),
      controller = ctl))
  }
}
