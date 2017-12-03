package fi.iki.santtu.energysimui

import com.payalabs.scalajs.react.bridge.{ReactBridgeComponent, WithProps}
import fi.iki.santtu.energysim.model.{Area, Line, Source, World}
import fi.iki.santtu.energysim.{AreaStatistics, LineStatistics, Portion, SimulationCollector}
import fi.iki.santtu.energysimui.Main.{AreaData, LineData}
import japgolly.scalajs.react.extra.components.TriStateCheckbox
import japgolly.scalajs.react.vdom.Attr
import japgolly.scalajs.react.vdom.Attr.ValueType
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, _}
import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.raw._

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.{JSGlobal, JSGlobalScope}
import scala.util.Try

object utils {
  def tri(b: Seq[Boolean]): Option[Boolean] =
    (b.contains(true), b.contains(false)) match {
      case (true, false) ⇒ Some(true)
      case (false, true) ⇒ Some(false)
      case _ ⇒ None
    }

  def powerSecurity(loss: Portion): VdomTag =
    <.div(
      tooltip := "The time all demand can be met and no power outages occur. Anything less than 100% means there are power outages.",
      "Power security", <.br,
      <.span(^.className := "power-security",
        f"${100.0 - loss.percentage}%.0f%%"))

  def dualGraph(data: Seq[Double]) =
    Sparklines(data = data)(
      SparklinesLine(style = Map("fill" → "none".asInstanceOf[js.Any]))(),
      SparklinesSpots()(),
      SparklinesReferenceLine(`type` = "custom", value = 0.0)())

  def singleGraph(data: Seq[Double], mean: Double) =
    Sparklines(data = data, min = 0.0)(
      SparklinesLine(color = "#253e56")(),
      SparklinesSpots()(),
      SparklinesReferenceLine(`type` = "custom", value = mean)())

  /**
    * Given the scale and check parameter, update given list of sources
    * with the capacity scaled by the value to current capacity and enabled by !checked.

    * @param sources
    * @param value
    * @param checked
    * @return
    */

  def updateSources(sources: Seq[Source], value: Double, checked: Option[Boolean]): Seq[Source] = {
    val capacity = sources.map(_.unitCapacity).sum
    sources map {
      case s if capacity == 0 ⇒
        s.copy(capacity = (value / sources.length).toInt)
      case s if value == capacity ⇒ s
      case s ⇒
        val scale = value / capacity
        s.copy(capacity = (scale * s.unitCapacity).toInt)
    } map {
      case s if checked.isEmpty ⇒ s
      case s ⇒ s.copy(disabled = !checked.get)
    }
  }

  /**
    * Generates an editor for the given sources, adjusted based on their
    * model type (this is a kludge, we should have a separate fuel or other
    * grouping attribute to do this...). This takes into account the fact
    * that multiple sources can have the same type.
    *
    * @param sources
    * @return
    */
  def sourceTypeEditor(sources: Seq[Source], update: Seq[Source] ⇒ Callback) = {
    // group all sources by their capacity type
    val byType = sources.groupBy(_.capacityType.id)

//    def updated(target: Seq[Source])(scale: Double, enabled: Option[Boolean]): Callback =
//      (Callback.log(s"target=$target scale=$scale enabled=$enabled") >>
//        CallbackTo(update(target))).flatten

    // Show them by sorted names
    Main.types.toSeq.sortBy(_._2.name).toVdomArray {
      case (id, info) ⇒
        val typeSources = byType.getOrElse(id, Seq.empty)
        val capacity = typeSources.map(_.unitCapacity).sum
        val enabled = tri(typeSources.map(!_.disabled))

        <.div(
          ^.className := "capacity",
          ^.key := id,
          ^.visibility.hidden.when(typeSources.isEmpty),
          ^.visibility.visible.when(typeSources.nonEmpty),

          EnabledNumber(
            label = info.name,
            value = capacity,
            checked = enabled,
            callback = {
              case (value, checked) if value != capacity || enabled != checked ⇒
                update(updateSources(typeSources, value, checked))
              case _ ⇒ Callback.empty
            },
            min = 0, max = 10000, step = 10)
        )
    }
  }
}

@js.native
@JSGlobal("scrollIntoViewIfNeeded")
object Scroll extends js.Object {
  def scrollIntoViewIfNeeded(element: Element,
                             parameters: js.UndefOr[js.Dictionary[Any]] = js.undefined): Unit = js.native
}

import fi.iki.santtu.energysimui.utils._


object tooltip extends Attr[String]("tooltip") {
  private val dataToggle = VdomAttr("data-toggle")
  // the use of data-original-title is not per popper.js spec, but
  // apparently due to bad interaction between selectors and react
  // that seems the only way to consistently get the correct tooltip
  // shown (when the same element is re-used by react, for example)
  private val dataOriginalTitle = VdomAttr("data-original-title")
  override def :=[A](text: A)(implicit t: Attr.ValueType[A, String]): TagMod =
    EmptyVdom(
      dataToggle := "tooltip",
      dataOriginalTitle := text,
    )
}

/**
  * Generic number input with a "enabled" checkbox. This puts off
  * updates on the value until user has finished the input.
  */

object EnabledNumber {
  case class State(checked: Option[Boolean], value: Double, inInput: Boolean)
  case class Props(checked: Option[Boolean], value: Double, label: TagMod,
                   callback: (Double, Option[Boolean]) ⇒ Callback,
                   min: Double, max: Double, step: Double, tooltip: Option[String])

  class Backend($: BackendScope[Props, State]) {
    def render(p: Props, s: State): VdomElement = {
      def update(state: State) =
        ($.setState(state) >>
          CallbackTo {
            // send new state only if not in input and values have changed
            if (!state.inInput &&
              (p.value != state.value ||
                p.checked != state.checked))
              p.callback(state.value, state.checked)
            else
              Callback.empty
          }).flatten

      val state = s.checked match {
        case Some(true) ⇒ TriStateCheckbox.Checked
        case Some(false) ⇒ TriStateCheckbox.Unchecked
        case None ⇒ TriStateCheckbox.Indeterminate
      }

      val nextChecked = state.nextDeterminate match {
        case TriStateCheckbox.Checked ⇒ true
        case TriStateCheckbox.Unchecked ⇒ false
      }

      <.div(
        <.div(
          ^.className := "form-row",
          <.div(
            ^.className := "col",
            <.div(^.className := "form-check-input",
              TriStateCheckbox.Component(TriStateCheckbox.Props(
                state,
                update(s.copy(checked = Some(nextChecked)))
              ))),
            tooltip :=? p.tooltip,
            p.label)),
        <.div(
          ^.className := "form-row",
          <.div(
            ^.className := "col-10",
            <.input(
              ^.`type` := "range",
              ^.className := "form-control",
              ^.value := s.value.toString,
              ^.min := p.min,
              ^.max := p.max,
              ^.step := p.step,
              ^.onChange ==> { e: ReactEventFromInput ⇒
                Try(e.target.value.toDouble).toOption match {
                  case Some(value) if value != s.value ⇒ update(s.copy(value = value))
                  case None ⇒ Callback.empty
                }
              },
              ^.onMouseDown --> update(s.copy(inInput = true)),
              ^.onMouseUp --> update(s.copy(inInput = false)),
              ^.onTouchStart --> update(s.copy(inInput = true)),
              ^.onTouchEnd --> update(s.copy(inInput = false)),
              ^.onTouchCancel --> update(s.copy(value = p.value, inInput = false)))),
          <.div(
            ^.className := "col-2",
            s.value.toString)))
    }

    def receiveProps(props: Props): Callback =
      $.modState {
        // use modstate and be careful if we're in input -- do not change
        // current state values if input is in process
        // while we receive new props, keep the current value (slider)
        case s if s.inInput ⇒ s
        case s ⇒ s.copy(checked = props.checked, value = props.value)
      }
  }

  private val component = ScalaComponent.builder[Props]("EnabledNumber")
    .initialState(State(Some(true), 0, false))
    .renderBackend[Backend]
    .componentWillMount(i ⇒ i.backend.receiveProps(i.props))
    .componentWillReceiveProps(i ⇒ i.backend.receiveProps(i.nextProps))
    .build

  def apply(label: TagMod = "",
            value: Double = 0,
            checked: Option[Boolean] = Some(true),
            callback: (Double, Option[Boolean]) ⇒ Callback,
            min: Double, max: Double, step: Double = 1,
            tooltip: String = "") =
    component(Props(checked, value, label, callback, min, max, step,
      if (tooltip.length > 0) Some(tooltip) else None))
}


/*
 * Various sub-components for displaying different portions of the
 * user interface:
 *
 * - AreasMap: displaying the "map" portion
 * - AreaInfo: static information about an area
 * - AreaStats: statistics view of an area
 */

object AreasMap {
  case class Props(world: World,
                   areaData: Map[Area, AreaData],
                   lineData: Map[Line, LineData],
                   selected: Selection,
                   updateSelection: (Selection) ⇒ Callback)

  class Backend($: BackendScope[Props, Unit]) {
    // is current selection related to this area? only if line is
    // selected can be
    def isRelated(p: Props, area: Area): Boolean =
      p.selected match {
        case LineSelection(id) ⇒ p.world.lineById(id).get.areasSeq.contains(area)
        case _ ⇒ false
      }

    def isRelated(p: Props, line: Line): Boolean =
      p.selected match {
        case AreaSelection(id) ⇒ line.areasSeq.map(_.id).contains(id)
        case _ ⇒ false
      }

    def render(p: Props) = {
      <.div(
        p.areaData.toVdomArray { case (area, info) ⇒
          val focused = p.selected == AreaSelection(area.id)
          <.div(
            ^.key := area.id,
            ^.className := "col area",
            (^.className := "focused").when(focused),
            (^.className := "related").when(isRelated(p, area)),
            (^.onClick --> p.updateSelection(AreaSelection(area.id))).when(!focused),
            (^.onClick --> p.updateSelection(NoSelection)).when(focused),
            info.name)
        },
        p.lineData.toVdomArray { case (line, info) ⇒
          val focused = p.selected == LineSelection(line.id)
          <.div(
            ^.key := line.id,
            ^.className := "col line",
            (^.className := "focused").when(focused),
            (^.className := "related").when(isRelated(p, line)),
            (^.onClick --> p.updateSelection(LineSelection(line.id))).when(!focused),
            (^.onClick --> p.updateSelection(NoSelection)).when(focused),
            info.name)
        })
    }
  }
  private val component = ScalaComponent.builder[Props]("AreasMap")
    .renderBackend[Backend]
    .build

  def apply(p: Props) = component(p)
}

object AreaInfo {
  case class Props(area: Area, areaUpdated: (Area) ⇒ Callback)
  type State = Map[String, (Int, Boolean)]

  class Backend($: BackendScope[Props, State]) {
    def render(state: State, p: Props): VdomElement = {
      val area = p.area
      val info = Main.areas(area.id)

      val (productionCapacity, productionEnabled) = (
        area.sources.map(_.unitCapacity).sum,
        tri(area.sources.map(!_.disabled)))

      <.div(
        ^.className := "capacity",
        EnabledNumber(
          <.b("Production capacity"),
          productionCapacity,
          productionEnabled,
          { (v, c) ⇒
            var a = area

            // if checked is set or unset, set all lines matching to that
            // (if indeterminate, leave as is)
            c match {
              case Some(enabled) ⇒
                a = a.setSourcesDisabled(!enabled)

              case None ⇒
            }

            if (v != productionCapacity) {
              val scale = v.toDouble / productionCapacity
              a = a.scaleSourceCapacity(scale)
            }

            if (a != area)
              p.areaUpdated(a)
            else
              Callback.empty
          },
          tooltip = s"The maximum production capacity in ${info.name}. Use the slider to adjust all production methods proportionally, and the checkbox to enable/disable all production in ${info.name} in one step.",
          min = 0, max = 10000, step = 100),

        utils.sourceTypeEditor(
          area.sources,
          ss ⇒ p.areaUpdated(area.updateSources(ss))))
    }

    def receiveProps(props: Props): Callback =
      $.setState(props.area.sources.map(s ⇒ s.id → (s.unitCapacity, s.disabled)).toMap)
  }

  private val component = ScalaComponent.builder[Props]("AreaInfo")
    .initialState(Map.empty[String, (Int, Boolean)])
    .renderBackend[Backend]
    .componentWillMount(i ⇒ i.backend.receiveProps(i.props))
    .componentWillReceiveProps(i ⇒ i.backend.receiveProps(i.nextProps))
    .build

  def apply(p: Props) = component.withKey(p.area.id)(p)
}

object LineInfo {
  case class Props(line: Line, lineUpdated: (Line) ⇒ Callback)
  type State = (Int, Boolean)

  class Backend($: BackendScope[Props, State]) {
    def render(p: Props): VdomElement = {
      val info = Main.lines(p.line.id)

      <.div(
        EnabledNumber(
          "Transmission capacity",
          p.line.unitCapacity,
          Some(!p.line.disabled),
          { (v, c) ⇒
            p.lineUpdated(p.line.copy(disabled = !(c.get), capacity = v.toInt)) },
          min = 0, max = 10000, step = 100))
    }

    def receiveProps(props: Props): Callback =
      $.setState((props.line.unitCapacity, props.line.disabled))
  }

  private val component = ScalaComponent.builder[Props]("LineInfo")
    .initialState((0, false))
    .renderBackend[Backend]
    .componentWillMount(i ⇒ i.backend.receiveProps(i.props))
    .componentWillReceiveProps(i ⇒ i.backend.receiveProps(i.nextProps))
    .build

  def apply(p: Props) = component.withKey(p.line.id)(p)
}


object GlobalInfo {
  case class Props(world: World,
                   worldUpdated: (World) ⇒ Callback)


  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props): VdomElement = {
      val sources = p.world.areas.filter(!_.external).flatMap(_.sources)
      val lines = p.world.lines.filter(l ⇒ l.area1.external || l.area2.external)
      val world = p.world

      def update(w: World) =
        if (w != world)
          p.worldUpdated(w)
        else
          Callback.empty

      val (productionCapacity, productionEnabled) = (
        sources.map(_.unitCapacity).sum,
        tri(sources.map(!_.disabled)))

      val (importCapacity, importEnabled) = (
        lines.map(_.unitCapacity).sum,
        tri(lines.map(!_.disabled)))

//      println(s"prod=${sources.map(!_.disabled)} -> $productionEnabled")
//      println(s"import=${lines.map(!_.disabled)} -> $importEnabled")

      <.div(^.className := "capacity",
        EnabledNumber(
          <.b("Production capacity"),
          productionCapacity,
          productionEnabled,
          { (v, c) ⇒
            var w = world

            // see if need to update enabled/disabled on all sources
            c match {
              case Some(enabled) ⇒
                w = w.setSourcesDisabled(!enabled)
              case None ⇒
            }

            // see if we need to scale capacities up or down
            if (v != productionCapacity) {
              val scale = v.toDouble / productionCapacity
              w = w.scaleSourceCapacity(scale)
            }

            update(w)
          },
          tooltip = "Total maximum production capacity in Finland, use slider to change capacities in all areas proportionally.",
          min = 5000, max = 100000, step = 100),

        utils.sourceTypeEditor(
          world.areas.filter(!_.external).flatMap(_.sources),
          ss ⇒ update(world.updateSources(ss))),

        <.hr,
        <.div(^.className := "capacity",
          EnabledNumber(
            <.b("Import capacity"),
            importCapacity,
            importEnabled,
            { (v, c) ⇒
              var w = world

              // if checked is set or unset, set all lines matching to that
              // (if indeterminate, leave as is)
              c match {
                case Some(enabled) ⇒
                  w = w.copy(
                    lines = world.lines.map {
                      case l if l.area1.external || l.area2.external ⇒
                        l.copy(disabled = !enabled)
                      case l ⇒ l
                    })
                case None ⇒
              }

              // see if we need to scale capacities up or down
              if (v != importCapacity) {
                w = w.copy(lines = w.lines.map {
                  case l if importCapacity == 0 ⇒ l.copy(capacity = (v / w.lines.length).toInt)
                  case l ⇒ l.copy(capacity = ((v / importCapacity) * l.unitCapacity).toInt)
                })
              }

              update(w)
            },
            tooltip = "Total imports from neighbouring countries. Use the slider to adjust all imports proportionally and the checkbox to enable or disable all electricity imports in one step.",
            min = 0, max = 10000, step = 100)),
      )
    }
  }

  private val component = ScalaComponent.builder[Props]("GlobalInfo")
    .renderBackend[Backend]
    .build

  def apply(world: World, worldUpdated: (World) ⇒ Callback) =
    component(Props(world, worldUpdated))
}

object AreaStats {
  type Props = (Area, AreaStatistics)

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props) = {
      val (area, data) = p

      <.div(
        ^.className := "statistics",
        powerSecurity(data.loss),

        <.div(
          tooltip := s"The power balance for ${area.name}, with values >0 showing unused or untransferred electricity production and <0 insufficient production and imports to meet demand.",
          "Power balance", <.br,
          <.span(^.className := "current",
            f"${data.total / 1e3}%.1s GW",
          ),
          dualGraph(data.total.toSeq)),

        "Transfer",  <.br,
        <.span(^.className := "current",
          f"${data.transfer / 1000}%.1s GW"),
        dualGraph(data.transfer.toSeq),

        "Production",  <.br,
        <.span(^.className := "current",
          f"${data.generation / 1000}%.1s GW"),
        singleGraph(data.generation.toSeq, data.generation.mean),

        "CO2 emissions",  <.br,
        <.span(^.className := "current",
          f"${data.ghg * Main.ghgScaleFactor / 1e6}%.1s Mt/a"),
        singleGraph(data.ghg.toSeq, data.ghg.mean),
      )
    }
  }

  private val component = ScalaComponent.builder[Props]("AreaStats")
    .renderBackend[Backend]
    .build

  def apply(p: Props) = component.withKey(p._1.id)(p)
}

object LineStats {
  type Props = (Line, LineStatistics)

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props) = {
      val (line, data) = p
      <.div(
        ^.className := "statistics",

        "Usage", <.br,
        <.span(^.className := "power-security",
          f"${data.usage.mean * 100.0}%.1f%%"), <.br,

//        "Capacity", <.br,
//        <.span(^.className := "current",
//          f"${data.capacity / 1000}%.1s GW"), <.br,
////        singleGraph(data.capacity.toSeq, data.capacity.mean),

        // "transfer" is absolute value, but we want to show +- as
        // the direction
        "Transfer", <.br,
        <.span(^.className := "current",
          f"${data.used / 1000}%.1s GW"),
        dualGraph(data.used.toSeq),
      )
    }
  }

  private val component = ScalaComponent.builder[Props]("LineStats")
    .renderBackend[Backend]
    .build

  def apply(p: Props) = component.withKey(p._1.id)(p)
}


object GlobalStats {
  class Backend($: BackendScope[SimulationCollector, Unit]) {
    def render(collector: SimulationCollector) = {
      val global = collector.global
      val external = collector.external

      <.div(
        ^.className := "statistics",
//        "Power security", <.br,
//        <.span(^.className := "power-security",
//          f"${100.0 - global.loss.percentage}%.0f%%"), <.br,

        powerSecurity(global.loss),

        "Power balance", <.br,
        <.span(^.className := "current",
          f"${global.total / 1e3}%.1s GW"),
        dualGraph(global.total.toSeq),

        "Import", <.br,
        <.span(^.className := "current",
          f"${external.transfer / -1e3}%.1s GW"),
        singleGraph((external.transfer * -1).toSeq, -external.transfer.mean),

        "Production", <.br,
        <.span(^.className := "current",
          f"${global.generation / 1e3}%.1s GW"),
        singleGraph(global.generation.toSeq, global.generation.mean),

        "CO2 emissions",  <.br,
        <.span(^.className := "current",
          f"${global.ghg * Main.ghgScaleFactor / 1e6}%.1s Mt/a"),
        singleGraph(global.ghg.toSeq, global.ghg.mean),

        {
          // calculate proportions of CO2 production by different types
          val total = global.ghg
          val types = Main.types.keys.map(id ⇒ id → collector.types(id).ghg)

//          println(s"total=$total original=${collector.types} types=$types")

          // the left margin of 100% where to put stuff to
          var left = 0.0

          <.div(^.className := "graph-container",
            types.toSeq.sortBy(_._2.mean).reverse.toVdomArray {
              case (id, ghg) ⇒
                val info = Main.types(id)
                val p = ghg / total.mean
                val el = <.div(
                  ^.className := "graph-line",
                  ^.key := id,
                  tooltip := f"${info.name}%s emissions ${Main.ghgToAnnual(ghg) / 1e6}%.0s Mt/a, ${p * 100.0}%.1s%% out of all emissions",
                  <.span(^.className := "graph-name", info.name),
                  <.div(^.className := "graph-bar",
                    {
                      import japgolly.scalajs.react.vdom.svg_<^.{<, ^}

                      // base path is always a square unit size unfilled box
                      val path = <.path(^.d := "M 0,0 l 1,0 l 0,1 l -1,0 z")

                      // all styling, fills etc. occur in CSS, not here --
                      // this provides an unit-sized box with vertical
                      // bars stacked (to unit size)

                      // kldugekldkalgkeg
                      object ClassName extends Attr[String]("class") {
                        override def :=[A](a: A)(implicit t: ValueType[A, String]): TagMod =
                          TagMod.fn(b => t.fn(b.addClassName, a))
                      }
                      val cls = ClassName
                      // cls = ^.`class`  or ^.className do not work, see
                      // https://github.com/japgolly/scalajs-react/issues/287
                      // (the suggest fix doesn't work)
                      <.svg(
                        ^.viewBox := "0 0 1 1",
                        ^.preserveAspectRatio := "none",
                        //                      ^.width := 100,
                        //                      ^.height := 20,
                        //                      ^.transform := "scale(100 20)",

                        <.g(
                          cls := "graph-stack",
                          //                        ^.transform := "scale(100, 20)",

                          // translate all right to the left point
                          <.g(^.transform := s"translate($left, 0)",

                            // the left deviation box
                            <.g(cls := "graph-deviation",
                              ^.transform := s"scale(${p.dev}, 1)", path),

                            // main body
                            <.g(cls := "graph-mean",
                              ^.transform := s"translate(${p.dev}, 0) scale(${p.mean}, 1)", path),

                            // right deviation
                            <.g(cls := "graph-deviation",
                              ^.transform := s"translate(${p.dev + p.mean}, 0) scale(${p.dev}, 1)", path)
                          )))
                    }))

                left += p.mean
                el
            })
        }
      )
    }
  }

  private val component = ScalaComponent.builder[SimulationCollector]("GlobalStats")
    .renderBackend[Backend]
    .build

  def apply(collector: SimulationCollector) =
    component(collector)
}


object WorldMap {
  case class Props(map: String,
                   areas: Map[String, AreaStatistics],
                   lines: Map[String, (Boolean, LineStatistics)],
                   selectedFn: Selection => Callback)

  class Backend($: BackendScope[Props, Boolean]) {
    var focused: Option[(Element, Element)] = None

    def render(p: Props, selected: Boolean) = {
      <.div(
        ^.className := "map",
        (^.className := "some-selected").when(selected),
        // unfocus if click on the background
        ^.onClick --> (Callback {
          focused foreach {
            case (el, anim) ⇒
              el.classList.remove("selected")
              anim.asInstanceOf[js.Dynamic].beginElement()
          }
          focused = None
        } >> $.setState(false)
          >> p.selectedFn(NoSelection)),
        ^.dangerouslySetInnerHtml := p.map
      )
    }

    def focus(selection: Selection, el: Element, fa: Element, ufa: Element) = {
      focused = if (el.classList.toggle("selected")) {
        focused foreach { case (ol, _) ⇒ ol.classList.remove("selected") }
        fa.asInstanceOf[js.Dynamic].beginElement()
        Some((el, ufa))
      } else {
        ufa.asInstanceOf[js.Dynamic].beginElement()
        None
      }

      ($.setState(focused.nonEmpty) >>
        ($.props >>= (p ⇒ p.selectedFn(
          if (focused.nonEmpty) selection else NoSelection))))
        .runNow()
    }

    def mounted: Callback = {
      // receiveProps will only be called on subsequent updates, so to
      // get correct initial values we need to call it here (and of
      // course it cannot be done in willMount since there we don't
      // have DOM elements yet)
      ($.props |> (p ⇒ receiveProps(p))).flatten >>
      Callback {
        // setting up click handler is needed only once, when the original
        // DOM is created
//        println("component mounted")
        val elements = Main.areas.map {
          case (id, area) ⇒ (id, area.mapId, area.selectedAnimationId, area.unselectedAnimationId, AreaSelection(id))
        } ++ Main.lines.map {
          case (id, line) ⇒ (id, line.mapId, line.selectedAnimationId, line.unselectedAnimationId, LineSelection(id))
        }

        for ((id, mapId, aId, faId, selection) ← elements) {
          (document.querySelector(mapId),
            document.querySelector(aId),
            document.querySelector(faId)) match {

            case bad@((null, _, _) | (_, null, _) | (_, _, null)) ⇒
              println(s"Failed to find $mapId, $aId or $faId")

            case (el: SVGElement, fa: Element, ufa: Element) ⇒
              el.onclick = {
                ev: dom.Event ⇒
                  focus(selection, el, fa, ufa)
                  ev.stopPropagation()
              }
          }
        }
      }
    }

    val quant = 10

    // old id -> cls values
    val oldCls = mutable.Map.empty[String, String]

    def receiveProps(props: Props): Callback =
      Callback {
//        println("receive props")

        val elements: Iterable[(String, String, Option[String])] = Main.areas.map {
          case (id, area) ⇒
            val data = props.areas(id)
            val loss = data.total.toSeq.map(_ < 0)
            val n = loss.length
            val used = loss.count(b ⇒ b).toDouble / n
            (id, area.mapId, if (n > 0) Some(s"power-${(quant * (1 - used)).toInt}") else None)
        } ++ Main.lines.map {
          case (id, line) ⇒
            val (_, data) = props.lines(id)
            val transferAndCapacity = data.transfer.toSeq.zip(data.capacity.toSeq)
            val n = transferAndCapacity.length
            val used = transferAndCapacity.map {
              case (t, c) ⇒ t / c / n
            }.sum
            (id, line.mapId, if (n > 0) Some(s"line-${(quant * used).toInt}") else None)
        }

        elements.foreach {
          case (id, elId, clsOpt) ⇒
            document.querySelector(elId) match {
              case null ⇒
              case el: SVGElement ⇒
                oldCls.get(elId) match {
                  case Some(old) ⇒
                    el.classList.remove(old)
                  case None ⇒
                }

                clsOpt match {
                  case Some(cls) ⇒
                    el.classList.add(cls)
                    oldCls(elId) = cls
                  case None ⇒
                    oldCls.remove(elId)
                }
            }
        }

        // show or hide line disabled element
        Main.lines.foreach {
          case (id, line) ⇒
            val (disabled, _) = props.lines(id)

            document.querySelector(line.disabledId) match {
              case null ⇒
              case el: SVGPathElement ⇒
//                println(s"id=$id did=${line.disabledId} el=$el enabled=$disabled")
                el.style.visibility = if (disabled) "visible" else "hidden"
            }
        }
      }
  }

  private val component = ScalaComponent.builder[Props]("WorldMap")
    .initialState(false)
    .renderBackend[Backend]
    .componentDidMount(_.backend.mounted)
    .componentWillReceiveProps(i ⇒ i.backend.receiveProps(i.nextProps))
    .build

  def apply(map: String,
            areas: Map[String, AreaStatistics],
            lines: Map[String, (Boolean, LineStatistics)],
            selectedFn: Selection ⇒ Callback) =
    component(Props(map, areas, lines, selectedFn))
}
