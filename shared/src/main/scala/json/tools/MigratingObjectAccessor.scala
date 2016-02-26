package json.tools

import json._
import json.internal.FieldAccessor

/**
 * Represents a single migration to a specified version
 *
 * @tparam T The version type
 */
trait Migration[T] {

  /**
   *
   * @return The migration version
   */
  def version: T

  /**
   * The function that migrates the original JSON object
   * @param jObject The original JSON object to be migrated
   * @return The migrated JSON object
   */
  def procedure(jObject: JObject): JObject

  /**
   * Used to chain Migration procedures/steps.
   * @param proc A Migration procedure that will further manipulate the JObject returned
   *             by this Migration's procedure.
   * @return The new Migration
   */
  def apply(proc: (JObject) => JObject): Migration[T] = {

    new Migration[T] {
      override def procedure(jObject: JObject): JObject = {

        proc(Migration.this.procedure(jObject))
      }
      override def version: T = Migration.this.version
    }
  }

  /**
   * Adds to this Migration, a step that moves a field from the parent JSON object to a child JSON object
   * and renames it.
   * @param child The child name
   * @param field The original field name
   * @param newField The new field name
   * @return The new Migration
   */
  def moveFromParentToChild(child: String,
                            field: String,
                            newField: String): Migration[T] = this{ jObject =>

    jObject.get(field).map { fieldData =>

      val updatedChild = jObject.get(child)
        .map(_.jObject)
        .getOrElse(JObject()) + (newField -> fieldData)

      jObject + (child -> updatedChild)
    }.getOrElse(jObject)
  }

  /**
   * Adds to this Migration, a step that moves a field from a child JSON object to the parent JSON object
   * and renames it.
   * @param child The child name
   * @param field The original field name
   * @param newField The new field name
   * @return The new Migration
   */
  def moveFromChildToParent(child: String,
                            field: String,
                            newField: String): Migration[T] = this{ jObject =>

    val updatedRecord = for {

      childObj <- jObject.get(child).map(_.jObject)
      fieldData <- childObj.get(field)
    } yield jObject + (newField -> fieldData)

    updatedRecord.getOrElse(jObject)
  }

  /**
   * Adds to this Migration, a step that moves a field from a child JSON object to another child JSON object
   * and renames it.
   * @param childSrc The source child name
   * @param childDest The destination child name
   * @param field The original field name
   * @param newField The new field name
   * @return The new Migration
   */
  def moveFromChildToChild(childSrc: String,
                           childDest: String,
                           field: String,
                           newField: String): Migration[T] = this{ jObject =>

    val updatedRecord = for {
      childSrcObj <- jObject.get(childSrc).map(_.jObject)
      fieldData <- childSrcObj.get(field)
      updatedDestChild = jObject.get(childDest)
        .map(_.jObject)
        .getOrElse(JObject()) + (newField -> fieldData)

    } yield jObject + (childDest -> updatedDestChild)

    updatedRecord.getOrElse(jObject)
  }

  /**
   * Adds to this Migration, a step that performs arbitrary manipulations of a field and renames it.
   * @param field The field name
   * @param newField The new field name
   * @param proc The procedure to perform
   * @return The new Migration
   */
  def transformField(field: String, newField: String)(proc: (JValue) => JValue = { jValue => jValue }): Migration[T] =
    this{ jObject =>

      jObject.get(field) match {

        case Some(data) => jObject + (newField -> proc(data))
        case _          => jObject
      }
  }

  /**
   * Adds to this Migration, a step that renames a field.
   * @param field The field name
   * @param newField The new field name
   * @return The new Migration
   */
  def renameField(field: String, newField: String): Migration[T] = transformField(field, newField)()

  /**
   * Adds to this Migration, a step that removes a field from a child field.
   * @param child The child field
   * @param field The field to be removed from the child field
   * @return The new Migration
   */
  def removeFieldFromChild(child: String, field: String): Migration[T] = transformField(child, child) {
    _.jObject.filter {
      case (`field`, _) => false
      case _                     => true
    }
  }

  /**
   * Adds to this Migration, a step that removes a field from a specified path.
   * @param path The path to the field
   * @param field The name of the field to be removed
   * @return The new Migration
   */
  def removeFieldFromPath(path: List[String], field: String): Migration[T] = {
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

  /**
   * Creates an empty migration with a version number
   * @param ver The version number
   * @return A Migration
   */
  def apply[T](ver: T): Migration[T] = {

    new Migration[T] {

      override def procedure(jObject: JObject): JObject = jObject
      override def version: T = ver
    }
  }
}

/**
 * [[ObjectAccessor]] that automatically migrates JSON objects using a sequence of [[Migration]]s in order of their version numbers.
 * @param migrations Seq of [[Migration]]s to perform.
 * @param versionField The field name that is used in checking the version of the object.
 * @param innerAccessor The [[ObjectAccessor]] to use in serializing/de-serializing
 * @param verAcc The implicit [[JSONAccessor]] for the version field type
 * @tparam T The base type this migrating accessor is for.
 * @tparam U The version field type
 */
class MigratingObjectAccessor[T,U <% Ordered[U]](migrations: Seq[Migration[U]],
                                 versionField: String,
                                 innerAccessor: ObjectAccessor[T])(implicit verAcc: JSONAccessor[U]) extends ObjectAccessor[T] {

  override def fromJSON(js: JValue): T = {

    val version: Option[U] = js.jObject.get(versionField) match {
      case Some(jv) => Some(jv.to[U])
      case _                => None
    }

    val migrationsToRun = migrations
      .filter(migration => !version.exists(ver => ver >= migration.version))
      .sortBy(_.version)

    val migratedJSON: JObject = migrationsToRun.foldLeft(js.jObject) { (jObject, migration) =>
      migration.procedure(jObject) + (versionField -> migration.version.js)
    }

    innerAccessor.fromJSON(migratedJSON)
  }

  override def fields: IndexedSeq[FieldAccessor[T]] = innerAccessor.fields
  override def describe: JValue = innerAccessor.describe
  override def clazz: Class[_] = innerAccessor.clazz
  override def createJSON(obj: T): JObject = innerAccessor.createJSON(obj)
}