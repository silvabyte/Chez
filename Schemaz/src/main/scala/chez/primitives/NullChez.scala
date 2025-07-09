package chez.primitives

import chez.Chez
import upickle.default.*

/**
 * Null schema type with JSON Schema 2020-12 validation keywords
 */
case class NullChez() extends Chez {
  
  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("type" -> ujson.Str("null"))
    
    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))
    
    schema
  }
  
  /**
   * Validate a null value against this schema
   */
  def validate(value: Null): List[chez.ValidationError] = {
    // Null values are always valid for null schema
    List.empty[chez.ValidationError]
  }
}