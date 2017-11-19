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


object UserInterface {
  case class Props(world: World,
                   defaultWorld: World,
                   collector: SimulationCollector,
                   // actually svg
                   worldMap: String,
                   controller: RouterCtl[Pages])

  case class State(selected: Selection = None,
                   playing: Boolean = false,
                   running: Boolean = false,
                   iterations: Int = 0,
                   runningTime: Double = 0.0)

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
          (($.props |> { p ⇒ p.collector += result }) >>
            $.modState(s ⇒ s.copy(
              iterations = s.iterations + reply.rounds,
              runningTime = s.runningTime + reply.interval))).runNow()
      }
    }

    worker.onmessage = receive

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
        world = io.circe.scalajs.convertJsonToJs(JsonDecoder.encodeAsJson(w))))
    }

    val startSimulation = {
      $.modState(_.copy(playing = true)) >>
        ($.props |> { p ⇒ sendWorld(p.world) }) >>
        Callback { send(Message(WorkerOperation.Start)) }
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
      println(s"updateWorld: new=${newWorld.hashCode()}")
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
      println(s"changeWorld: new=${world.hashCode()}")

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

      <.div(
        // header contains controllers and graphs
        <.header(^.className := "row pb-3",
          // controls (play stop) are here
          <.div(^.className := "col-2",
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-primary",
              ^.onClick --> startSimulation,
              ^.disabled := s.running,
              "START").when(!s.playing),
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-primary",
              ^.onClick --> stopSimulation,
              ^.disabled := !s.running,
              "PAUSE").when(s.playing),
          ),

          // graphs come here
          <.div(^.className := "col",
            <.small(
              ^.className := "text-muted",
              s"${s.iterations} iterations ",
              f"in ${s.runningTime}%.1f seconds".when(s.runningTime > 0.0)),
            <.br,
            <.small(
              f"Loss: ${p.collector.global.loss.percentage}%.1f%%", <.br,
              f"Generation: ${p.collector.global.generation.mean}%.0f±${p.collector.global.generation.dev}%.0f MW", <.br,
              f"GHG: ${p.collector.global.ghg.mean * ghgScaleFactor}%.0f±${p.collector.global.ghg.dev * ghgScaleFactor}%.0f t/a", <.br
            ).when(p.collector.rounds > 0)),

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
            if (p.collector.rounds > 0)
              s.selected match {
                case Some(Left(id)) ⇒
                  val area = world.areaById(id).get
                  val data = p.collector.areas(id)
                  <.div(
                    ^.className := "stats",
                    AreaStats((area, data)))
                case Some(Right(id)) ⇒
                  val line = world.lineById(id).get
                  val data = p.collector.lines(id)
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
                case Some(Left(id)) ⇒
                  AreaInfo(AreaInfo.Props(world.areaById(id).get,
                    { newArea ⇒
                      changeWorld(world.update(newArea))
                    }))

                case Some(Right(id)) ⇒
                  val line = world.lineById(id).get
                  LineInfo(LineInfo.Props(line,
                    { newLine ⇒
                      println(s"old line: ${line.unitCapacity} ${line.disabled} new ${newLine.unitCapacity} ${newLine.disabled}")
                      changeWorld(world.update(newLine))
                    }))
                case None ⇒
                  "No area or line selected"
              }
            ))))
    }
  }

  val component = ScalaComponent.builder[Props]("UserInterface")
    .initialState(State())
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.uninit)
    .componentWillReceiveProps(i ⇒ i.backend.updateWorld(i.nextProps.world))
    .build

  def apply(world: World, defaultWorld: World, worldMap: String, ctl: RouterCtl[Pages]) = {
    component(Props(world = world,
      defaultWorld = defaultWorld,
      worldMap = worldMap,
      collector = SimulationCollector(world),
      controller = ctl))
  }
}
