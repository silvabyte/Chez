package chez.modifiers

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
import upickle.default.*

/**
 * Wrapper for adding title metadata to a schema
 *
 * According to JSON Schema 2020-12, title provides a short description of the instance
 * and is primarily used for documentation and user interface purposes.
 */
case class TitleChez[T <: Chez](
    underlying: T,
    titleValue: String
) extends Chez {

  override def title: Option[String] = Some(titleValue)

  // Delegate all other metadata to underlying schema
  override def description: Option[String] = underlying.description
  override def default: Option[ujson.Value] = underlying.default
  override def examples: Option[List[ujson.Value]] = underlying.examples
  override def readOnly: Option[Boolean] = underlying.readOnly
  override def writeOnly: Option[Boolean] = underlying.writeOnly
  override def deprecated: Option[Boolean] = underlying.deprecated

  // Delegate core vocabulary to underlying schema
  override def $schema: Option[String] = underlying.$schema
  override def $id: Option[String] = underlying.$id
  override def $ref: Option[String] = underlying.$ref
  override def $defs: Option[Map[String, Chez]] = underlying.$defs
  override def $dynamicRef: Option[String] = underlying.$dynamicRef
  override def $dynamicAnchor: Option[String] = underlying.$dynamicAnchor
  override def $vocabulary: Option[Map[String, Boolean]] = underlying.$vocabulary
  override def $comment: Option[String] = underlying.$comment

  override def toJsonSchema: ujson.Value = {
    val base = underlying.toJsonSchema
    base("title") = ujson.Str(titleValue)
    base
  }

  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    underlying.validate(value, context)
  }

  // Override modifier methods to maintain chaining
  override def optional: Chez = chez.OptionalChez(this)
  override def nullable: Chez = chez.NullableChez(this)
  override def withDefault(value: ujson.Value): Chez = chez.DefaultChez(this, value)

  // Override with* methods to preserve title while adding other metadata
  override def withDescription(desc: String): Chez = DescriptionChez(this, desc)
  override def withSchema(schema: String): Chez = SchemaChez(this, schema)
  override def withId(id: String): Chez = IdChez(this, id)
  override def withDefs(defs: (String, Chez)*): Chez = chez.modifiers.DefsChez(Some(this), defs.toMap)
}
