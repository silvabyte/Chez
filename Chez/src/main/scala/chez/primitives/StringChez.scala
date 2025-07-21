package chez.primitives

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
import upickle.default.*
import scala.util.{Try, Success, Failure}

/**
 * String schema type with JSON Schema 2020-12 validation keywords
 */
case class StringChez(
    minLength: Option[Int] = None,
    maxLength: Option[Int] = None,
    pattern: Option[String] = None,
    format: Option[String] = None,
    const: Option[String] = None
) extends Chez {

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("type" -> ujson.Str("string"))

    minLength.foreach(min => schema("minLength") = ujson.Num(min))
    maxLength.foreach(max => schema("maxLength") = ujson.Num(max))
    pattern.foreach(p => schema("pattern") = ujson.Str(p))
    format.foreach(f => schema("format") = ujson.Str(f))
    const.foreach(c => schema("const") = ujson.Str(c))

    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))

    schema
  }

  /**
   * Validate a ujson.Value against this string schema
   */
  def validate(value: ujson.Value): ValidationResult = {
    validate(value, ValidationContext())
  }

  /**
   * Validate a ujson.Value against this string schema with context
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    // Type check for ujson.Str
    value match {
      case ujson.Str(stringValue) =>
        // Inline validation logic
        var errors = List.empty[chez.ValidationError]

        // Length validation
        minLength.foreach { min =>
          if (stringValue.length < min) {
            errors = chez.ValidationError.MinLengthViolation(
              min,
              stringValue.length,
              context.path
            ) :: errors
          }
        }

        maxLength.foreach { max =>
          if (stringValue.length > max) {
            errors = chez.ValidationError.MaxLengthViolation(
              max,
              stringValue.length,
              context.path
            ) :: errors
          }
        }

        // Pattern validation
        pattern.foreach { p =>
          if (!stringValue.matches(p)) {
            errors = chez.ValidationError.PatternMismatch(p, stringValue, context.path) :: errors
          }
        }

        // Const validation
        const.foreach { c =>
          if (stringValue != c) {
            errors = chez.ValidationError.TypeMismatch(c, stringValue, context.path) :: errors
          }
        }

        // Format validation - this is a basic implementation...
        format.foreach { f =>
          val isValid = f match {
            case "email" =>
              stringValue.matches("""^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$""")
            case "uri" => Try(java.net.URI(stringValue)).isSuccess
            case "uuid" =>
              stringValue.matches(
                """^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"""
              )
            case "date" => stringValue.matches("""^\d{4}-\d{2}-\d{2}$""")
            case "time" => stringValue.matches("""^\d{2}:\d{2}:\d{2}$""")
            case "date-time" => stringValue.matches("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}""")
            // TODO: add support for more formats, enable custom format registration as well
            // TODO: maybe change format to enum to provide type safety and expected behavior
            case _ => true // Unknown formats are not validated
          }

          if (!isValid) {
            errors = chez.ValidationError.InvalidFormat(f, stringValue, context.path) :: errors
          }
        }

        if (errors.isEmpty) {
          ValidationResult.valid()
        } else {
          ValidationResult.invalid(errors.reverse)
        }
      case _ =>
        // Non-string ujson.Value type - return TypeMismatch error
        val error = chez.ValidationError.TypeMismatch("string", getValueType(value), context.path)
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
