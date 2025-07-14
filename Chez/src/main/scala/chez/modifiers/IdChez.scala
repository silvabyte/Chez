package chez.modifiers

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
import upickle.default.*

/**
 * Wrapper for adding $id metadata to a schema
 *
 * According to JSON Schema 2020-12, $id is used to declare a unique identifier
 * for the schema. It serves as the base URI for resolving relative references.
 */
case class IdChez[T <: Chez](
    underlying: T,
    idValue: String
) extends Chez {

  override def $id: Option[String] = Some(idValue)

  // Delegate all other core vocabulary to underlying schema
  override def $schema: Option[String] = underlying.$schema
  override def $ref: Option[String] = underlying.$ref
  override def $defs: Option[Map[String, Chez]] = underlying.$defs
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
    base("$id") = ujson.Str(idValue)
    base
  }

  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    underlying.validate(value, context)
  }

  // Override modifier methods to maintain chaining
  override def optional: Chez = chez.OptionalChez(this)
  override def nullable: Chez = chez.NullableChez(this)
  override def withDefault(value: ujson.Value): Chez = chez.DefaultChez(this, value)

  // Override with* methods to preserve $id while adding other metadata
  override def withTitle(title: String): Chez = TitleChez(this, title)
  override def withDescription(desc: String): Chez = DescriptionChez(this, desc)
  override def withSchema(schema: String): Chez = SchemaChez(this, schema)
  override def withDefs(defs: (String, Chez)*): Chez = chez.modifiers.DefsChez(this, defs.toMap)
}
