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

import json._

import scala.reflect.ClassTag

case class Pickle(data: JValue, className: String)
object Pickle {
  implicit val acc = ObjectAccessor.create[Pickle]({ x =>
    JObject("data".js -> x.data, "class".js -> x.className.js)
  },{
    case obj: JObject =>
      Pickle(obj("data"), obj("class").jString.str)
    case x => sys.error("expected pickle, got " + x)
  })
}

object AccessorRegistry extends AccessorRegistry
trait AccessorRegistry {
  @volatile
  private var _accessors = Map[Class[_], JSONAccessorProducer[_, JValue]]()

  @volatile
  private var classNameMap = Map[String, Class[_]]()

  def addAccessor(newAccs: TraversableOnce[JSONAccessorProducer[_, JValue]]): Unit = newAccs.foreach { acc =>
    addAccessor(acc.clazz, acc)
  }

  def addAccessor(clz: Class[_], acc: JSONAccessorProducer[_, JValue]): Unit = synchronized {
    _accessors += (clz -> acc)
    classNameMap += clz.getName -> clz
  }

  def addAccessor[T <: AnyRef](implicit acc: JSONAccessorProducer[T, JValue], tag: ClassTag[T]): Unit =
    addAccessor(tag.runtimeClass, acc)

  def addSingleton[T](obj: T)(implicit tag: ClassTag[T]): Unit = {
    val acc = JSONAccessor.create[T, JValue]({x: T => JNull}, {x: JValue => obj})

    addAccessor(tag.runtimeClass, acc)
  }

  def accessors = _accessors

  def accessor(clz: Class[_]): Option[JSONAccessorProducer[_, JValue]] = {
    accessors get clz match {
      case None =>
        val head = accessors.filter(_._1.isAssignableFrom(clz)).headOption

        head map { case (_, acc) =>
          addAccessor(clz, acc)
          acc
        }
      case x => x
    }
  }

  /**
   * Accessor that enables use of `Any` via this registry by creating a pickle
   */
  implicit val anyAccessor = ObjectAccessor.create[Any]({ x =>
    //create the pickle
    val clz = x.getClass
    val acc = accessor(clz).map(_.asInstanceOf[JSONAccessorProducer[Any, JValue]])

    acc match {
      case Some(acc) =>
        val jv = acc.createJSON(x)

        Pickle(jv, clz.getName).js
      case None => sys.error(s"Unknown accessor for $x of class $clz")
    }
  }, { jv =>
    val pickle = jv.toObject[Pickle]
    val clz = classNameMap.getOrElse(pickle.className, sys.error("Unregistered class " + pickle.className))
    val acc = accessor(clz)

    acc.get.fromJSON(pickle.data)
  })
}

