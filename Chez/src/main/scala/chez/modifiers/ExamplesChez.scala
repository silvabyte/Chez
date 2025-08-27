package chez.modifiers

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}

/**
 * Wrapper for adding examples metadata to a schema
 *
 * According to JSON Schema 2020-12, examples provides sample JSON values
 * that are intended to validate against the schema.
 */
case class ExamplesChez[T <: Chez](
    underlying: T,
    examplesValue: List[ujson.Value]
) extends Chez {

  override def examples: Option[List[ujson.Value]] = Some(examplesValue)

  // Delegate all other metadata to underlying schema
  override def title: Option[String] = underlying.title
  override def description: Option[String] = underlying.description
  override def default: Option[ujson.Value] = underlying.default
  override def readOnly: Option[Boolean] = underlying.readOnly
  override def writeOnly: Option[Boolean] = underlying.writeOnly
  override def deprecated: Option[Boolean] = underlying.deprecated

  // Delegate all core vocabulary to underlying schema
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
    if (examplesValue.nonEmpty) {
      base("examples") = ujson.Arr(examplesValue*)
    }
    base
  }

  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    underlying.validate(value, context)
  }

  // Override modifier methods to maintain chaining
  override def optional: Chez = chez.OptionalChez(this)
  override def nullable: Chez = chez.NullableChez(this)
  override def withDefault(value: ujson.Value): Chez = chez.DefaultChez(this, value)

  // Override with* methods to preserve examples while adding other metadata
  override def withTitle(title: String): Chez = TitleChez(this, title)
  override def withDescription(desc: String): Chez = DescriptionChez(this, desc)
  override def withSchema(schema: String): Chez = SchemaChez(this, schema)
  override def withId(id: String): Chez = IdChez(this, id)
  override def withDefs(defs: (String, Chez)*): Chez = chez.modifiers.DefsChez(Some(this), defs.toMap)
}
