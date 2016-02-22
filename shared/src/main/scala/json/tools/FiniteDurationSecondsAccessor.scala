/*
 * Copyright 2016 MediaMath, Inc
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
 */

package json.tools

import json._

import scala.concurrent.duration._

/**
 * Serializes FiniteDuration as the floating-point number of seconds with fractional component.
 * This is not globally implicit because it is use-case dependant and should generally only
 * be imported into the specific scope that it's needed.
 */
object FiniteDurationSecondsAccessor extends FiniteDurationSecondsAccessor

/**
 * Base trait for [[FiniteDurationSecondsAccessor]], useful for extending if needed.
 */
trait FiniteDurationSecondsAccessor extends JSONAccessorProducer[FiniteDuration, JNumber] {
  def fromJSON(js: JValue): FiniteDuration = js.jNumber.toInt.seconds

  def createJSON(obj: FiniteDuration): JNumber = JNumber(obj.toSeconds.toInt)

  //use int here so we can get a numeric swagger type
  def clazz: Class[_] = classOf[Int]

  def describe = baseDescription

  implicit def acc: JSONAccessorProducer[FiniteDuration, JNumber] = this
}