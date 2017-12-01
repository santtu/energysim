package fi.iki.santtu.energysimui

import fi.iki.santtu.energysim.model.World
import fi.iki.santtu.energysim.{JsonDecoder, SimulationCollector}
import fi.iki.santtu.energysimui.Main._
import fi.iki.santtu.energysimworker.WorkerState.{Result, Started, Stopped}
import fi.iki.santtu.energysimworker.{Message, Reply, WorkerOperation, WorkerState}
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.{Attr, VdomElement}
import org.scalajs.dom.raw.MessageEvent
import japgolly.scalajs.react.{BackendScope, Callback, CallbackTo, ScalaComponent}
import org.scalajs.dom.raw.Worker
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js
import scala.util.Random



object UserInterface {
  /**
    * How many rounds are run for each user interface update.
    */
  val roundsPerUpdate = 1
  /**
    * Length of value history retained for use in graphs etc.
    */
  val historySize = 100
  /**
    * Maximum number of rounds per run until simulation is automatically
    * stopped to prevent it from consuming resources forever.
    */
  val maxRounds = 25000

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
          (($.state >>= { s ⇒
            // 2* is because we'll get one **more** update after sending stop
            if (s.playing &&
              (s.iterations + 2 * result.rounds.length) >= maxRounds)
              stopSimulation
            else
              Callback.empty
          }) >> $.modState(s ⇒ {
            s.collector.get += result
            s.copy(
              iterations = s.iterations + reply.rounds,
              runningTime = s.runningTime + reply.interval)
          })).runNow()
      }
    }

    worker.onmessage = receive

    def resetCollector(world: World): Callback =
      $.modState(s ⇒ {
        val collector = SimulationCollector(world, historySize)
        Main.collector = collector // BAD BAD
        s.copy(collector = Some(collector))
      })

    def init: Callback =
      $.props >>= { p ⇒ resetCollector(p.world) }

    def uninit: Callback =
      Callback {
        worker.terminate()
      }

    def send(m: Message): Unit = {
      worker.postMessage(m)
      println(s"Sent worker message: ${WorkerOperation(m.op)}")
    }

    def sendWorld(w: World): Unit = {
      send(Message(WorkerOperation.SetWorld,
        value = io.circe.scalajs.convertJsonToJs(JsonDecoder.encodeAsJson(w))))
    }

    val startSimulation: Callback = {
      $.modState(_.copy(playing = true)) >>
        ($.props |> { p ⇒ sendWorld(p.world) }) >>
        Callback { send(Message(WorkerOperation.Start, roundsPerUpdate)) }
    }

    val stopSimulation: Callback = {
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
      Main.world = newWorld  // BAD BAD
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
    def changeWorld(world: World, reset: Boolean = false): Callback = {
//      println(s"changeWorld: new=${world.hashCode()}")

//      val p = WorldPage(world)
//      val p2 = WorldPage(p.world)
//      assert(p == p2)   // json serialization is not stable, strings can differ
//      assert(world == p.world, s"world and page world differ:\n$world\n${p.world}")
//      assert(world == p2.world, "re-paged world and world differ")

      (if (reset) resetCollector(world) else Callback.empty) >>
        ($.props >>= {
          case props if props.world != world ⇒ props.controller.set(WorldPage(world))
          case _ ⇒ Callback.empty
        })
    }

    def scroll(id: String) = Callback {
      val el = org.scalajs.dom.document.getElementById(id)
      Scroll.scrollIntoViewIfNeeded(el,
        js.Dictionary("duration" → 1000, "easing" → "ease"))
    }

    def render(s: State, p: Props): VdomElement = {
      val world = p.world
      val collector: SimulationCollector = s.collector.get

      <.div(
        <.div(^.className := "sticky-top btn-group btn-group-lg d-block d-sm-block d-md-none",
          ^.role := "group",
          ^.id := "nav",
          <.button(^.`type` := "button",
            ^.className := "btn btn-secondary",
            ^.onClick --> scroll("map"),
            "map"),
          <.button(^.`type` := "button",
            ^.className := "btn btn-secondary",
            ^.onClick --> scroll("info"),
            "info"),
          <.button(^.`type` := "button",
            ^.className := "btn btn-secondary",
            ^.onClick --> scroll("stats"),
            "stats")
        ),
        
        // header contains controllers and graphs
        <.header(^.className := "row pb-3",
          // controls (play stop) are here
          <.div(^.className := "col-2",
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-primary start-stop",
              ^.onClick --> startSimulation,
              ^.disabled := s.running,
              tooltip := "Start simulation",
              "START"
            ).when(!s.playing),
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-primary start-stop",
              ^.onClick --> stopSimulation,
              ^.disabled := !s.running,
              tooltip := s"Stop the simulation (it will stop after $maxRounds iterations automatically)",
              "STOP").when(s.playing)),

          // graphs come here
          <.div(^.className := "col",
            <.div(
              <.small(
                ^.className := "text-muted",
                s"${s.iterations} iterations ",
                f"in ${s.runningTime}%.1f seconds"),
            ).when(s.iterations > 0),
            <.img(^.className := "starting",
              ^.src := "images/loading.svg").when(s.iterations == 0 && s.playing),

          ),

          // summary information
          <.div(^.className := "col-5 text-right",
            <.i(^.className := "fa fa-share-alt px-3",
              ^.id := "share",
              tooltip := "You can copy the page address directly, as it includes all of the model parameters (you can also bookmark it)."),
            " ",
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-warning",
              ^.onClick --> changeWorld(p.world, reset = true),
              ^.disabled := (s.iterations == 0),
              tooltip := "Clear statistics, but do not change changes to model",
              "CLEAR"),
            " ",
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-danger",
              //|              ^.onClick --> p.ctl.set(defaultPage),
              ^.onClick --> changeWorld(p.defaultWorld, reset = true),
              ^.disabled := (s.playing || world == p.defaultWorld),
              tooltip := "Resets all changes to the model",
              "RESET"))),

        <.div(^.className := "row",
          // map column
          <.div(^.className := "col col-sm-8 col-md-6 col-lg-5",
            ^.id := "map",
            WorldMap(p.worldMap,
              collector.areas,
              collector.lines.map {
                case (id, data) ⇒
                  id → (p.world.lineById(id).get.disabled, data)
              },
              selected ⇒ $.modState(s ⇒ s.copy(selected = selected)))),


          // info column
          <.div(
            ^.className := "col-sm-4 col-md-3 col-lg-3 main-right info",
            ^.id := "info",
            // name
            <.div(^.className := "description",
              s.selected match {
                case AreaSelection(id) ⇒
                  <.span(
//                    ^.id := s"$id-area-name",
//                    ^.key := id,
                    areas(id).name,
                    tooltip := areas(id).description)

                case LineSelection(id) ⇒
                  lines(id).name

                case NoSelection ⇒
                  "Global"
              }),

            // Information on global, area or line:
            <.div(^.className := "info",
              s.selected match {
                case AreaSelection(id) ⇒
                  AreaInfo(AreaInfo.Props(world.areaById(id).get,
                    { newArea ⇒ changeWorld(world.update(newArea)) }))
                case LineSelection(id) ⇒
                  val line = world.lineById(id).get
                  LineInfo(LineInfo.Props(line,
                    { newLine ⇒ changeWorld(world.update(newLine)) }))
                case NoSelection ⇒
                  // show global statistics here
                  GlobalInfo(world, changeWorld(_))
              }
            )),

          // statistics column
          <.div(^.className := "col-md-3 col-lg-4 main-right",
            ^.id := "stats",

            // Global, area or line statistics:

            // if rounds > 0 we have statistics to show, either
            // running or paused
            if (collector.rounds > 0)
              s.selected match {
                case AreaSelection(id) ⇒
                  val area = world.areaById(id).get
                  val data = collector.areas(id)
                  AreaStats(area, data)

                case LineSelection(id) ⇒
                  val line = world.lineById(id).get
                  val data = collector.lines(id)
                  LineStats((line, data))

                case NoSelection ⇒
                  GlobalStats(collector.global, collector.external)
              }
            else
//              EmptyVdom
              "No statistics to show. Click on \"START\" on the top-left corner to start the simulation."
          )))
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
