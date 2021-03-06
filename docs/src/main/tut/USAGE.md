Usage and Examples
------------------

The intermediate type used by scala-json is the JValue. All intermediate JSON values extend [JValue](http://mediamath.github.io/scala-json/doc/index.html#json.JValue).
JValue types can be serialized to a JSON string by just using [JValue#toString](http://mediamath.github.io/scala-json/doc/index.html#json.JValue@toString(settings:json.JSONBuilderSettings,lvl:Int):String).
[JValue.fromString](http://mediamath.github.io/scala-json/doc/index.html#json.JValue$@fromString(str:String):json.JValue)
is used to create a JValue from a JSON string.

### Basic JValue types ###

* Import the json package
```tut
import json._
JValue fromString "[1,2,3,4,5]" //parse JSON
JArray(JNull, JTrue) //create JSON
```
* Implicit conversion to JValue types using built-in accessors for base scala types
```tut
"hello".js
true.js
1.7.js
def testMap = Map("hey" -> "there")
val testMapJs = testMap.js
Map("key" -> Seq.fill(3)(Set(Some(false), None))).js //even complex types
testMap.keySet.headOption.js
testMap.get("nokey").js
testMap.js.toDenseString
```
* JS-like select
```tut
require(testMapJs("nokey") == JUndefined)
```
* JS-like boolean conditions
```tut
if(testMapJs("nokey")) sys.error("unexpected")
```
* JArrays as scala collections
```tut
JArray(1, 2, 3, 4).map(_.toJString)
JArray(1, 2, 3, 4).map(_.num) //drops down to Seq when working with non JValue types
JArray(1, 2, 3, 4) ++ JArray(5)
JArray(1, 2, 3, 4) ++ Seq(JNumber(5)) //can append any Iterable[JValue]
JArray(JObject.empty, JArray.empty) ++ Seq("nonjval") //adding a non JValue results in a normal Seq
```
* JObjects as scala collections
```tut
JObject("foo" -> 1.js, "a" -> false.js) ++ Map("bar" -> true).js - "a" //extends MapLike
```

### Case Class Usage ###

* Compile-time case class marshalling
```tut
case class TestClass(@name("FIELD_A") a: Int, b: Option[Int], c: String = "", d: Option[Int] = None) {
    @ephemeral def aString = a.toString //ephemeral fields get written but never read
}
implicit val acc = ObjectAccessor.create[TestClass] //macro expands here to create the accessor
val testClassJs = TestClass(1, None).js //implicit accessor allows us to use '.js' here to produce a JValue
val testClassJsString = testClassJs.toDenseString
JValue.fromString(testClassJsString).toObject[TestClass] //go from JSON string directly to object
```
* JSON field annotations
```tut
require(testClassJs("aString") != JUndefined) //ephemeral field exists
JObject("FIELD_A" -> 23.js).toObject[TestClass] //using FIELD_A as renamed via @name annotation
```
* Streamlined compile-time case class marshalling (requires [macro-paradise](#dependencies))
```tut
@accessor case class SomeModel(a: String, other: Int)
accessorOf[SomeModel] //accessor available in scope via hidden implicit
SomeModel("foo", 22).js
```
* Typed exceptions with field data

```tut
import json.exceptions._

try {
  JObject("FIELD_A" -> "badint".js).toObject[TestClass]
  sys.error("should fail before")
} catch {
  case e: InputFormatException =>
    //returns all field exceptions for a parse, not just the first one!
    e.getExceptions.map {
      //detailed exception classes very useful for form validation
      case fieldEx: InputFieldException if fieldEx.fieldName == "FIELD_A" =>
        fieldEx.getMessage
      case x => sys.error("unexpected error " + x)
    }.mkString
}
```

### Dynamic field access ###

This allows 'dynamic member' access. Generally not needed but can be useful if doing lots of operations
on the intermediary untyped JValues and provides a syntax very similar to JS.

```tut
val seqJson = Seq(TestClass(1, None), TestClass(1, Some(10), c = "hihi")).js
seqJson.dynamic(1).c.value
seqJson.dynamic.length
require(seqJson.d == seqJson.dynamic)
```
