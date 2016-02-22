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
  * Produces JValue
  * Produces T from JValue
* ```JSONAccessorProducer[T, +JV <: JValue] extends JSONAccessorProducer.CreateJSON[T, JV]```
  * Produces specific subclass of JValue from T
  * Produces T from JValue
  * Useful in places where JValue type must be explicit (such as the key in a Map needing to be a JString)
* ```JSONAccessorProducer.CreateJSON[-T, +JV <: JValue]```
  * Produces specific subclass of JValue from T and any subclass of T

Accessors chain together implicitly to build the compile-time procedure used for marshalling JSON. These chains
can be very deep and extend across several types of accessors.

```tut
import json._

val complexValue = Map("key" -> Seq.fill(3)(Set(Some(false), None)))

complexValue.js

accessorFor(complexValue).describe
```

The one partial exception to this is the treatment of Option. Normally
defaults of a case class are used if there is either a null or undefined present.
Option treats null and undefined differently. When parsing if an Option field is null, None
is used regardless of the default. If the field is undefined, the default is used
or else it resolves to null. This gives Option fields the unique property
of resolving even if undefined is present. 

Scala macros are used to create accessors for case classes automatically.
It's best to put these under a val or lazy val in a static scope. They could
be used dynamically but it can cause serious code bloat as the macro code
is inlined per usage.

```tut

case class TestClass(a: Int, b: String = "foo", c: Map[String, Set[Boolean]])
ObjectAccessor.create[TestClass].describe

```

Custom types
```tut

class Foo(val bar: String)
val fooAccessor = JSONAccessor.create[Foo, JString](
      { x: Foo =>
        x.bar.js
      },
      {
        case JString(str) => new Foo(str)
        case x => sys.error("Cannot parse" + x)
      }
    )

```

Sub-classes
```tut

class Bar(s: String) extends Foo(s)

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

@json.accessor case class TestClass(a: Int)

```

Proxying between types can be tricky, but its verbose nature allows you to implement
whatever sub-classing format you want for your JSON serialization using custom accessor
implementations.