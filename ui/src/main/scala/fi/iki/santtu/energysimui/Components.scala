package fi.iki.santtu.energysimui

import fi.iki.santtu.energysim.model.{Area, Line, Source, World}
import fi.iki.santtu.energysim.{AreaStatistics, LineStatistics}
import fi.iki.santtu.energysimui.Main.{AreaData, LineData, Selection}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, _}
import org.scalajs.dom

import scala.util.Try


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
//      println(s"render: values=$values")
      <.div(
        <.div(s"AREA: ${p.area}"), <.br,
        p.area.sources.toVdomArray(source ⇒
          <.div(^.className := "source",
            ^.key := source.id,
            <.div(^.className := "form-group",
              <.label(^.`for` := s"source-${source.id}-mw",
                s"$source capacity (MW)"),
              <.input(
                ^.className := "form-control",
                (^.className := "edited").when(source.unitCapacity != values(source.id)),
                ^.`type` := "number",
                ^.key := s"${source.id}",
                ^.defaultValue := s"${values(source.id)}",
                ^.id := s"source-${source.id}-mw",
                ^.onKeyDown ==> { e: ReactEventFromInput ⇒
                  e.nativeEvent match {
                    case key: dom.KeyboardEvent if key.key == "Enter" ⇒
                      p.areaUpdated(p.area.update(source.copy(capacity = values(source.id))))
                    case _ ⇒
                      Callback {}
                  }
                },
                ^.onChange ==> { e: ReactEventFromInput ⇒
                  Try(e.target.value.toInt).toOption match {
                    case Some(value) ⇒
                      $.setState(values.updated(source.id, value)) >>
                        $.forceUpdate // uuugly
                    case None ⇒ Callback {}
                  }
                })))))
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
        <.div(^.className := "form-group",
          <.label(^.`for` := s"line-${p.line.id}-mw",
            "Transmission capacity (MW)"),
          <.input(
            ^.className := "form-control",
            (^.className := "edited").when(p.line.unitCapacity != inputValue),
            ^.`type` := "number",
            ^.key := s"${p.line.id}",
            ^.defaultValue := s"${inputValue}",
            ^.id := s"line-${p.line.id}-mw",
            ^.onKeyDown ==> { e: ReactEventFromInput ⇒
              e.nativeEvent match {
                case key: dom.KeyboardEvent if key.key == "Enter" ⇒
                  p.lineUpdated(p.line.copy(capacity = inputValue))
                case _ ⇒
                  Callback {}
              }
            },
            ^.onChange ==> { e: ReactEventFromInput ⇒
              Try(e.target.value.toInt).toOption match {
                case Some(value) ⇒
                  Callback {
                    inputValue = value
                  } >>
                    $.forceUpdate // uuugly
                case None ⇒ Callback {}
              }
            })))
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
