package chez.primitives

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
import upickle.default.*

/**
 * Number schema type with JSON Schema 2020-12 validation keywords
 */
case class NumberChez(
  minimum: Option[Double] = None,
  maximum: Option[Double] = None,
  exclusiveMinimum: Option[Double] = None,
  exclusiveMaximum: Option[Double] = None,
  multipleOf: Option[Double] = None,
  const: Option[Double] = None
) extends Chez {
  
  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("type" -> ujson.Str("number"))
    
    minimum.foreach(min => schema("minimum") = ujson.Num(min))
    maximum.foreach(max => schema("maximum") = ujson.Num(max))
    exclusiveMinimum.foreach(min => schema("exclusiveMinimum") = ujson.Num(min))
    exclusiveMaximum.foreach(max => schema("exclusiveMaximum") = ujson.Num(max))
    multipleOf.foreach(mul => schema("multipleOf") = ujson.Num(mul))
    const.foreach(c => schema("const") = ujson.Num(c))
    
    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))
    
    schema
  }
  
  /**
   * Validate a number value against this schema
   */
  def validate(value: Double): List[chez.ValidationError] = {
    var errors = List.empty[chez.ValidationError]
    
    // Minimum validation
    minimum.foreach { min =>
      if (value < min) {
        errors = chez.ValidationError.OutOfRange(Some(min), None, value, "/") :: errors
      }
    }
    
    // Maximum validation
    maximum.foreach { max =>
      if (value > max) {
        errors = chez.ValidationError.OutOfRange(None, Some(max), value, "/") :: errors
      }
    }
    
    // Exclusive minimum validation
    exclusiveMinimum.foreach { min =>
      if (value <= min) {
        errors = chez.ValidationError.OutOfRange(Some(min), None, value, "/") :: errors
      }
    }
    
    // Exclusive maximum validation
    exclusiveMaximum.foreach { max =>
      if (value >= max) {
        errors = chez.ValidationError.OutOfRange(None, Some(max), value, "/") :: errors
      }
    }
    
    // Multiple of validation
    multipleOf.foreach { mul =>
      if (value % mul != 0) {
        errors = chez.ValidationError.MultipleOfViolation(mul, value, "/") :: errors
      }
    }
    
    // Const validation
    const.foreach { c =>
      if (value != c) {
        errors = chez.ValidationError.TypeMismatch(c.toString, value.toString, "/") :: errors
      }
    }
    
    
    errors.reverse
  }

  /**
   * Validate a ujson.Value against this number schema
   */
  def validate(value: ujson.Value): ValidationResult = {
    validate(value, ValidationContext())
  }

  /**
   * Validate a ujson.Value against this number schema with context
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    // Type check for ujson.Num
    value match {
      case ujson.Num(numberValue) =>
        // Delegate to existing validate(Double) method but update error paths
        val errors = validate(numberValue)
        val pathAwareErrors = errors.map(updateErrorPath(_, context.path))
        if (pathAwareErrors.isEmpty) {
          ValidationResult.valid()
        } else {
          ValidationResult.invalid(pathAwareErrors)
        }
      case _ =>
        // Non-number ujson.Value type - return TypeMismatch error
        val error = chez.ValidationError.TypeMismatch("number", getValueType(value), context.path)
        ValidationResult.invalid(error)
    }
  }

  /**
   * Update error path for context-aware error reporting
   */
  private def updateErrorPath(error: chez.ValidationError, path: String): chez.ValidationError = {
    error match {
      case chez.ValidationError.OutOfRange(min, max, actual, _) =>
        chez.ValidationError.OutOfRange(min, max, actual, path)
      case chez.ValidationError.MultipleOfViolation(multiple, value, _) =>
        chez.ValidationError.MultipleOfViolation(multiple, value, path)
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