package chez.primitives

import chez.Chez
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
    const: Option[String] = None,
    enumValues: Option[List[String]] = None
) extends Chez {

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("type" -> ujson.Str("string"))

    minLength.foreach(min => schema("minLength") = ujson.Num(min))
    maxLength.foreach(max => schema("maxLength") = ujson.Num(max))
    pattern.foreach(p => schema("pattern") = ujson.Str(p))
    format.foreach(f => schema("format") = ujson.Str(f))
    const.foreach(c => schema("const") = ujson.Str(c))
    enumValues.foreach(e => schema("enum") = ujson.Arr(e.map(ujson.Str(_))*))

    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))

    schema
  }

  /**
   * Validate a string value against this schema
   */
  def validate(value: String): List[chez.ValidationError] = {
    var errors = List.empty[chez.ValidationError]

    // Length validation
    minLength.foreach { min =>
      if (value.length < min) {
        errors = chez.ValidationError.MinLengthViolation(min, value.length, "/") :: errors
      }
    }

    maxLength.foreach { max =>
      if (value.length > max) {
        errors = chez.ValidationError.MaxLengthViolation(max, value.length, "/") :: errors
      }
    }

    // Pattern validation
    pattern.foreach { p =>
      if (!value.matches(p)) {
        errors = chez.ValidationError.PatternMismatch(p, value, "/") :: errors
      }
    }

    // Const validation
    const.foreach { c =>
      if (value != c) {
        errors = chez.ValidationError.TypeMismatch(c, value, "/") :: errors
      }
    }

    // Enum validation
    enumValues.foreach { e =>
      if (!e.contains(value)) {
        errors = chez.ValidationError.TypeMismatch(e.mkString(","), value, "/") :: errors
      }
    }

    // Format validation - this is a basic implementation...
    format.foreach { f =>
      val isValid = f match {
        case "email"     => value.matches("""^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$""")
        case "uri"       => Try(java.net.URI(value)).isSuccess
        case "uuid"      => value.matches("""^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$""")
        case "date"      => value.matches("""^\d{4}-\d{2}-\d{2}$""")
        case "time"      => value.matches("""^\d{2}:\d{2}:\d{2}$""")
        case "date-time" => value.matches("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}""")
        // TODO: add support for more formats, enable custom format registration as well
        // TODO: maybe change format to enum to provide type safety and expected behavior
        case _ => true // Unknown formats are not validated
      }

      if (!isValid) {
        errors = chez.ValidationError.InvalidFormat(f, value, "/") :: errors
      }
    }

    errors.reverse
  }
}
