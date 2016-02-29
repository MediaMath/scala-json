Migration
=========

The MigratingObjectAccessor can wrap a standard ObjectAccessor and provide migration functionality for JSON documents.
By specifying an Ordered version field and a sequence of Migrations, JSON documents will automatically be migrated through
the sequence of Migrations (sorted by version) before being marshaled into the final Scala object.


```scala
scala> import json._
import json._

scala> import json.tools.{MigratingObjectAccessor,Migration}
import json.tools.{MigratingObjectAccessor, Migration}

scala>   case class TestModel(version: Int, data: String)
defined class TestModel

scala>   val testModelMigrations: Seq[Migration[Int]] = Seq(
     |     Migration(1) { jObject =>
     |       jObject.get("oldData") match {
     |         case Some(data) => jObject + ("data" -> data) - "oldData"
     |         case _          => jObject
     |       }
     |     }
     |   )
testModelMigrations: Seq[json.tools.Migration[Int]] = List(json.tools.Migration$$anon$1@57e5e9f3)

scala>   implicit val testModelAcc: ObjectAccessor[TestModel] = new MigratingObjectAccessor(testModelMigrations, "version", ObjectAccessor.create[TestModel])
testModelAcc: json.ObjectAccessor[TestModel] = json.tools.MigratingObjectAccessor@7e195aa

scala>   val needsMigration = """{"oldData": "Data","version": 0}"""
needsMigration: String = {"oldData": "Data","version": 0}

scala>   JValue.fromString(needsMigration).toObject[TestModel]
res0: TestModel = TestModel(1,Data)
```

