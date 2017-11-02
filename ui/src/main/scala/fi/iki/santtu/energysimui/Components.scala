package fi.iki.santtu.energysimui

import fi.iki.santtu.energysim.model.{Area, Line, Source, World}
import fi.iki.santtu.energysim.{AreaStatistics, LineStatistics}
import fi.iki.santtu.energysimui.Main.{AreaData, LineData, Selection}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, _}
import org.scalajs.dom

import scala.util.Try


/**
  * Generic number input with a "enabled" checkbox.
  */

object EnabledNumber {
  case class State(checked: Boolean, value: Int)
  case class Props(checked: Boolean, value: Int, label: String, updated: (Int, Boolean) ⇒ Callback)

  class Backend($: BackendScope[Props, State]) {
    def init: Callback =
      $.props >>= { p ⇒ $.setState(State(p.checked, p.value)) }

    def render(p: Props, s: State): VdomElement = {
      <.div(^.className := "form-group",
        <.input(^.`type` := "checkbox",
          ^.checked := s.checked,
          ^.onChange ==> { e: ReactEventFromInput ⇒
            val checked = e.target.checked
            $.setState(s.copy(checked = checked)) >>
              p.updated(s.value, checked)
          }),
        " ",
        <.label(p.label,
          <.input(
            ^.`type` := "number",
            ^.className := "form-control",
            (^.className := "edited").when(p.value != s.value),
            ^.defaultValue := p.value.toString,
            ^.onKeyDown ==> { e: ReactEventFromInput ⇒
              e.nativeEvent match {
                case key: dom.KeyboardEvent if key.key == "Enter" ⇒
                  p.updated(s.value, s.checked)
                case _ ⇒
                  Callback {}
              }
            },
            ^.onChange ==> { e: ReactEventFromInput ⇒
              Try(e.target.value.toInt).toOption match {
                case Some(value) ⇒
                  $.modState(s ⇒ s.copy(value = value))
                case None ⇒ Callback {}
              }
            })))
    }
  }

  private val component = ScalaComponent.builder[Props]("LineInfo")
    .initialState(State(true, 0))
    .renderBackend[Backend]
    .componentWillMount(_.backend.init)
    .build

  def apply(label: String = "", value: Int = 0, checked: Boolean = true, updated: (Int, Boolean) ⇒ Callback) =
    component(Props(checked, value, label, updated))
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

  class Backend($: BackendScope[Props, Map[String, Int]]) {
    def init: Callback =
      $.props >>= { p ⇒
        $.setState(p.area.sources.map(s ⇒ s.id → s.unitCapacity).toMap)
      }

    def render(values: Map[String, Int], p: Props): VdomElement = {
      <.div(
        <.div(s"AREA: ${p.area}"), <.br,
        p.area.sources.toVdomArray(source ⇒
          <.div(^.className := "source",
            ^.key := source.id,
            EnabledNumber(s"${source.name.getOrElse(source.id)} (MW)",
              source.unitCapacity,
              !source.disabled,
              { (value, enabled) ⇒
                p.areaUpdated(p.area.update(source.copy(
                  capacity = value,
                  disabled = !enabled)))
              }))))
    }
  }

  private val component = ScalaComponent.builder[Props]("AreaInfo")
    .initialState(Map.empty[String, Int])
    .renderBackend[Backend]
    .componentWillMount(_.backend.init)
    .build

  def apply(p: Props) = component.withKey(p.area.id)(p)
}

object LineInfo {
  case class Props(line: Line, lineUpdated: (Line) ⇒ Callback)

  class Backend($: BackendScope[Props, Unit]) {
    var inputValue = -1

    def init: Callback =
      $.props |> { p ⇒ inputValue = p.line.unitCapacity }

    def render(p: Props): VdomElement = {
      <.div(
        <.div(s"LINE: ${p.line}"),
        EnabledNumber(
          "Transmission capacity (MW)",
          p.line.unitCapacity,
          !p.line.disabled,
          { (v, c) ⇒
            p.lineUpdated(p.line.copy(disabled = !c, capacity = v)) }))

    }
  }

  private val component = ScalaComponent.builder[Props]("LineInfo")
    .renderBackend[Backend]
    .componentWillMount(_.backend.init)
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
