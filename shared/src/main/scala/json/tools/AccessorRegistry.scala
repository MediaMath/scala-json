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

import scala.reflect.ClassTag

/**
 * This is the actual object that is serialized to JSON to represent the 'pickled' data.
 * @param data - The generic 'pickled' data, to be used by the accessor
 *             that is looked up via the registry.
 * @param className - The class name that is used to look up the accessor via the registry.
 */
case class Pickle(data: JValue, className: String)
object Pickle {
  implicit val acc = JSONAccessor.create[Pickle, JObject]({ x =>
    JObject("data" -> x.data, "class" -> x.className.js)
  },{ obj =>
    Pickle(obj("data"), obj("class").jString.str)
  })
}

/**
 * Global singleton implementation of the registry.
 */
object AccessorRegistry extends AccessorRegistry

/**
 * Accessor registry is used to create or resolve a [[Pickle]] by using a registry
 * of run-time classes associated to their respective accessor. These must be registered
 * near application startup before use.
 *
 * The registry is roughly thread-safe, all operations are idempotent so consistent
 * state is observed by all users.
 */
trait AccessorRegistry {
  @volatile
  private var _accessors = Map[Class[_], JSONAccessorProducer[_, JValue]]()

  @volatile
  private var classNameMap = Map[String, Class[_]]()

  def add(newAccs: JSONAccessorProducer[_, JValue]*): Unit = newAccs.foreach { acc =>
    add(acc.clazz, acc)
  }

  def add(clz: Class[_], acc: JSONAccessorProducer[_, JValue]): Unit = synchronized {
    _accessors += (clz -> acc)
    classNameMap += clz.getName -> clz
  }

  def add[T <: AnyRef](implicit acc: JSONAccessorProducer[T, JValue], tag: ClassTag[T]): Unit =
    add(tag.runtimeClass, acc)

  def add[T](obj: T)(implicit tag: ClassTag[T]): Unit = {
    val acc = JSONAccessor.create[T, JValue]({x: T => JNull}, {x: JValue => obj})

    add(tag.runtimeClass, acc)
  }

  def add(registry: AccessorRegistry): Unit = {
    registry.accessors.foreach {
      case (clz, acc) => add(clz, acc)
    }
  }

  def accessors = _accessors

  def accessor(clz: Class[_]): Option[JSONAccessorProducer[_, JValue]] = {
    accessors get clz match {
      case None =>
        val head = accessors.filter(_._1.isAssignableFrom(clz)).headOption

        head match {
          case None => None
          case Some((_, acc)) =>
            add(clz, acc)
            Some(acc)
        }
      case x => x
    }
  }

  /**
   * Accessor that enables use of `Any` via this registry by creating a pickle
   */
  implicit val anyAccessor = json.createAccessor[Any]({ x =>
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

