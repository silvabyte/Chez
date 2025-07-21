package chez.modifiers

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
import upickle.default.*

/**
 * JSON Schema $defs keyword implementation
 *
 * The $defs keyword provides a location for reusable schema definitions within a schema document.
 * These definitions can be referenced using $ref with a JSON Pointer to the definition.
 *
 * According to JSON Schema 2020-12, $defs is the standardized way to define reusable schemas
 * (replacing the draft-07 "definitions" keyword).
 *
 * This class supports two modes:
 * 1. Wrapper mode: Add $defs to an existing schema (underlying != null)
 * 2. Standalone mode: Create a schema containing only $defs (underlying == null)
 *
 * Example:
 * {
 *   "$defs": {
 *     "User": {
 *       "type": "object",
 *       "properties": {
 *         "name": { "type": "string" }
 *       }
 *     }
 *   },
 *   "type": "object",
 *   "properties": {
 *     "user": { "$ref": "#/$defs/User" }
 *   }
 * }
 */
case class DefsChez[T <: Chez](
    underlying: T,
    defsValue: Map[String, Chez]
) extends Chez {

  override def $defs: Option[Map[String, Chez]] = Some(defsValue)

  // Delegate all other core vocabulary to underlying schema (if present)
  override def $schema: Option[String] = Option(underlying).flatMap(_.$schema)
  override def $id: Option[String] = Option(underlying).flatMap(_.$id)
  override def $ref: Option[String] = Option(underlying).flatMap(_.$ref)
  override def $dynamicRef: Option[String] = Option(underlying).flatMap(_.$dynamicRef)
  override def $dynamicAnchor: Option[String] = Option(underlying).flatMap(_.$dynamicAnchor)
  override def $vocabulary: Option[Map[String, Boolean]] = Option(underlying).flatMap(_.$vocabulary)
  override def $comment: Option[String] = Option(underlying).flatMap(_.$comment)

  // Delegate all metadata to underlying schema (if present)
  override def title: Option[String] = Option(underlying).flatMap(_.title)
  override def description: Option[String] = Option(underlying).flatMap(_.description)
  override def default: Option[ujson.Value] = Option(underlying).flatMap(_.default)
  override def examples: Option[List[ujson.Value]] = Option(underlying).flatMap(_.examples)
  override def readOnly: Option[Boolean] = Option(underlying).flatMap(_.readOnly)
  override def writeOnly: Option[Boolean] = Option(underlying).flatMap(_.writeOnly)
  override def deprecated: Option[Boolean] = Option(underlying).flatMap(_.deprecated)

  override def toJsonSchema: ujson.Value = {
    val base = if (underlying != null) {
      underlying.toJsonSchema
    } else {
      // Standalone mode: create a new schema with only $defs
      val schema = ujson.Obj()
      // Add meta-data keywords if present (from standalone usage)
      title.foreach(t => schema("title") = ujson.Str(t))
      description.foreach(d => schema("description") = ujson.Str(d))
      schema
    }

    if (defsValue.nonEmpty) {
      val defsObj = ujson.Obj()
      defsValue.foreach { case (name, chez) =>
        defsObj(name) = chez.toJsonSchema
      }
      base("$defs") = defsObj
    }
    base
  }

  // Override modifier methods to maintain chaining
  override def optional: Chez = chez.OptionalChez(this)
  override def nullable: Chez = chez.NullableChez(this)
  override def withDefault(value: ujson.Value): Chez = chez.DefaultChez(this, value)

  // Override with* methods to preserve $defs while adding other metadata
  override def withTitle(title: String): Chez = TitleChez(this, title)
  override def withDescription(desc: String): Chez = DescriptionChez(this, desc)
  override def withSchema(schema: String): Chez = SchemaChez(this, schema)
  override def withId(id: String): Chez = IdChez(this, id)

  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    if (underlying != null) {
      // If there's an underlying schema, delegate validation to it
      underlying.validate(value, context)
    } else {
      // $defs itself doesn't validate anything - it just provides definitions
      // The actual validation happens when the definitions are referenced
      ValidationResult.valid()
    }
  }

  /**
   * Get a definition by name
   */
  def getDefinition(name: String): Option[Chez] = {
    defsValue.get(name)
  }

  /**
   * Check if a definition exists
   */
  def hasDefinition(name: String): Boolean = {
    defsValue.contains(name)
  }

  /**
   * Get all definition names
   */
  def definitionNames: Set[String] = {
    defsValue.keySet
  }
}
