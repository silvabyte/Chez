package chez.primitives

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
import upickle.default.*

/**
 * Integer schema type with JSON Schema 2020-12 validation keywords
 */
case class IntegerChez(
    minimum: Option[Int] = None,
    maximum: Option[Int] = None,
    exclusiveMinimum: Option[Int] = None,
    exclusiveMaximum: Option[Int] = None,
    multipleOf: Option[Int] = None,
    const: Option[Int] = None
) extends Chez {

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("type" -> ujson.Str("integer"))

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
   * Validate an integer value against this schema
   */
  def validate(value: Int): List[chez.ValidationError] = {
    var errors = List.empty[chez.ValidationError]

    // Minimum validation
    minimum.foreach { min =>
      if (value < min) {
        errors =
          chez.ValidationError.OutOfRange(Some(min.toDouble), None, value.toDouble, "/") :: errors
      }
    }

    // Maximum validation
    maximum.foreach { max =>
      if (value > max) {
        errors =
          chez.ValidationError.OutOfRange(None, Some(max.toDouble), value.toDouble, "/") :: errors
      }
    }

    // Exclusive minimum validation
    exclusiveMinimum.foreach { min =>
      if (value <= min) {
        errors =
          chez.ValidationError.OutOfRange(Some(min.toDouble), None, value.toDouble, "/") :: errors
      }
    }

    // Exclusive maximum validation
    exclusiveMaximum.foreach { max =>
      if (value >= max) {
        errors =
          chez.ValidationError.OutOfRange(None, Some(max.toDouble), value.toDouble, "/") :: errors
      }
    }

    // Multiple of validation
    multipleOf.foreach { mul =>
      if (value % mul != 0) {
        errors =
          chez.ValidationError.MultipleOfViolation(mul.toDouble, value.toDouble, "/") :: errors
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
   * Validate a ujson.Value against this integer schema
   */
  def validate(value: ujson.Value): ValidationResult = {
    validate(value, ValidationContext())
  }

  /**
   * Validate a ujson.Value against this integer schema with context
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    // Type check for ujson.Num that represents an integer
    value match {
      case ujson.Num(numberValue) if numberValue.isWhole =>
        // Delegate to existing validate(Int) method but update error paths
        val errors = validate(numberValue.toInt)
        val pathAwareErrors = errors.map(updateErrorPath(_, context.path))
        if (pathAwareErrors.isEmpty) {
          ValidationResult.valid()
        } else {
          ValidationResult.invalid(pathAwareErrors)
        }
      case ujson.Num(_) =>
        // Non-integer number - return TypeMismatch error
        val error = chez.ValidationError.TypeMismatch("integer", "number", context.path)
        ValidationResult.invalid(error)
      case _ =>
        // Non-number ujson.Value type - return TypeMismatch error
        val error = chez.ValidationError.TypeMismatch("integer", getValueType(value), context.path)
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
