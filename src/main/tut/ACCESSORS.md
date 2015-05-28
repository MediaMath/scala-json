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

Scala macros are used to create accessors for case classes automatically.
It's best to put these under a val or lazy val in a static scope. They can
be used on demand but it can cause serious code bloat as the macro code
is inlined per usage.
```tut
import json._

case class TestClass(a: Int)
implicit val testClassAcc = ObjectAccessor.of[TestClass]
```

Custom types
```tut
class Foo(val bar: String)
val fooAccessor = JSONAccessor.create(
      { x: Foo =>
        JString(x.bar.toString)
      },
      { x: JValue =>
        new Foo(x.jString.str)
      }
    )
```

Sub-classes
```tut
class Bar(s: String) extends Foo(s)
``

Putting the implicit under a companion object of the same name provides
the implicit in any scope where the type is imported.
```scala
object TestClass {
  implicit val acc = ObjectAccessor.of[TestClass]
}
```

Proxying between types can be tricky, but its verbose nature allows you to implement
whatever sub-classing format you want for your JSON serialization.