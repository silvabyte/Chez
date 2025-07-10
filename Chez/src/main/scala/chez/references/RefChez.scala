package chez.references

import chez.Chez
import upickle.default.*

/**
 * JSON Schema $ref keyword implementation
 * 
 * Represents a reference to another schema definition. According to JSON Schema 2020-12,
 * $ref is a URI reference that identifies a schema to be applied to the instance.
 * When $ref is present, other keywords are typically ignored in that schema location.
 */
case class RefChez(
  ref: String
) extends Chez {
  
  override def $ref: Option[String] = Some(ref)
  
  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("$ref" -> ujson.Str(ref))
    
    // Add meta-data keywords if present
    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    
    schema
  }
  
  /**
   * Validate a value against this reference schema
   * 
   * Note: In a complete implementation, this would resolve the reference
   * and validate against the target schema. For now, we return a reference error
   * indicating that reference resolution is needed.
   */
  def validate(value: ujson.Value): List[chez.ValidationError] = {
    // In a full implementation, this would:
    // 1. Resolve the reference URI against the base URI
    // 2. Retrieve the target schema
    // 3. Validate the value against the target schema
    // 
    // For now, we return an error indicating unresolved reference
    List(chez.ValidationError.ReferenceError(ref, "/"))
  }
}