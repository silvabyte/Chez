package boogieloops.schema.modifiers

import boogieloops.schema.Schema
import boogieloops.schema.validation.{ValidationResult, ValidationContext}

/**
 * Wrapper for adding $schema metadata to a schema
 *
 * According to JSON Schema 2020-12, $schema is used to declare which version
 * of the JSON Schema specification the schema is written against.
 */
case class SchemaModifier[T <: Schema](
    underlying: T,
    schemaValue: String
) extends Schema {

  override def $schema: Option[String] = Some(schemaValue)

  // Delegate all other core vocabulary to underlying schema
  override def $id: Option[String] = underlying.$id
  override def $ref: Option[String] = underlying.$ref
  override def $defs: Option[Map[String, Schema]] = underlying.$defs
  override def $dynamicRef: Option[String] = underlying.$dynamicRef
  override def $dynamicAnchor: Option[String] = underlying.$dynamicAnchor
  override def $vocabulary: Option[Map[String, Boolean]] = underlying.$vocabulary
  override def $comment: Option[String] = underlying.$comment

  // Delegate all metadata to underlying schema
  override def title: Option[String] = underlying.title
  override def description: Option[String] = underlying.description
  override def default: Option[ujson.Value] = underlying.default
  override def examples: Option[List[ujson.Value]] = underlying.examples
  override def readOnly: Option[Boolean] = underlying.readOnly
  override def writeOnly: Option[Boolean] = underlying.writeOnly
  override def deprecated: Option[Boolean] = underlying.deprecated

  override def toJsonSchema: ujson.Value = {
    val base = underlying.toJsonSchema
    base("$schema") = ujson.Str(schemaValue)
    base
  }

  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    underlying.validate(value, context)
  }

  // Override modifier methods to maintain chaining
  override def optional: Schema = boogieloops.schema.OptionalSchema(this)
  override def nullable: Schema = boogieloops.schema.NullableSchema(this)
  override def withDefault(value: ujson.Value): Schema = boogieloops.schema.DefaultSchema(this, value)

  // Override with* methods to preserve $schema while adding other metadata
  override def withTitle(title: String): Schema = TitleSchema(this, title)
  override def withDescription(desc: String): Schema = DescriptionSchema(this, desc)
  override def withId(id: String): Schema = IdSchema(this, id)
  override def withDefs(defs: (String, Schema)*): Schema = DefsSchema(Some(this), defs.toMap)
}
