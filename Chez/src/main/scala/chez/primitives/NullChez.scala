package chez.primitives

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
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

  /**
   * Validate a ujson.Value against this null schema
   */
  def validate(value: ujson.Value): ValidationResult = {
    validate(value, ValidationContext())
  }

  /**
   * Validate a ujson.Value against this null schema with context
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    // Type check for ujson.Null
    value match {
      case ujson.Null =>
        // Null values are always valid for null schema
        ValidationResult.valid()
      case _ =>
        // Non-null ujson.Value type - return TypeMismatch error
        val error = chez.ValidationError.TypeMismatch("null", getValueType(value), context.path)
        ValidationResult.invalid(error)
    }
  }

  /**
   * Get string representation of ujson.Value type for error messages
   */
  private def getValueType(value: ujson.Value): String = {
    value match {
      case _: ujson.Str => "string"
      case _: ujson.Num => "number"  
      case _: ujson.Bool => "boolean"
      case ujson.Null => "null"
      case _: ujson.Arr => "array"
      case _: ujson.Obj => "object"
    }
  }
}