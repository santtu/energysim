package fi.iki.santtu.energysimui

import com.payalabs.scalajs.react.bridge.{ReactBridgeComponent, WithProps}
import fi.iki.santtu.energysim.model.{Area, Line, World}
import fi.iki.santtu.energysim.{AreaStatistics, LineStatistics}
import fi.iki.santtu.energysimui.Main.{AreaData, LineData}
import japgolly.scalajs.react.extra.components.TriStateCheckbox
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, _}
import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.raw._

import scala.collection.mutable
import scala.scalajs.js
import scala.util.Try

object utils {
  def tri(b: Seq[Boolean]): Option[Boolean] =
    (b.contains(true), b.contains(false)) match {
      case (true, false) ⇒ Some(true)
      case (false, true) ⇒ Some(false)
      case _ ⇒ None
    }
}

/*
data - the data set used to build the sparkline

limit - optional, how many data points to display at once

width, height - dimensions of the generated sparkline in the SVG viewbox. This will be automatically scaled (i.e. responsive) inside the parent container by default.

svgWidth, svgHeight - If you want absolute dimensions instead of a responsive component set these attributes.

preserveAspectRatio - default: 'none', set this to modify how the sparkline should scale

margin - optional, offset the chart

min, max - optional, bound the chart
 */


object Sparklines extends ReactBridgeComponent {
  def apply(data: js.UndefOr[Seq[Double]] = js.undefined,
            limit: js.UndefOr[Int] = js.undefined,
            width: js.UndefOr[Int] = js.undefined,
            height: js.UndefOr[Int] = js.undefined,
            svgWidth: js.UndefOr[Int] = js.undefined,
            svgHeight: js.UndefOr[Int] = js.undefined,
            preserveAspectRatio: js.UndefOr[String] = js.undefined,
            margin: js.UndefOr[Int] = js.undefined,
            min: js.UndefOr[Double] = js.undefined,
            max: js.UndefOr[Double] = js.undefined): WithProps = auto
//             defaultValue: js.UndefOr[Seq[String]] = js.undefined,
//            value: js.UndefOr[Seq[String]] = js.undefined,
//            placeholder: js.UndefOr[String] = js.undefined,
//            onChange: js.UndefOr[js.Array[String] => Callback] = js.undefined,
//            validate: js.UndefOr[String => CallbackTo[Boolean]] = js.undefined,
//            transform: js.UndefOr[String => CallbackTo[String]] = js.undefined): WithPropsNoChildren = autoNoChildren
}

object SparklinesLine extends ReactBridgeComponent {
  def apply(color: js.UndefOr[String] = js.undefined,
            style: js.UndefOr[Map[String, Any]] = js.undefined): WithProps = auto
}

object SparklinesSpots extends ReactBridgeComponent {
  def apply(size: js.UndefOr[Int] = js.undefined,
            style: js.UndefOr[Map[String, Any]] = js.undefined): WithProps = auto
}

/*

    static propTypes = {
        type: PropTypes.oneOf(['max', 'min', 'mean', 'avg', 'median', 'custom']),
        value: PropTypes.number,
        style: PropTypes.object
    };
 */
object SparklinesReferenceLine extends ReactBridgeComponent {
  def apply(`type`: js.UndefOr[String] = js.undefined,
            value: js.UndefOr[Double] = js.undefined,
            style: js.UndefOr[Map[String, Any]] = js.undefined,
            margin: js.UndefOr[Int] = js.undefined): WithProps = auto
}


/**
  * Generic number input with a "enabled" checkbox. This puts off
  * updates on the value until user has finished the input.
  */

object EnabledNumber {
  case class State(checked: Option[Boolean], value: Double, inInput: Boolean = false)
  case class Props(checked: Option[Boolean], value: Double, label: String,
                   callback: (Double, Option[Boolean]) ⇒ Callback,
                   min: Double, max: Double, step: Double)

  class Backend($: BackendScope[Props, State]) {
    def render(p: Props, s: State): VdomElement = {
      def update(state: State) =
          $.setState(state) >>
          // send new state only if not in input and values have changed
          (if (!state.inInput &&
            (p.value != state.value ||
              p.checked != state.checked))
            p.callback(state.value, state.checked)
          else
            Callback.empty)

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
      $.setState(State(props.checked, props.value))
  }

  private val component = ScalaComponent.builder[Props]("EnabledNumber")
    .initialState(State(Some(true), 0))
    .renderBackend[Backend]
    .componentWillMount(i ⇒ i.backend.receiveProps(i.props))
    .componentWillReceiveProps(i ⇒ i.backend.receiveProps(i.nextProps))
    .build

  def apply(label: String = "",
            value: Double = 0,
            checked: Option[Boolean] = Some(true),
            callback: (Double, Option[Boolean]) ⇒ Callback,
            min: Double, max: Double, step: Double = 1) =
    component(Props(checked, value, label, callback, min, max, step))
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
        utils.tri(area.sources.map(!_.disabled)))

      <.div(
        EnabledNumber(
          "Production capacity",
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
              println(s"production capacity changes $productionCapacity --> $v, scale=$scale")
              a = a.scaleSourceCapacity(scale)
            }

            if (a != area)
              p.areaUpdated(a)
            else
              Callback.empty
          },
          min = 0, max = 10000, step = 100),

        area.sources.toVdomArray(source ⇒ {
          val typeinfo = Main.types(source.capacityType.id)
          <.div(^.className := "source",
            ^.key := source.id,
            EnabledNumber(
              label = typeinfo.name,
              value = state(source.id)._1,
              checked = Some(!state(source.id)._2),
              callback = { (value, enabled) ⇒
                p.areaUpdated(area.update(source.copy(
                  capacity = value.toInt,
                  disabled = !(enabled.get)))) >>
                  $.modState(s ⇒ s.updated(source.id, (value.toInt, !(enabled.get))))
              },
              min = 0, max = 10000, step = 5))
        }))
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
        utils.tri(sources.map(!_.disabled)))

      val (importCapacity, importEnabled) = (
        lines.map(_.unitCapacity).sum,
        utils.tri(lines.map(!_.disabled)))

//      println(s"prod=${sources.map(!_.disabled)} -> $productionEnabled")
//      println(s"import=${lines.map(!_.disabled)} -> $importEnabled")

      <.div(
        EnabledNumber(
          "Production capacity",
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
              println(s"production capacity changes $productionCapacity --> $v, scale=$scale")
              w = w.scaleSourceCapacity(scale)
            }

            update(w)
          },
          min = 5000, max = 100000, step = 100),

        Main.types.toVdomArray {
          case (id, data) ⇒
            val sources = world.areas.filter(!_.external).flatMap(_.sources).filter(_.capacityType.id == id)
            val capacityType = world.typeById(id).get
            val (typeCapacity, typeEnabled) = (
              sources.map(_.unitCapacity).sum,
              utils.tri(sources.map(!_.disabled)))
            <.div(^.key := id,
              EnabledNumber(
                data.name,
                typeCapacity, typeEnabled,
                { (v, c) ⇒
                  var w = world
                  c match {
                    case Some(enabled) ⇒
                      w = w.setSourcesDisabledByType(!enabled, capacityType)
                    case None ⇒
                  }
                  if (v != typeCapacity) {
                    val scale = v.toDouble / typeCapacity
                    w = w.scaleSourceCapacityByType(scale, capacityType)
                  }
                  update(w)
                },
                min = 0, max = 20000, step = 100))
        },

        <.hr,
        EnabledNumber(
          "Import capacity",
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
              println(s"import capacity changes $importCapacity --> $v")
              ???
            }

            update(w)
          },
          min = 0, max = 10000, step = 100),
      )
    }

//    def receiveProps(props: Props): Callback =
//      $.setState((props.line.unitCapacity, props.line.disabled))
  }

  private val component = ScalaComponent.builder[Props]("GlobalInfo")
//    .initialState((0, false))
    .renderBackend[Backend]
//    .componentWillMount(i ⇒ i.backend.receiveProps(i.props))
//    .componentWillReceiveProps(i ⇒ i.backend.receiveProps(i.nextProps))
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
        "Power security", <.br,
        <.span(^.className := "power-security",
          f"${100.0 - data.loss.percentage}%.0f%%"), <.br,

        "Power balance", <.br,
        <.span(^.className := "current",
          f"${data.total / 1e3}%.1s GW",
        ),
        Sparklines(data = data.total.toSeq)(
          SparklinesLine(style = Map("fill" → "none"))(),
          SparklinesSpots()(),
          SparklinesReferenceLine(`type` = "custom", value = 0.0)(),
        ),

        "Transfer",  <.br,
        <.span(^.className := "current",
          f"${data.transfer / 1000}%.1s GW"),
        Sparklines(data = data.transfer.toSeq)(
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

        "CO2 emissions",  <.br,
        <.span(^.className := "current",
          f"${data.ghg * Main.ghgScaleFactor / 1e6}%.1s Mt/a"),
        Sparklines(data = data.ghg.toSeq, min = 0.0)(
          SparklinesLine(color = "#253e56")(),
          SparklinesSpots()(),
          SparklinesReferenceLine(`type` = "custom", value = data.ghg.mean)(),
        ),
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

        "Capacity", <.br,
        <.span(^.className := "current",
          f"${data.capacity / 1000}%.1s GW"),
        Sparklines(data = data.capacity.toSeq, min = 0.0)(
          SparklinesLine(color = "#253e56")(),
          SparklinesSpots()(),
          SparklinesReferenceLine(`type` = "custom", value = data.capacity.mean)(),
        ),

        // "transfer" is absolute value, but we want to show +- as
        // the direction
        "Transfer", <.br,
        <.span(^.className := "current",
          f"${data.used / 1000}%.1s GW"),
        Sparklines(data = data.used.toSeq)(
          SparklinesLine(style = Map("fill" → "none"), color = "#253e56")(),
          SparklinesSpots()(),
          SparklinesReferenceLine(`type` = "custom", value = 0.0)(),
        ),
      )
    }
  }

  private val component = ScalaComponent.builder[Props]("LineStats")
    .renderBackend[Backend]
    .build

  def apply(p: Props) = component.withKey(p._1.id)(p)
}


object GlobalStats {
  type Props = (AreaStatistics, AreaStatistics)

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props) = {
      val (global, external) = p

      <.div(
        ^.className := "statistics",
        "Power security", <.br,
        <.span(^.className := "power-security",
          f"${100.0 - global.loss.percentage}%.0f%%"), <.br,

        "Power balance", <.br,
        <.span(^.className := "current",
          f"${global.total / 1e3}%.1s GW"),
        Sparklines(data = global.total.toSeq)(
          SparklinesLine(style = Map("fill" → "none"))(),
          SparklinesSpots()(),
          SparklinesReferenceLine(`type` = "custom", value = 0.0)(),
        ),

        "Import", <.br,
        <.span(^.className := "current",
          f"${external.transfer / -1e3}%.1s GW"),
        Sparklines(data = (external.transfer * -1).toSeq, min = 0.0)(
          SparklinesLine()(),
          SparklinesSpots()(),
          SparklinesReferenceLine(`type` = "custom", value = -external.transfer.mean)(),
        ),

        "Production", <.br,
        <.span(^.className := "current",
          f"${global.generation / 1e3}%.1s GW"),
        Sparklines(data = global.generation.toSeq, min = 0.0)(
          SparklinesLine()(),
          SparklinesSpots()(),
          SparklinesReferenceLine(`type` = "custom", value = global.generation.mean)(),
        ),

        "CO2 emissions",  <.br,
        <.span(^.className := "current",
          f"${global.ghg * Main.ghgScaleFactor / 1e6}%.1s Mt/a"),
        Sparklines(data = global.ghg.toSeq, min = 0.0)(
          SparklinesLine(color = "#253e56")(),
          SparklinesSpots()(),
          SparklinesReferenceLine(`type` = "custom", value = global.ghg.mean)(),
        ),

      )
    }
  }

  private val component = ScalaComponent.builder[Props]("GlobalStats")
    .renderBackend[Backend]
    .build

  def apply(global: AreaStatistics, external: AreaStatistics) =
    component((global, external))
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