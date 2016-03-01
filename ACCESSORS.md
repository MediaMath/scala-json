Accessors
=========

Accessors are the implicit factory objects available that handle
the marshalling of a specific type to a JValue type. These accessors
are linked together at compile to assemble marshalling logic instead
of relying on runtime logic like some marshallers.

The default accessors provided are ones that make literal sense when
translated to JSON. One of the main goals of this library was to produce
sensible and normal looking JSON serializations of internal structures.
Things like dates, times and subclassing are things that vary greatly
between JSON APIs and are left for you to handle. Things like Set, Seq, Map
have a very literal definition and refer to the accessor of the value type
for per-item marshalling. These are provided for you out of the box.

Accessors come in 3 types:

* ```JSONAccessor[T] = JSONAccessorProducer[T, JValue]```
  * Main accessor type used
  * Produces JValue from T
  * Produces T from JValue
* ```JSONAccessorProducer[T, +JV <: JValue] extends JSONAccessorProducer.CreateJSON[T, JV]```
  * Produces specific type of JValue (JV) from T
  * Produces T from JValue
  * Useful in places where JValue type must be explicit (such as the key in a Map needing to be a JString)
  * Allows the implicit [Any#js](http://mediamath.github.io/scala-json/doc/index.html#json.Implicits$AnyValJSEx@js[U<:json.JValue](implicitacc:json.JSONAccessorProducer.CreateJSON[T,U]):U)
    method to produce more specific types than just JValue
* ```JSONAccessorProducer.CreateJSON[-T, +JV <: JValue]```
  * Produces specific type of JValue (JV) from T
  * Generally not used directly, exists just to provide contravariant resolution for type T of a non-variant JSONAccessor
  * Allows the implicit [Any#js](http://mediamath.github.io/scala-json/doc/index.html#json.Implicits$AnyValJSEx@js[U<:json.JValue](implicitacc:json.JSONAccessorProducer.CreateJSON[T,U]):U)
    to use accessors for a superclass to produce JValues. This only works in a few places, generally accessors should be
    for a non-varied type T. Example of why full variance is not provided: The accessor for type Option[T] is capable of producing a JValue
    from Some[T] and None. When parsing JSON it is capable of producing Some[T] or None. It cannot however be used to produce
    a value of JUST Some[T]. Therefor to use this accessor in a full accessor chain, the field must be typed as Option[T]. However,
    Some("blah").js will work just fine as the method only needs a 1-way contravariant create-only accessor and can use just the CreateJSON
    part of the Option[T] accessor.

Accessors chain together implicitly to build the compile-time procedure used for marshalling JSON. These chains
can be very deep and extend across several types of accessors.

```scala
scala> import json._
import json._

scala> val complexValue = Map("key" -> Seq.fill(3)(Set(Some((1, false, "")), None)))
complexValue: scala.collection.immutable.Map[String,Seq[scala.collection.immutable.Set[Option[(Int, Boolean, String)]]]] = Map(key -> List(Set(Some((1,false,)), None), Set(Some((1,false,)), None), Set(Some((1,false,)), None)))

scala> complexValue.js
res0: json.JObject =
{
  "key": [[[1, false, ""], null], [[1, false, ""], null], [[1, false, ""], null]]
}

scala> accessorFor(complexValue).describe //JSON pretty formatted description of accessor
res1: json.JValue =
{
  "accessor": "MapAccessor",
  "types": [{
    "accessor": "StringAccessor"
  }, {
    "accessor": "IterableAccessor",
    "types": [{
      "accessor": "IterableAccessor",
      "types": [{
        "accessor": "OptionAccessor",
        "types": [{
          "accessor": "Tuple3Accessor",
          "types": [{
            "accessor": "IntAccessor"
          }, {
            "accessor": "BooleanAccessor"
          }, {
            "accessor": "StringAccessor"
          }]
        }]
      }]
    }]
  }]
}

scala> //
```

Option is treated in a special way in scala-json. Normally field
defaults of a case class field are used if there is either a null or undefined present (non-existent fields ~~ undefined).
Option treats null and undefined differently. When parsing, if an Option field is null, None
is used regardless of the default. If the field is undefined, the default is used
or else it resolves to null.

|                   	| Option[T] 	| Option[T] w/ default 	| other                	| other w/ default 	|
|-------------------	|-----------	|----------------------	|----------------------	|------------------	|
| **JNull**         	| None      	| None                 	| InputFormatException 	| default          	|
| **JUndefined**    	| None      	| default              	| InputFormatException 	| default          	|
| **Useful JValue** 	| Some[T]   	| Some[T]              	| x: T                 	| x: T             	|

Scala macros are used to create accessors for case classes automatically.
It's best to put these under a val or lazy val in a static scope. They could
be used dynamically but it can cause serious code bloat as the macro code
is inlined per usage.

```scala
     | case class TestClass(a: Int, b: String = "foo", c: Map[String, Set[Boolean]])
defined class TestClass

scala> ObjectAccessor.create[TestClass].describe
res3: json.JValue =
{
  "accessor": "CaseClassObjectAccessor",
  "types": [{
    "accessor": "IntAccessor"
  }, {
    "accessor": "StringAccessor"
  }, {
    "accessor": "MapAccessor",
    "types": [{
      "accessor": "StringAccessor"
    }, {
      "accessor": "IterableAccessor",
      "types": [{
        "accessor": "BooleanAccessor"
      }]
    }]
  }]
}

scala> //
```

Custom types
```scala
     | class Foo(val bar: String)
defined class Foo

scala> val fooAccessor = JSONAccessor.create[Foo, JString](
     |       { x: Foo =>
     |         x.bar.js
     |       },
     |       {
     |         case JString(str) => new Foo(str)
     |         case x => sys.error("Cannot parse" + x)
     |       }
     |     )
fooAccessor: json.JSONAccessorProducer[Foo,json.JString] = JSONAccessor.create
```


Putting the implicit under a companion object of the same name provides
the implicit in any scope.

```scala

object TestClass {
  implicit val acc = ObjectAccessor.create[TestClass]
}
```

This boilerplate can be reduced by using the convenient @json.accessor annotation.
This annotation will basically apply the above boilerplate for you, creating a companion
object if one does not exist or just adding the implicit val to the existing object.
This @accessor annotation is beyond the scope of normal scala macros
and requires [macro-paradise](http://docs.scala-lang.org/overviews/macros/paradise.html) 2.1.0+
to gain the advanced functionality needed. The remaining functionality (ObjectAccessor.create, etc)
will work without macro-paradise, it is only required for the optional @accessor annotation.

```scala

@accessor case class TestClass(a: Int)

```

