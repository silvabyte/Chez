package chez.modifiers

import chez.Chez
import upickle.default.*

/**
 * Wrapper for adding description metadata to a schema
 *
 * According to JSON Schema 2020-12, description provides a more detailed explanation
 * of the purpose of the instance described by the schema.
 */
case class DescriptionChez[T <: Chez](
    underlying: T,
    descriptionValue: String
) extends Chez {

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
  override def $defs: Option[Map[String, Chez]] = underlying.$defs
  override def $dynamicRef: Option[String] = underlying.$dynamicRef
  override def $dynamicAnchor: Option[String] = underlying.$dynamicAnchor
  override def $vocabulary: Option[Map[String, Boolean]] = underlying.$vocabulary
  override def $comment: Option[String] = underlying.$comment

  override def toJsonSchema: ujson.Value = {
    val base = underlying.toJsonSchema
    base("description") = ujson.Str(descriptionValue)
    base
  }

  // Override modifier methods to maintain chaining
  override def optional: Chez = chez.OptionalChez(this)
  override def nullable: Chez = chez.NullableChez(this)
  override def withDefault(value: ujson.Value): Chez = chez.DefaultChez(this, value)

  // Override with* methods to preserve description while adding other metadata
  override def withTitle(title: String): Chez = TitleChez(this, title)
  override def withSchema(schema: String): Chez = SchemaChez(this, schema)
  override def withId(id: String): Chez = IdChez(this, id)
  override def withDefs(defs: (String, Chez)*): Chez = chez.modifiers.DefsChez(this, defs.toMap)
}
