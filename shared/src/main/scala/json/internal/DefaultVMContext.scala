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

package json.internal

import json._
import json.internal.DefaultVMContext.PrimitiveArray
import json.internal.PrimitiveJArray.Builder

import scala.reflect.ClassTag

trait BaseVMContext {
  def fromString(str: String): JValue
  def fromAny(value: Any): JValue
  def quoteJSONString(string: String, builder: SimpleStringBuilder): SimpleStringBuilder
  def newVMStringBuilder: SimpleStringBuilder
  def createPrimitiveArray[T: ClassTag](length: Int): DefaultVMContext.PrimitiveArray[T]
  def extractPrimitiveJArray[T: ClassTag: PrimitiveJArray.Builder](x: Iterable[T]): Option[JArray]

  private[json] trait JValueCompanionBase

  private[json] trait JValueBase
  private[json] trait JBooleanBase
  private[json] trait JNumberBase
  private[json] trait JArrayBase
  private[json] trait JObjectBase
  private[json] trait JUndefinedBase
  private[json] trait JNullBase
  private[json] trait JStringBase

  object PrimitiveJArrayExtractor {
    def unapply[T: ClassTag: PrimitiveJArray.Builder](x: Iterable[T]): Option[JArray] =
      extractPrimitiveJArray(x)
  }
}

object DefaultVMContext {
  //to be replaced via shadowing by build for proper VM
  object VMContext extends BaseVMContext {
    private[json] trait JValueCompanionBase

    private[json] trait JValueBase
    private[json] trait JBooleanBase
    private[json] trait JNumberBase
    private[json] trait JArrayBase
    private[json] trait JObjectBase
    private[json] trait JUndefinedBase
    private[json] trait JNullBase
    private[json] trait JStringBase

    def fromString(str: String): JValue = ???
    def fromAny(value: Any): JValue = ???
    def quoteJSONString(string: String, builder: SimpleStringBuilder): SimpleStringBuilder = ???
    def newVMStringBuilder: SimpleStringBuilder = ???
    def createPrimitiveArray[T: ClassTag](length: Int): PrimitiveArray[T] = ???
    def extractPrimitiveJArray[T: ClassTag : Builder](x: Iterable[T]): Option[JArray] = ???
  }

  type PrimitiveArray[T] = scala.collection.mutable.IndexedSeq[T]
}
