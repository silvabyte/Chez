package chez.primitives

import chez.Chez
import upickle.default.*

/**
 * Boolean schema type with JSON Schema 2020-12 validation keywords
 */
case class BooleanChez(
  const: Option[Boolean] = None
) extends Chez {
  
  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("type" -> ujson.Str("boolean"))
    
    const.foreach(c => schema("const") = ujson.Bool(c))
    
    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))
    
    schema
  }
  
  /**
   * Validate a boolean value against this schema
   */
  def validate(value: Boolean): List[chez.ValidationError] = {
    var errors = List.empty[chez.ValidationError]
    
    // Const validation
    const.foreach { c =>
      if (value != c) {
        errors = chez.ValidationError.TypeMismatch(c.toString, value.toString, "/") :: errors
      }
    }
    
    errors.reverse
  }
}