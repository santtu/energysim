package fi.iki.santtu.energysimworker

import fi.iki.santtu.energysim.JsonDecoder
import fi.iki.santtu.energysim.model.World
import fi.iki.santtu.energysim.simulation.ScalaSimulation
import org.scalajs.dom.raw.MessageEvent

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.{JSExport, ScalaJSDefined}


object SimulationWorker extends JSApp {
  object Op extends Enumeration {
    type Op = Value
    val Nop, SetWorld, Start, Stop = Value
  }
  import Op._


  @ScalaJSDefined
  class Message(val op: Int, val world: js.UndefOr[String] = js.undefined) extends js.Object {
  }

  @JSExport def main(): Unit = {
    var world: Option[World] = None

    scalajs.js.Dynamic.global.onmessage = { (event: MessageEvent) =>
      println(s"worker got: event ${event.data} (${event.data.getClass})")

//      val data = event.data.asInstanceOf[Array]
//
//      println(s"worker got: data=$data")
//
//      (data.headOption.getOrElse(Nop), data.drop(1)) match {
//        case (Nop, _) ⇒
//          println("got NOP")
//        case (SetWorld, Seq(world)) ⇒
//          println(s"got SETWORLD: $world")
//        case (unknown, _) ⇒
//          println(s"Unknown operation: $unknown")

      
      val data = event.data.asInstanceOf[Message]
      println(s"data=$data")
      println(s"data.op=${data.op}")

      Op(data.op) match {
        case Nop ⇒ println("got nop")
        case SetWorld ⇒ println(s"got setworld")
          val decoded = JsonDecoder.decode(data.world.get.getBytes("UTF-8"))
          println(s"world decoded: $decoded")
          world = Some(decoded)

          val round = ScalaSimulation.simulate(world.get)
          println(s"simulated 1 round: $round")
      }

//      val data2 = event.data.asInstanceOf[mutable.Seq[Any]]
//      println(s"data2=$data2")
    }
  }
}
