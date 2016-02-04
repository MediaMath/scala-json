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

```tut
import json._

Map("key" -> Seq.fill(3)(Set(Some(false), None))).js
```

The one partial exception to this is the treatment of Option. Normally
defaults of a case class are used if there is either a null or undefined present.
Option treats null and undefined differently. When parsing if an Option field is null, None
is used regardless of the default. If the field is undefined, the default is used
or else it resolves to null. This gives Option fields the unique property
of resolving even if undefined is present. 

Scala macros are used to create accessors for case classes automatically.
It's best to put these under a val or lazy val in a static scope. They can
be used dynamically but it can cause serious code bloat as the macro code
is inlined per usage.

```tut
case class TestClass(a: Int)
implicit val testClassAcc = ObjectAccessor.of[TestClass]
```

Custom types
```tut
class Foo(val bar: String)
val fooAccessor = JSONAccessor.create[Foo, JValue](
      { x: Foo =>
        x.bar.js
      },
      { x: JValue =>
        new Foo(x.jString.str)
      }
    )
```

Sub-classes
```tut
class Bar(s: String) extends Foo(s)
```

Putting the implicit under a companion object of the same name provides
the implicit in any scope where the type is imported.
```scala
object TestClass {
  implicit val acc = ObjectAccessor.of[TestClass]
}
```

Proxying between types can be tricky, but its verbose nature allows you to implement
whatever sub-classing format you want for your JSON serialization.