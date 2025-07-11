package chez.modifiers

import chez.Chez
import upickle.default.*

/**
 * Wrapper for adding $defs metadata to a schema
 * 
 * According to JSON Schema 2020-12, $defs provides a location for reusable
 * schema definitions within a schema document.
 */
case class DefsChez[T <: Chez](
  underlying: T,
  defsValue: Map[String, Chez]
) extends Chez {
  
  override def $defs: Option[Map[String, Chez]] = Some(defsValue)
  
  // Delegate all other core vocabulary to underlying schema
  override def $schema: Option[String] = underlying.$schema
  override def $id: Option[String] = underlying.$id
  override def $ref: Option[String] = underlying.$ref
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
}