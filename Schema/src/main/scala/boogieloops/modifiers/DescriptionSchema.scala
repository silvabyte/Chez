package boogieloops.schema.modifiers

import boogieloops.schema.Schema
import boogieloops.schema.validation.{ValidationResult, ValidationContext}

/**
 * Wrapper for adding description metadata to a schema
 *
 * According to JSON Schema 2020-12, description provides a more detailed explanation
 * of the purpose of the instance described by the schema.
 */
case class DescriptionSchema[T <: Schema](
    underlying: T,
    descriptionValue: String
) extends Schema {

  override def description: Option[String] = Some(descriptionValue)

  // Delegate all other metadata to underlying schema
  override def title: Option[String] = underlying.title
  override def default: Option[ujson.Value] = underlying.default
  override def examples: Option[List[ujson.Value]] = underlying.examples
  override def readOnly: Option[Boolean] = underlying.readOnly
  override def writeOnly: Option[Boolean] = underlying.writeOnly
  override def deprecated: Option[Boolean] = underlying.deprecated

  // Delegate core vocabulary to underlying schema
  override def $schema: Option[String] = underlying.$schema
  override def $id: Option[String] = underlying.$id
  override def $ref: Option[String] = underlying.$ref
  override def $defs: Option[Map[String, Schema]] = underlying.$defs
  override def $dynamicRef: Option[String] = underlying.$dynamicRef
  override def $dynamicAnchor: Option[String] = underlying.$dynamicAnchor
  override def $vocabulary: Option[Map[String, Boolean]] = underlying.$vocabulary
  override def $comment: Option[String] = underlying.$comment

  override def toJsonSchema: ujson.Value = {
    val base = underlying.toJsonSchema
    base("description") = ujson.Str(descriptionValue)
    base
  }

  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    underlying.validate(value, context)
  }

  // Override modifier methods to maintain chaining
  override def optional: Schema = boogieloops.schema.OptionalSchema(this)
  override def nullable: Schema = boogieloops.schema.NullableSchema(this)
  override def withDefault(value: ujson.Value): Schema = boogieloops.schema.DefaultSchema(this, value)

  // Override with* methods to preserve description while adding other metadata
  override def withTitle(title: String): Schema = TitleSchema(this, title)
  override def withSchema(schema: String): Schema = SchemaModifier(this, schema)
  override def withId(id: String): Schema = IdSchema(this, id)
  override def withDefs(defs: (String, Schema)*): Schema = DefsSchema(Some(this), defs.toMap)
}
