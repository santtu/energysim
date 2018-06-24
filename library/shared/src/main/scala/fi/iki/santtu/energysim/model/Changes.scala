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

package fi.iki.santtu.energysim.model

final case class IdentifierNotFound(id: String) extends Exception(s"Source or line with identifier '$id' not found")
final case class InconsistentWorld(message: String) extends Exception(message)

case class Change(id: String,
  enabled: Option[Boolean] = None,
  capacity: Option[Int] = None) {

  private def update(u: Source): Source =
    u.copy(disabled = !enabled.getOrElse(!u.disabled),
      capacity = capacity.getOrElse(u.unitCapacity))

  private def update(u: Line): Line =
    u.copy(disabled = !enabled.getOrElse(!u.disabled),
      capacity = capacity.getOrElse(u.unitCapacity))

  def apply(world: World): World = {
    world.units.find(_.id == id) match {
      case None => throw IdentifierNotFound(id)
      case Some(l: Line) => world.updateLine(update(l))
      case Some(s: Source) => world.updateSource(update(s))
      case Some(u: Unit) => throw IdentifierNotFound(id) // it is a drain ... don't support that
      case _ => ???  // this should not happen
    }
  }
}

case class Changes(worldName: String,
  worldVersion: Int,
  changes: Seq[Change] = Seq.empty) {

  def apply(world: World): World = {
    if (world.name != worldName || world.version != worldVersion)
      throw new InconsistentWorld("Changes for $worldName@$worldVersion cannot be applied to ${world.name}@${world.version}")

    changes.foldLeft(world)((w, c) => c(w))
  }
}

object Changes {
  def apply(origin: World, world: World): Changes = {
    require(origin.name == world.name && origin.version == world.version)
    // make a map of id -> unit for changed world
    val units = world.units.map { u => u.id -> u } toMap

    // enumerate through identifiers in origin, filter unchanged
    val changes = origin.units.filter {
      ou => ou != units(ou.id)
    }

    Changes(origin.name, origin.version,
      changes.map {
        ou =>
        val id = ou.id
        val wu = units(id)
        val (capacity, enabled) =
          (ou.unitCapacity != wu.unitCapacity, ou.disabled != wu.disabled) match {
            case (true, true) => (Some(wu.unitCapacity), Some(!wu.disabled))
            case (true, false) => (Some(wu.unitCapacity), None)
            case (false, true) => (None, Some(!wu.disabled))
            case (false, false) => ???  // this should not happen
          }
        Change(id, enabled, capacity)
      })
  }
}
