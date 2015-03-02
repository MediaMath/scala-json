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
  def now = EpochDeadline(Duration(System.currentTimeMillis, TimeUnit.MILLISECONDS))

  def apply(tsSeconds: Double): EpochDeadline = from(tsSeconds)

  def from(tsSeconds: Double) =
    EpochDeadline(Duration((tsSeconds * 1000).toLong, TimeUnit.MILLISECONDS))

  implicit def ordering: Ordering[EpochDeadline] = this

  //private val logging = new SLF4JLoggingAdapter(classOf[EpochDeadline])

  implicit val deadlineAccessor: JSONAccessor[EpochDeadline] =
    JSONAccessor.create(
      { x: EpochDeadline =>
        val mils = x.time.toMillis

        JNumber(math.max(mils / 1000.0, 0.0))
      },
      { x: JValue =>
        val num = x.toJNumber
        require(!num.isNaN, "bad number " + x)

        try {
          EpochDeadline.from(math.max(num.num, 0.0))
        } catch {
          //shouldnt happen... but it did
          case x: IllegalArgumentException =>
            //logging.error(x, "IllegalArgumentException when parsing EpochDeadline")
            EpochDeadline.now
        }
      }
    )

  override def compare(x: EpochDeadline, y: EpochDeadline): Int = x compare y
}