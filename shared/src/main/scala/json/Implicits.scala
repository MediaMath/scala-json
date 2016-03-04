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

package json

object Implicits extends Implicits
trait Implicits {
  //these were AnyVal at some point, but we're about to do a ton of allocations anyway...

  /** This is the class extension that allows you to use the .js method on any value */
  implicit class AnyValJSEx[T](val x: T) {
    def js[U <: JValue](implicit acc: JSONAccessorProducer.CreateJSON[T, U]): U = acc.createJSON(x)
  }

  implicit class JSONStringOps(str: String) {
    def parseJSON = JValue fromString str
  }
}
