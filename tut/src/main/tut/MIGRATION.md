Migration
=========

The MigratingObjectAccessor can wrap a standard ObjectAccessor and provide migration functionality for JSON documents.
By specifying an Ordered version field and a sequence of Migrations, JSON documents will automatically be migrated through
the sequence of Migrations (sorted by version) before being marshaled into the final Scala object.


```tut
import json._
import json.tools.{MigratingObjectAccessor,Migration}

  case class TestModel(version: Int, data: String)

  val testModelMigrations: Seq[Migration[Int]] = Seq(

    Migration(1) { jObject =>

      jObject.get("oldData") match {

        case Some(data) => jObject + ("data" -> data) - "oldData"
        case _          => jObject
      }
    }
  )
  
  implicit val testModelAcc: ObjectAccessor[TestModel] = new MigratingObjectAccessor(testModelMigrations, "version", ObjectAccessor.create[TestModel])

  val needsMigration = """{"oldData": "Data","version": 0}"""
  
  JValue.fromString(needsMigration).toObject[TestModel]
  

```

