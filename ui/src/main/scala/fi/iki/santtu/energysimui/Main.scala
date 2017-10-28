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


// @JSImport("bootstrap", JSImport.Namespace)
// @js.native
// object bootstrap extends js.Object {
// }

@JSExportTopLevel("EnergySim")
object Main {
  val data = """{
               |    "areas": {
               |        "east": {
               |            "drains": [
               |                {
               |                    "capacity": 250,
               |                    "name": "variant",
               |                    "type": "city"
               |                }
               |            ],
               |            "sources": [
               |                {
               |                    "capacity": 1000,
               |                    "name": "wind",
               |                    "type": "uniform"
               |                },
               |                {
               |                    "capacity": 1000,
               |                    "ghg": 1,
               |                    "name": "gas",
               |                    "type": "uniform"
               |                }
               |            ]
               |        },
               |        "north": {
               |            "drains": [
               |                {
               |                    "capacity": 500,
               |                    "name": "variant",
               |                    "type": "city"
               |                }
               |            ],
               |            "sources": [
               |                {
               |                    "capacity": 10000,
               |                    "ghg": 0,
               |                    "name": "hydro",
               |                    "type": "hydro"
               |                }
               |            ]
               |        },
               |        "south": {
               |            "drains": [
               |                {
               |                    "capacity": 5000,
               |                    "name": "general",
               |                    "type": "uniform"
               |                },
               |                {
               |                    "capacity": 5000,
               |                    "name": "variant",
               |                    "type": "city"
               |                }
               |            ],
               |            "sources": [
               |                {
               |                    "capacity": 1000,
               |                    "ghg": 10,
               |                    "name": "coal",
               |                    "type": "uniform"
               |                }
               |            ]
               |        }
               |    },
               |    "lines": [
               |        {
               |            "areas": [
               |                "north",
               |                "south"
               |            ],
               |            "capacity": 5000
               |        },
               |        {
               |            "areas": [
               |                "south",
               |                "east"
               |            ],
               |            "capacity": 2000
               |        },
               |        {
               |            "areas": [
               |                "north",
               |                "east"
               |            ],
               |            "capacity": 500
               |        }
               |    ],
               |    "name": "simple model",
               |    "types": {
               |        "city": {
               |            "data": [
               |                3,
               |                5
               |            ],
               |            "model": "beta",
               |            "size": 0
               |        },
               |        "hydro": {
               |            "data": [
               |                [
               |                    0.1,
               |                    0,
               |                    0
               |                ],
               |                [
               |                    0.9,
               |                    1,
               |                    1
               |                ]
               |            ],
               |            "model": "step",
               |            "size": 25
               |        }
               |    }
               |}""".stripMargin.getBytes("UTF-8")

  @JSExport("defaultWorld")
  val defaultWorld = Model.from(data, JsonDecoder)

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

  case class State(world: World, playing: Boolean = false) {
  }

  object State {
    val default = State(defaultWorld)
  }

  class Interface($: BackendScope[Props, State]) {
    val start = {
      $.modState(_.copy(playing = true)) >>
      Callback.log ("START CLICKED!")
    }
    val stop = {
      $.modState(_.copy(playing = false)) >>
      Callback.log("STOP CLICKED!")
    }

    def source(s: Source)(implicit a: Area) =
      <.div(
        ^.className := "source",
        s.name, <.br,
        s.unitCapacity.toString, " MW")

    def remove(a: Area, d: Drain) = {
//      val before = $.state |> { s ⇒ println(s"drains before: ${s.world.areas.flatMap(_.drains)}") })
//      val modify = $.modState(s ⇒ s.copy(world = s.world.remove(a, d)))
//      val log = Callback.log(s"removed $d from $a")
//      val after = ($.state |> { s ⇒ println(s"drains after: ${s.world.areas.flatMap(_.drains)}") })
//      val chain = before >> modify >> log >> after
//
//      $.modState {
//        s ⇒
//          println(s"modifying state s=$s, drains=${s.world.areas.flatMap(_.drains)}")
//          val ns = s.copy(world = s.world.remove(a, d))
//          println(s"modified state ns=$ns drains=${ns.world.areas.flatMap(_.drains)}")
//          ns
//      }
//
//      CallbackTo {
//        println("in callback 1")
//        Callback {
//          println("in callback 2")
//        }
//      }
//
      $.props |> (_.ctl) >>= {
        ctl ⇒
          $.state |> (_.world) >>= {
            w ⇒
              CallbackTo {
                (ctl, w.remove(a, d))
              }
          }
      } >>= {
        case (ctl, w) ⇒
          $.modState {
            s ⇒
              println(s"XXX ctl=$ctl w=$w s=$s")
              s.copy(world = w)
          } >> ctl.set(WithWorld(w))
      }
//
//          $.modState {
//          s ⇒
//            println(s"CTL=$ctl")
//            println(s"modifying state s=$s, drains=${s.world.areas.flatMap(_.drains)}")
//            val ns = s.copy(world = s.world.remove(a, d))
//            println(s"modified state ns=$ns drains=${ns.world.areas.flatMap(_.drains)}")
//            ctl.set(WithWorld(ns.world))
//            ns
//        }
//      }

//      chain
    }
    //    >> {
//        for {
//          s ← $.state
//          p ← $.props
//        } yield (s, p)
//      }.flatMap { case (s, p) ⇒
//        val ww = WithWorld(s.world)
//        p.ctl.set(ww) >>
//          Callback.log(s">>= s=$s p=$s ww=$ww") >>
//          Callback.log(s"-- drains: ${s.world.areas.flatMap(_.drains)}")
//      }

//          $.state |> (_.world) >>= {
//            w ⇒
//              Callback.log(s">>= w=$w drains=${w.areas.flatMap(_.drains)}")
//          }
//        }
//        ($.state >> _.world
//          |> {
//          w ⇒
//            $.props |> _.ctl >>= {
//              ctl ⇒
//                println(s">>= w=$w ctl=$ctl")
//                ???
//            }
//        })
//    $.props |> _.ctl >>= { x ⇒ Callback.log(s"x=$x")}
//        $.state.map(s ⇒ {
//          val ww = WithWorld(s.world)
//          println(s"state map, s=$s ww=$ww")
//          $.props.map(_.ctl).
//        })

//    $.state(s ⇒
//      ctl.set(WithWorld($.state.))
//      None)

    def drain(d: Drain)(implicit a: Area) =
      <.div(
        ^.className := "drain",
        d.name, <.br,
        d.unitCapacity.toString, " MW", <.br,
        <.button(
          ^.`type` := "button",
          ^.className := "btn btn-danger",
          ^.onClick --> remove(a, d).void,
          <.i(^.className := "fa fa-trash-o")))

    def area(implicit a: Area) =
      <.div(^.className := "area row",
        <.h2(^.className := "col",
          s"Area: ${a.name}"),
        <.div(
          ^.className := "col",
          <.div(^.className := "sources row no-gutters",
            <.h4("Sources"),
            a.sources.toVdomArray(s ⇒
              <.div(^.className := "col",
                ^.key := s.name,
                source(s))))),
        <.div(
          ^.className := "col",
          <.div(^.className := "drains row no-gutters",
            <.h4(^.className := "col",
              "Drains"),
            a.drains.toVdomArray(d ⇒
            <.div(^.className := "col",
              ^.key := d.name,
              drain(d))))))

    def line(l: Line) =
      <.div(^.className := "line",
        <.h2(s"Line: ${l.name}"), <.br,
        l.unitCapacity.toString, " MW", <.br,
        l.area1.name, " ⇌ ", l.area2.name)

    def render(p: Props, s: State): VdomElement = {
      println(s"render: p=$p s=$s")
      <.div(
        // header contains controllers and graphs
        <.header(^.className := "row",
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
            s"World: ${s.world.name}",
            <.br,
            s"Units: ${s.world.units.length}", <.br,
            <.button(
              ^.`type` := "button",
              ^.className := "btn btn-warning",
              ^.onClick --> p.ctl.set(defaultPage),
              ^.disabled := (s.playing || s.world == defaultWorld),
              "RESET"))),

        <.div(
          // next comes actual world information
          <.div(^.className := "row",
            s.world.areas.toVdomArray(a ⇒
              <.div(
                ^.key := a.name,
                ^.className := "col-lg",
                area(a))),
            s.world.lines.toVdomArray(l ⇒
              <.div(
                ^.key := l.name,
                ^.className := "col-lg",
                line(l))))),

        // followed by the terminating footer
        <.footer(^.className := "row",
          "FOOTER HERE"))
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

//      val homeRoute =
//        staticRoute("", Home) ~> {
//          println("HOME ROUTE RENDER")
//          render(<.h1("Home"))
//        }

      val worldRoute =
        dynamicRouteCT("#" / string(".+").caseClass[WithWorld]) ~>
          dynRenderR((w, ctl) => {
            val ui = UserInterfaceComponent(w.world, ctl)
            println(s"worldroute: w=$w ctl=$ctl ui=$ui")
            ui
//
//            <.span()
            //            UserInterfaceComponent()
            //              .setSt (State(WithWorld.from(w)))
            //          }
            //            <.div(
            //              <.h1(s"$w"),
            //              <.h2("buttons"),
            //              wws.toVdomArray(
            //                w => <.button(^.key := w.encoded,
            //                  ctl.setOnClick(w),
            //                  w.encoded)),
            //              <.h2("links"),
            //              wws.toVdomArray(
            //                w ⇒ <.span(^.key := w.encoded, ctl.link(w)(w.encoded), <.br)))
            //          )
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

//    println(s"defaultPage=$defaultPage")
//    println(s"router=$router")
    router().renderIntoDOM(dom.document.getElementById("playground"))
//
//    val component = ScalaComponent.builder[Unit]("Interface")
//      .initialState(State.default)
//      .renderBackend[Interface]
//      .build
//
//    val encoded = JsonDecoder.encode(world)
//    println(s"world encoded=$encoded")

//    component().renderIntoDOM(
//      dom.document.getElementById("playground"))


    // println(s"bootstrap: $bootstrap")
    println(s"World: ${defaultWorld}")
    println(s"units: ${defaultWorld.units}")

//    run(1000)
  }
}
