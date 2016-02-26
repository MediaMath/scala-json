package json.tools

import json._
import json.internal.FieldAccessor

trait Migration {
  def version: Int
  def procedure(jObject: JObject): JObject

  def apply(proc: (JObject) => JObject): Migration = {

    new Migration {
      override def procedure(jObject: JObject): JObject = {

        proc(Migration.this.procedure(jObject))
      }
      override def version: Int = Migration.this.version
    }
  }

  def moveFromParentToChild(child: String,
                            field: String,
                            newField: String) = this{ jObject =>

    jObject.get(field).map { fieldData =>

      val updatedChild = jObject.get(child)
        .map(_.jObject)
        .getOrElse(JObject()) + (newField -> fieldData)

      jObject + (child -> updatedChild)
    }.getOrElse(jObject)
  }

  def moveFromChildToParent(child: String,
                            field: String,
                            newField: String) = this{ jObject =>

    val updatedRecord = for {

      childObj <- jObject.get(child).map(_.jObject)
      fieldData <- childObj.get(field)
    } yield jObject + (newField -> fieldData)

    updatedRecord.getOrElse(jObject)
  }

  def moveFromChildToChild(childSrc: String,
                           childDest: String,
                           field: String,
                           newField: String) = this{ jObject =>

    val updatedRecord = for {
      childSrcObj <- jObject.get(childSrc).map(_.jObject)
      fieldData <- childSrcObj.get(field)
      updatedDestChild = jObject.get(childDest)
        .map(_.jObject)
        .getOrElse(JObject()) + (newField -> fieldData)

    } yield jObject + (childDest -> updatedDestChild)

    updatedRecord.getOrElse(jObject)
  }

  def transformField(field: String, newField: String)(proc: (JValue) => JValue = { jValue => jValue }) = this{ jObject =>

    jObject.get(field) match {

      case Some(data) => jObject + (newField -> proc(data))
      case _          => jObject
    }
  }

  def renameField(field: String, newField: String) = transformField(field, newField)()

  def removeFieldFromChild(child: String, field: String) = transformField(child, child) {
    _.jObject.filter {
      case (`field`, _) => false
      case _                     => true
    }
  }

  def removeFieldFromPath(path: List[String], field: String): Migration = {
    def recur(current: JObject, remaining: List[String]): JObject = remaining match {
      case h :: t => current.get(h) match {
        case Some(child) => current + (h -> recur(child.jObject, t))
        case None        => current
      }
      case Nil => current - field
    }

    this{ recur(_, path) }
  }
}

object Migration {

  def apply(ver: Int): Migration = {

    new Migration {

      override def procedure(jObject: JObject): JObject = jObject
      override def version: Int = ver
    }
  }
}

class MigratingObjectAccessor[T](migrations: Seq[Migration],
                                 versionField: String,
                                 innerAccessor: ObjectAccessor[T]) extends ObjectAccessor[T] {

  override def fromJSON(js: JValue): T = {
    val version: Option[Int] = js.jObject.get(versionField) match {
      case Some(JNumber(n)) => Some(n.toInt)
      case _                => None
    }
    val migrationsToRun = migrations
      .filter(migration => !version.exists(ver => ver >= migration.version))
      .sortBy(_.version)

    val migratedJSON: JObject = migrationsToRun.foldLeft(js.jObject) { (jObject, migration) =>
      migration.procedure(jObject) + (versionField -> JNumber(migration.version))
    }

    innerAccessor.fromJSON(migratedJSON)
  }

  override def fields: IndexedSeq[FieldAccessor[T]] = innerAccessor.fields
  override def describe: JValue = innerAccessor.describe
  override def clazz: Class[_] = innerAccessor.clazz
  override def createJSON(obj: T): JObject = innerAccessor.createJSON(obj)
}