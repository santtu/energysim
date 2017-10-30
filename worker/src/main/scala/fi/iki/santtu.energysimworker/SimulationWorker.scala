package fi.iki.santtu.energysimworker

import fi.iki.santtu.energysim.JsonDecoder
import fi.iki.santtu.energysim.model.World
import fi.iki.santtu.energysim.simulation.ScalaSimulation
import fi.iki.santtu.energysimworker.WorkerOperation.Op
import fi.iki.santtu.energysimworker.WorkerState.State
import org.scalajs.dom.raw.MessageEvent

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.{JSApp, timers}
import scala.scalajs.js.annotation.{JSExport, ScalaJSDefined}
import scala.scalajs.js.timers.SetIntervalHandle

object WorkerState extends Enumeration {
  type State = Value
  val Started, Stopped, Result = Value
}

object WorkerOperation extends Enumeration {
  type Op = Value
  val Nop, SetWorld, Start, Stop = Value
}

@ScalaJSDefined
class Message(val op: Int,
              val world: js.UndefOr[String] = js.undefined) extends js.Object {
}

object Message {
  def apply(op: Op, world: js.UndefOr[String] = js.undefined) =
    new Message(op.id, world)
}

@ScalaJSDefined
class Reply(val state: Int,
            val rounds: Int,
            val interval: Double) extends js.Object


object Reply {
  def apply(state: State, rounds: Int = 0, interval: Double = 0.0) =
    new Reply(state.id, rounds, interval)
}

object SimulationWorker extends JSApp {
  val roundsPerStep = 100

  import WorkerOperation._
  import WorkerState._

  var world: Option[World] = None
  var timer: Option[SetIntervalHandle] = None

  def simulateRound: Unit = {
    require(world.isDefined)

    val start = System.currentTimeMillis()
    val result = ScalaSimulation.simulate(world.get, roundsPerStep)
    val end = System.currentTimeMillis()
    val interval = (end - start) / 1000.0

    println(s"Simulate round, $roundsPerStep rounds")
    reply(Reply(Result, rounds = result.rounds.length, interval = interval ))
  }

  private def reply(r: Reply) =
    scalajs.js.Dynamic.global.postMessage(r)

  @JSExport def main(): Unit = {

    scalajs.js.Dynamic.global.onmessage = { (event: MessageEvent) =>
      val data = event.data.asInstanceOf[Message]

      println(s"Received message: ${WorkerOperation(data.op)}")

      WorkerOperation(data.op) match {
        case Nop ⇒ println("got nop")

        case SetWorld ⇒ println(s"got setworld")
          val decoded = JsonDecoder.decode(data.world.get.getBytes("UTF-8"))
          println(s"world decoded: $decoded")
          world = Some(decoded)

        case Start if world.isDefined && timer.isEmpty ⇒
          timer = Some(timers.setInterval(1.0)(simulateRound))
          reply(Reply(Started))
          println(s"started worker with timer $timer")

        case Stop if timer.nonEmpty ⇒
          println(s"stopping worker timer $timer")
          timers.clearInterval(timer.get)
          timer = None
          reply(Reply(Stopped))

        case Start ⇒
          println(s"start when cannot start -- world=$world timer=$timer")

        case Stop ⇒
          println(s"stop when already stopped")
      }
    }
  }
}
