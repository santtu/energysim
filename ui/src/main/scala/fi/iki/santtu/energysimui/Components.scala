package fi.iki.santtu.energysimui

import fi.iki.santtu.energysim.model.{Area, Line, World}
import fi.iki.santtu.energysim.{AreaStatistics, LineStatistics}
import fi.iki.santtu.energysimui.Main.{AreaData, LineData, Selection}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, _}
import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.raw._
import org.scalajs.dom.svg.SVG

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.Try


/**
  * Specific slider that does not fire change until 
  */
object Slider {

}

/**
  * Generic number input with a "enabled" checkbox. This puts off
  * updates on the value until user has finished the input.
  */

object EnabledNumber {
  case class State(checked: Boolean, value: Double, inInput: Boolean = false)
  case class Props(checked: Boolean, value: Double, label: String,
                   callback: (Double, Boolean) ⇒ Callback,
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

      <.div(
        <.div(
          ^.className := "form-row",
          <.div(
            ^.className := "col",
            <.input(
              ^.`type` := "checkbox",
              ^.className := "form-check-input",
              ^.checked := s.checked,
              ^.onChange ==> { e: ReactEventFromInput =>
                update(s.copy(checked = e.target.checked))
              }),
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

  private val component = ScalaComponent.builder[Props]("LineInfo")
    .initialState(State(true, 0))
    .renderBackend[Backend]
    .componentWillMount(i ⇒ i.backend.receiveProps(i.props))
    .componentWillReceiveProps(i ⇒ i.backend.receiveProps(i.nextProps))
    .build

  def apply(label: String = "", value: Double = 0, checked: Boolean = true, callback: (Double, Boolean) ⇒ Callback,
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
        case Some(Right(id)) ⇒ p.world.lineById(id).get.areasSeq.contains(area)
        case _ ⇒ false
      }

    def isRelated(p: Props, line: Line): Boolean =
      p.selected match {
        case Some(Left(id)) ⇒ line.areasSeq.map(_.id).contains(id)
        case _ ⇒ false
      }

    def render(p: Props) = {
      <.div(
        p.areaData.toVdomArray { case (area, info) ⇒
          val focused = p.selected.contains(Left(area.id))
          <.div(
            ^.key := area.id,
            ^.className := "col area",
            (^.className := "focused").when(focused),
            (^.className := "related").when(isRelated(p, area)),
            (^.onClick --> p.updateSelection(Some(Left(area.id)))).when(!focused),
            (^.onClick --> p.updateSelection(None)).when(focused),
            info.name)
        },
        p.lineData.toVdomArray { case (line, info) ⇒
          val focused = p.selected.contains(Right(line.id))
          <.div(
            ^.key := line.id,
            ^.className := "col line",
            (^.className := "focused").when(focused),
            (^.className := "related").when(isRelated(p, line)),
            (^.onClick --> p.updateSelection(Some(Right(line.id)))).when(!focused),
            (^.onClick --> p.updateSelection(None)).when(focused),
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
      val info = Main.areas(p.area.id)

      <.div(
        <.div(^.className := "description",
          info.name),
        p.area.sources.toVdomArray(source ⇒ {
          val typeinfo = Main.types(source.capacityType.id)
          <.div(^.className := "source",
            ^.key := source.id,
            EnabledNumber(
              label = typeinfo.name,
              value = state(source.id)._1,
              checked = !state(source.id)._2,
              callback = { (value, enabled) ⇒
                p.areaUpdated(p.area.update(source.copy(
                  capacity = value.toInt,
                  disabled = !enabled))) >>
                  $.modState(s ⇒ s.updated(source.id, (value.toInt, !enabled)))
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
        <.div(^.className := "description", info.name),
        EnabledNumber(
          "Transmission capacity",
          p.line.unitCapacity,
          !p.line.disabled,
          { (v, c) ⇒
            p.lineUpdated(p.line.copy(disabled = !c, capacity = v.toInt)) },
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

object AreaStats {
  type Props = (Area, AreaStatistics)

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props) = {
      val (area, stats) = p
      <.span(stats.toString)
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
      val (line, stats) = p
      <.span(s"${line.unitCapacity} MW", <.br,
        stats.toString)
    }
  }

  private val component = ScalaComponent.builder[Props]("LineStats")
    .renderBackend[Backend]
    .build

  def apply(p: Props) = component.withKey(p._1.id)(p)
}


object WorldMap {
  case class Props(map: String, selectedFn: Selection => Callback)

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
              focused = None
              p.selectedFn(None)
          }
        } >> $.setState(false)),
        ^.dangerouslySetInnerHtml := p.map
      )
    }

    def focus(s: Either[String, String], el: Element, fa: Element, ufa: Element) = {
      focused = if (el.classList.toggle("selected")) {
        focused foreach { case (ol, _) ⇒ ol.classList.remove("selected") }
        fa.asInstanceOf[js.Dynamic].beginElement()
        Some((el, ufa))
      } else {
        ufa.asInstanceOf[js.Dynamic].beginElement()
        None
      }

      val selected = focused.map(_ ⇒ s)

      ($.setState(selected.nonEmpty) >>
        ($.props >>= (p ⇒ p.selectedFn(selected))))
        .runNow()
    }

    def mounted: Callback = Callback {
      val elements = Main.areas.map {
        case (id, area) ⇒ (id, area.mapId, area.selectedAnimationId, area.unselectedAnimationId, Left(id))
      } ++ Main.lines.map {
        case (id, line) ⇒ (id, line.mapId, line.selectedAnimationId, line.unselectedAnimationId, Right(id))
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

  private val component = ScalaComponent.builder[Props]("WorldMap")
    .initialState(false)
    .renderBackend[Backend]
    .componentDidMount(_.backend.mounted)
    .build

  def apply(map: String, selectedFn: Selection ⇒ Callback) = component(Props(map, selectedFn))
}