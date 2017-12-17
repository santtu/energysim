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

package fi.iki.santtu.energysimui

import com.payalabs.scalajs.react.bridge.{ReactBridgeComponent, WithProps}
import scala.scalajs.js

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
}

object SparklinesLine extends ReactBridgeComponent {
  def apply(color: js.UndefOr[String] = js.undefined,
            style: js.UndefOr[Map[String, js.Any]] = js.undefined): WithProps = auto
}

object SparklinesSpots extends ReactBridgeComponent {
  def apply(size: js.UndefOr[Int] = js.undefined,
            style: js.UndefOr[Map[String, js.Any]] = js.undefined): WithProps = auto
}

object SparklinesReferenceLine extends ReactBridgeComponent {
  def apply(`type`: js.UndefOr[String] = js.undefined,
            value: js.UndefOr[Double] = js.undefined,
            style: js.UndefOr[Map[String, js.Any]] = js.undefined,
            margin: js.UndefOr[Int] = js.undefined): WithProps = auto
}

