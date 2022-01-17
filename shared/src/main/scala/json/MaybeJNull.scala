package json

/*
 Helper type that should be used if a field needs additional json null primitive logic. If a field has this type it will
 be possible to recognize that the incoming json literal was a `null`. In this case a JNullWrapper will be substituted.
 Generally this type should be used to discover if the corresponding json looked like: {"someField": null}
*/
abstract class MaybeJNull[+A] extends Product with Serializable{
  self =>
  def isJNull: Boolean
  def isDefined: Boolean = !isJNull
  def get: A
  @inline final def getOrElse[B >: A](default: => B): B =
    if (isJNull) default else this.get
  @inline final def map[B](f: A => B): MaybeJNull[B] =
    if (isJNull) JNullWrapper else ValueWrapper(f(this.get))
  @inline final def flatMap[B](f: A => MaybeJNull[B]): MaybeJNull[B] =
    if (isJNull) JNullWrapper else f(this.get)
  def flatten[B](implicit ev: A <:< MaybeJNull[B]): MaybeJNull[B] =
    if (isJNull) JNullWrapper else ev(this.get)
}


case object JNullWrapper extends MaybeJNull[Nothing] {
  def isJNull = true
  def get = throw new NoSuchElementException("JNullWrapper.get")
}

final case class ValueWrapper[+A](x: A) extends MaybeJNull[A] {
  def isJNull = false
  def get = x
}