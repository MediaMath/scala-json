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

package json

import json.tools.JProxy
import utest._

object ProxyTest extends TestSuite {
  trait A extends AProxy.To {
    def proxyTo: AProxy
  }

  object A {
    implicit val acc: JSONAccessor[A] = AProxy.proxyAccessor
  }

  case class B(a: String = "") extends A {
    def proxyTo = AProxy(this)
  }
  case class C(a: String = "") extends A {
    def proxyTo = AProxy(this)
  }

  object AProxy extends JProxy {
    type ProxyType = AProxy
    type ItemType = A

    val acc = ObjectAccessor.of[AProxy]
    val Cacc = ObjectAccessor.of[C]
    val Bacc = ObjectAccessor.of[B]

    def apply(c: C): AProxy = new AProxy(c.js(Cacc) + ("typ".js -> "c".js))
    def apply(b: B): AProxy = new AProxy(b.js(Bacc) + ("typ".js -> "b".js))
  }
  case class AProxy(js: JValue) extends AProxy.Proxy {
    def self: A = js("typ").jString.str match {
      case "c" => js.toObject(AProxy.Cacc)
      case "b" => js.toObject(AProxy.Bacc)
    }
  }

  val tests = TestSuite {
    "proxy assemble" - {
      B().js.toObject[A]
      B().js.toObject[B]
      B().js.toObject[C]

      require(B().js == C().js)
    }
  }
}
