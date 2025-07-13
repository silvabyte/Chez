package chez.primitives

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
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

  /**
   * Validate a ujson.Value against this boolean schema
   */
  def validate(value: ujson.Value): ValidationResult = {
    validate(value, ValidationContext())
  }

  /**
   * Validate a ujson.Value against this boolean schema with context
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    // Type check for ujson.Bool
    value match {
      case ujson.Bool(booleanValue) =>
        // Delegate to existing validate(Boolean) method but update error paths
        val errors = validate(booleanValue)
        val pathAwareErrors = errors.map(updateErrorPath(_, context.path))
        if (pathAwareErrors.isEmpty) {
          ValidationResult.valid()
        } else {
          ValidationResult.invalid(pathAwareErrors)
        }
      case _ =>
        // Non-boolean ujson.Value type - return TypeMismatch error
        val error = chez.ValidationError.TypeMismatch("boolean", getValueType(value), context.path)
        ValidationResult.invalid(error)
    }
  }

  /**
   * Update error path for context-aware error reporting
   */
  private def updateErrorPath(error: chez.ValidationError, path: String): chez.ValidationError = {
    error match {
      case chez.ValidationError.TypeMismatch(expected, actual, _) =>
        chez.ValidationError.TypeMismatch(expected, actual, path)
      case other => other // For error types that don't need path updates
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