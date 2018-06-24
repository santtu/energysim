/*
 * Copyright 2017 Santeri Paavolainen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package fi.iki.santtu.energysim

import fi.iki.santtu.energysim.model._
import io.circe.parser.decode
import io.circe._
import org.scalatest.{FlatSpec, Matchers}

class ChangesSpec extends FlatSpec with Matchers {
  val world = {
    val area1 = Area("area-1",
          sources = Seq(
            Source("source-1", capacity = 100),
            Source("source-2", capacity = 50)))
    val area2 = Area("area-2",
      drains = Seq(
        Drain("drain-1", capacity = 150)))

    World(name = "brutopia", version = 2,
      areas = Seq(area1, area2),
      lines = Seq(Line("line-1", area1 = area1, area2 = area2)))
  }

  "Empty set of changes" should "not change the world" in {
    val changes = Changes(world.name, world.version)
    val other = changes(world)
    world shouldBe other
  }

  "Inconsistent name" should "result in an exception" in {
    val changes = Changes("atlantis", world.version)
    an [InconsistentWorld] should be thrownBy changes(world)
  }

  "Inconsistent version" should "result in an exception" in {
    val changes = Changes(world.name, 1)
    an [InconsistentWorld] should be thrownBy changes(world)
  }

  "Invalid identifiers in changes" should "result in an exception" in {
    val changes = Changes(world.name, world.version,
      Seq(Change("non-existent", Some(true), Some(100))))
    an [IdentifierNotFound] should be thrownBy changes(world)
  }


  "Changes on non-changeable elements" should "result in exceptions" in {
    val changes1 = Changes(world.name, world.version,
      Seq(Change("area-1")))

    an [IdentifierNotFound] should be thrownBy changes1(world)

    val changes2 = Changes(world.name, world.version,
      Seq(Change("drain-1")))

    an [IdentifierNotFound] should be thrownBy changes2(world)
  }

  "Null changes" should "result in no changes" in {
    val changes = Changes(world.name, world.version,
      Seq(
        Change("source-1"),
        Change("source-2"),
        Change("line-1")))

    changes(world) shouldBe world
  }

  "Changes in capacities" should "result in changes" in {
    val changes = Changes(world.name, world.version,
      Seq(
        Change("source-1", capacity = Some(101)),
        Change("source-2", capacity = Some(51)),
        Change("line-1", capacity = Some(251))))

    val other = changes(world)

    other should not be world
  }

  "Changes in enabledness" should "result in changes" in {
    val changes = Changes(world.name, world.version,
      Seq(
        Change("source-1", enabled = Some(false)),
        Change("source-2", enabled = Some(false)),
        Change("line-1", enabled = Some(false))))

    val other = changes(world)

    other should not be world
  }

  "Changes in capacities and enabledness" should "result in changes" in {
    val changes = Changes(world.name, world.version,
      Seq(
        Change("source-1", capacity = Some(200), enabled = Some(false))))

    val other = changes(world)

    other should not be world
  }

  "No changes in world" should "result in empty set of changes" in {
    val changes = Changes(world, world)
    changes.worldName shouldBe world.name
    changes.worldVersion shouldBe world.version
    changes.changes shouldBe empty
  }

  "World changes in capacity" should "result in changes" in {
    val changes = Changes(world,
      world
        .updateSources(Seq(
          Source("source-1", capacity = 10),
          Source("source-2", capacity = 99)))
        .updateLine(world.lines(0).copy(capacity = 101)))

    changes.changes.toSet shouldBe Set(
      Change("source-1", capacity = Some(10)),
      Change("source-2", capacity = Some(99)),
      Change("line-1", capacity = Some(101)))
  }

  "World changes in enabledness" should "result in changes" in {
    val changes = Changes(world,
      world
        .updateSources(Seq(
          Source("source-1", capacity = 100, disabled = true),
          Source("source-2", capacity = 50, disabled = true)))
        .updateLine(world.lines(0).copy(disabled = true)))

    changes.changes.toSet shouldBe Set(
      Change("source-1", enabled = Some(false)),
      Change("source-2", enabled = Some(false)),
      Change("line-1", enabled = Some(false)))
  }

  "World changes in capacity and enabledness" should "result in changes" in {
    val changes = Changes(world,
      world
        .updateSources(Seq(
          Source("source-1", capacity = 10, disabled = true),
          Source("source-2", capacity = 99, disabled = true)))
        .updateLine(world.lines(0).copy(capacity = 101, disabled = true)))

    changes.changes.toSet shouldBe Set(
      Change("source-1", capacity = Some(10), enabled = Some(false)),
      Change("source-2", capacity = Some(99), enabled = Some(false)),
      Change("line-1", capacity = Some(101), enabled = Some(false)))
  }
}
