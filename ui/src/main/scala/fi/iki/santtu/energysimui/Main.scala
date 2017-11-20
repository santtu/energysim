package fi.iki.santtu.energysimui

import com.github.marklister.base64.Base64._
import fi.iki.santtu.energysim._
import fi.iki.santtu.energysim.model.World
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.{Router, _}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html.Element

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation._
import scala.util.{Failure, Success}


@JSExportTopLevel("EnergySim")
object Main {
  /**
    * Scale factor to convert kg/MWh -> kg -> t/a values for display.
    */
  val ghgScaleFactor = 1e-3 * 365 * 24

  // TODO: move these datas into external file like world definition
  // json and map svg already done

  /**
    * Container for UI-related things for each area, e.g. visible name,
    * description etc.
    */
  case class AreaData(name: String, mapId: String, selectedAnimationId: String, unselectedAnimationId: String)

  val areas: Map[String, AreaData] = Map(
    "north" → AreaData("Northern Finland", "#north", "#north-focus", "#world-focus"),
    "south" → AreaData("Southern Finland", "#south", "#south-focus", "#world-focus"),
    "east" → AreaData("Eastern Finland", "#east", "#east-focus", "#world-focus"),
    "west" → AreaData("Western Finland", "#west", "#west-focus", "#world-focus"),
    "central" → AreaData("Central Finland", "#center", "#center-focus", "#world-focus"),
  )

  /**
    * Container for UI-related stuff for lines.
    */

  case class LineData(name: String, mapId: String, selectedAnimationId: String, unselectedAnimationId: String)

  val lines: Map[String, LineData] = Map(
    "west-south" → LineData("West-South", "#west-south", "#west-south-focus", "#world-focus"),
    "west-central" → LineData("West-Central", "#west-center", "#west-center-focus", "#world-focus"),
    "south-east" → LineData("South-East", "#south-east", "#south-east-focus", "#world-focus"),
    "south-central" → LineData("South-Central", "#south-center", "#south-center-focus", "#world-focus"),
    "east-central" → LineData("East-Central", "#center-east", "#center-east-focus", "#world-focus"),
    "central-north" → LineData("Central-North", "#north-center", "#north-center-focus", "#world-focus"),
    "russia-east" → LineData("Russia (import)", "#russia-east", "#russia-east-focus", "#world-focus"),
    "sweden-west" → LineData("Sweden (import)", "#sweden-west", "#sweden-west-focus", "#world-focus"),
    "sweden-north" → LineData("Sweden (import)", "#sweden-north", "#sweden-north-focus", "#world-focus"),
    "estonia-south" → LineData("Estonia (import)", "#estonia-south", "#estonia-south-focus", "#world-focus"),
    "norway-north" → LineData("Norway (import)", "#norway-north", "#norway-north-focus", "#world-focus"),
  )

  // TODO: include icon (path to image) to use in UI, tooltip text
  case class TypeData(name: String, iconUrl: String)

  val types: Map[String, TypeData] = Map(
    "bio" → TypeData("Biofuel", ""),
    "peat" → TypeData("Peat", "/images/icons/peat.svg"),
    "wind" → TypeData("Wind", "/images/icons/wind.svg"),
    "hydro" → TypeData("Hydro", "/images/icons/hydro.svg"),
    "oil" → TypeData("Oil", "/images/icons/oil.svg"),
    "solar" → TypeData("Solar", ""),
    "nuclear" → TypeData("Nuclear", ""),
    "other" → TypeData("Other", ""),
    "gas" → TypeData("Natural gas", ""),
    "coal" → TypeData("Coal", "")
  )

  // these are ids, not direct references: left = area, right = line
  type Selection = Option[Either[String, String]]

  sealed trait Pages
  case class WorldPage(encoded: String) extends Pages {
    def world = JsonDecoder.decode(new String(encoded.toByteArray, "UTF-8"))
  }

  object WorldPage {
    def apply(w: World): WorldPage =
      WorldPage(JsonDecoder.encode(w).getBytes("UTF-8").toBase64)
  }

  def router(defaultWorld: World, worldMap: String) = {
    val defaultPage = WorldPage(defaultWorld)
    val routerConfig = Router(BaseUrl.until_#,
      RouterConfigDsl[Pages].buildConfig {
        dsl =>
          import dsl._

          val worldRoute =
            dynamicRouteCT("#" / string(".+").caseClass[WorldPage]) ~>
              dynRenderR((page: WorldPage, ctl) => {
                UserInterface(page.world, defaultWorld, worldMap, ctl)
              })

          (worldRoute)
            .notFound(redirectToPage(defaultPage)(Redirect.Replace))
            .setPostRender((prev, cur) ⇒ Callback {
            })
      })
    routerConfig()
  }

  object Loader {
    type Props = Seq[Future[Any]]

    class Backend($: BackendScope[Props, Int]) {
      def render(futures: Props, count: Int) = {
        <.div(
          ^.className := "loader",
          <.div(
            ^.className := "progress",
            <.div(
              ^.className := "progress-bar",
              ^.role := "progressbar",
              ^.width := s"${100 * count / futures.size}%")))
      }

      def init: Callback = $.props |> (_.foreach(_.onComplete(_ => $.modState(count ⇒ count + 1).runNow())))
    }

    val component = ScalaComponent.builder[Props]("Loader")
      .initialState(0)
      .renderBackend[Backend]
      .componentWillMount(_.backend.init)
      .build

    def apply(futures: Future[Any]*) =
      component(futures)
  }

  @JSExport
  def main(args: Array[String]): Unit = {
    val f1 = Ajax.get(Data.worldUrl)
    val f2 = Ajax.get(Data.mapUrl)

    val loaderElement = dom.document.querySelector("#loader")

    Loader(f1, f2).renderIntoDOM(loaderElement)

    val loader = for {
      w ← f1
      m ← f2
      _ ← Future { loaderElement.parentNode.removeChild(loaderElement) }
    } yield (Model.from(w.responseText, JsonDecoder), m.responseText)

    loader.onComplete {
      case Success((world, map)) ⇒
        println("Default world and world map loaded")
        router(world, map).renderIntoDOM(dom.document.getElementById("playground"))

      case Failure(ex) ⇒
        // TODO: show errors somewhere
        println(s"failed loading: $ex")
    }
  }
}
