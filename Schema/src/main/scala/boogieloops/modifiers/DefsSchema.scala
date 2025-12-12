package boogieloops.schema.modifiers

import boogieloops.schema.Schema
import boogieloops.schema.validation.{ValidationResult, ValidationContext}

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
 * 1. Wrapper mode: Add $defs to an existing schema (underlying is Some)
 * 2. Standalone mode: Create a schema containing only $defs (underlying is None)
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
case class DefsSchema(
    underlying: Option[Schema],
    defsValue: Map[String, Schema]
) extends Schema {

  override def $defs: Option[Map[String, Schema]] = Some(defsValue)

  // Delegate all other core vocabulary to underlying schema (if present)
  override def $schema: Option[String] = underlying.flatMap(_.$schema)
  override def $id: Option[String] = underlying.flatMap(_.$id)
  override def $ref: Option[String] = underlying.flatMap(_.$ref)
  override def $dynamicRef: Option[String] = underlying.flatMap(_.$dynamicRef)
  override def $dynamicAnchor: Option[String] = underlying.flatMap(_.$dynamicAnchor)
  override def $vocabulary: Option[Map[String, Boolean]] = underlying.flatMap(_.$vocabulary)
  override def $comment: Option[String] = underlying.flatMap(_.$comment)

  // Delegate all metadata to underlying schema (if present)
  override def title: Option[String] = underlying.flatMap(_.title)
  override def description: Option[String] = underlying.flatMap(_.description)
  override def default: Option[ujson.Value] = underlying.flatMap(_.default)
  override def examples: Option[List[ujson.Value]] = underlying.flatMap(_.examples)
  override def readOnly: Option[Boolean] = underlying.flatMap(_.readOnly)
  override def writeOnly: Option[Boolean] = underlying.flatMap(_.writeOnly)
  override def deprecated: Option[Boolean] = underlying.flatMap(_.deprecated)

  override def toJsonSchema: ujson.Value = {
    val base = underlying match {
      case Some(u) => u.toJsonSchema
      case None =>
        // Standalone mode: create a new schema with only $defs
        val schema = ujson.Obj()
        // Add meta-data keywords if present (from standalone usage)
        title.foreach(t => schema("title") = ujson.Str(t))
        description.foreach(d => schema("description") = ujson.Str(d))
        schema
    }

    if (defsValue.nonEmpty) {
      val defsObj = ujson.Obj()
      defsValue.foreach { case (name, schema) =>
        defsObj(name) = schema.toJsonSchema
      }
      base("$defs") = defsObj
    }
    base
  }

  // Override modifier methods to maintain chaining
  override def optional: Schema = boogieloops.schema.OptionalSchema(this)
  override def nullable: Schema = boogieloops.schema.NullableSchema(this)
  override def withDefault(value: ujson.Value): Schema = boogieloops.schema.DefaultSchema(this, value)

  // Override with* methods to preserve $defs while adding other metadata
  override def withTitle(title: String): Schema = TitleSchema(this, title)
  override def withDescription(desc: String): Schema = DescriptionSchema(this, desc)
  override def withSchema(schema: String): Schema = SchemaModifier(this, schema)
  override def withId(id: String): Schema = IdSchema(this, id)

  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    underlying match {
      case Some(u) =>
        // If there's an underlying schema, delegate validation to it
        u.validate(value, context)
      case None =>
        // $defs itself doesn't validate anything - it just provides definitions
        // The actual validation happens when the definitions are referenced
        ValidationResult.valid()
    }
  }

  /**
   * Get a definition by name
   */
  def getDefinition(name: String): Option[Schema] = {
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
