/*
 * Copyright 2015 MediaMath, Inc
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

import java.util.Date
import java.util.concurrent.TimeUnit

import json._

import scala.concurrent.duration._

final case class EpochDeadline(time: FiniteDuration) extends Ordered[EpochDeadline] {
  def +(dur: FiniteDuration) = EpochDeadline(time + dur)
  def -(dur: FiniteDuration) = EpochDeadline(time - dur)
  def -(dl: EpochDeadline) = time - dl.time

  def timeLeft = this - EpochDeadline.now
  def timeExpired = -timeLeft
  def isOverdue = this < EpochDeadline.now
  def hasTimeLeft = !isOverdue

  def compare(other: EpochDeadline) = time compare other.time

  def date = new Date(time.toMillis)

  override def toString = s"(${timeLeft}).fromNowEpoch"
}

object EpochDeadline extends Ordering[EpochDeadline] {
  val epoch = EpochDeadline(0.seconds)

  implicit val deadlineAccessor: JSONAccessor[EpochDeadline] =
    JSONAccessor.create(
      { x: EpochDeadline =>
        val mils = x.time.toMillis

        JNumber(mils / 1000.0)
      },
      { x: JValue =>
        val num = x.toJNumber
        require(!num.isNaN, "bad number " + x)

        EpochDeadline.from(num.num)
      }
    )

  def now = epoch + System.currentTimeMillis.millis

  def apply(tsSeconds: Double): EpochDeadline = from(tsSeconds)

  def from(tsSeconds: Double) = epoch + (tsSeconds * 1000).millis

  implicit def ordering: Ordering[EpochDeadline] = this

  override def compare(x: EpochDeadline, y: EpochDeadline): Int = x compare y
}
