package fi.iki.santtu.energysimui

import fi.iki.santtu.energysim.{AreaStatistics, LineStatistics}
import fi.iki.santtu.energysim.model.{Area, Line, World}
import fi.iki.santtu.energysimui.Main.{AreaData, LineData, Selection}
import japgolly.scalajs.react.{BackendScope, Callback}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._


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
    def isRelated(selected: Selection, area: Area) =
      selected match {
        case Some(Right(line)) ⇒ line.areasSeq.contains(area)
        case _ ⇒ false
      }

    def isRelated(selected: Selection, line: Line) =
      selected match {
        case Some(Left(area)) ⇒ line.areasSeq.contains(area)
        case _ ⇒ false
      }

    def render(p: Props) = {
      <.div(
        p.areaData.toVdomArray { case (area, info) ⇒
          val focused = p.selected.contains(Left(area))
          <.div(
            ^.key := area.id,
            ^.className := "col area",
            (^.className := "focused").when(focused),
            (^.className := "related").when(isRelated(p.selected, area)),
            (^.onClick --> p.updateSelection(Some(Left(area)))).when(!focused),
            (^.onClick --> p.updateSelection(None)).when(focused),
            info.name)
        },
        p.lineData.toVdomArray { case (line, info) ⇒
          val focused = p.selected.contains(Right(line))
          <.div(
            ^.key := line.id,
            ^.className := "col line",
            (^.className := "focused").when(focused),
            (^.className := "related").when(isRelated(p.selected, line)),
            (^.onClick --> p.updateSelection(Some(Right(line)))).when(!focused),
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
  class Backend($: BackendScope[Area, Unit]) {
    def render(area: Area): VdomElement =
      <.div(s"AREA: $area")
  }

  private val component = ScalaComponent.builder[Area]("AreaInfo")
    .renderBackend[Backend]
    .build

  def apply(area: Area) = component(area)
}

object LineInfo {
  class Backend($: BackendScope[Line, Unit]) {
    def render(line: Line): VdomElement =
      <.div(s"LINE: $line")
  }

  private val component = ScalaComponent.builder[Line]("LineInfo")
    .renderBackend[Backend]
    .build

  def apply(line: Line) = component(line)
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

  def apply(p: Props) = component(p)
}

object LineStats {
  type Props = (Line, LineStatistics)

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props) = {
      val (line, stats) = p
      <.span(stats.toString)
    }
  }

  private val component = ScalaComponent.builder[Props]("LineStats")
    .renderBackend[Backend]
    .build

  def apply(p: Props) = component(p)
}
