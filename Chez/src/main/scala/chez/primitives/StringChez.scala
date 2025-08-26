package chez.primitives

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
import scala.util.Try

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
        val minLengthErrors = minLength.fold(List.empty[chez.ValidationError]) { min =>
          if (stringValue.length < min)
            List(chez.ValidationError.MinLengthViolation(min, stringValue.length, context.path))
          else Nil
        }

        val maxLengthErrors = maxLength.fold(List.empty[chez.ValidationError]) { max =>
          if (stringValue.length > max)
            List(chez.ValidationError.MaxLengthViolation(max, stringValue.length, context.path))
          else Nil
        }

        val patternErrors = pattern.fold(List.empty[chez.ValidationError]) { p =>
          if (!stringValue.matches(p))
            List(chez.ValidationError.PatternMismatch(p, stringValue, context.path))
          else Nil
        }

        val constErrors = const.fold(List.empty[chez.ValidationError]) { c =>
          if (stringValue != c)
            List(chez.ValidationError.TypeMismatch(c, stringValue, context.path))
          else Nil
        }

        // Format validation - this is a basic implementation...
        val formatErrors = format.fold(List.empty[chez.ValidationError]) { f =>
          val isValid = f match {
            case "email" =>
              stringValue.matches("""^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$""")
            case "uri" => Try(java.net.URI(stringValue)).isSuccess
            case "uuid" =>
              stringValue.matches(
                """^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"""
              )
            case "date" => stringValue.matches("""^\d{4}-\d{2}-\d{2}$""")
            case "time" => stringValue.matches("""^\d{2}:\d{2}:\d{2}""")
            case "date-time" => stringValue.matches("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}""")
            // TODO: add support for more formats, enable custom format registration as well
            // TODO: maybe change format to enum to provide type safety and expected behavior
            case _ => true // Unknown formats are not validated
          }

          if (!isValid)
            List(chez.ValidationError.InvalidFormat(f, stringValue, context.path))
          else Nil
        }

        val allErrors = minLengthErrors ++ maxLengthErrors ++ patternErrors ++ constErrors ++ formatErrors

        if (allErrors.isEmpty) {
          ValidationResult.valid()
        } else {
          ValidationResult.invalid(allErrors)
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
