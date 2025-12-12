package boogieloops.schema.primitives

import boogieloops.schema.Schema
import boogieloops.schema.validation.{ValidationResult, ValidationContext}

/**
 * Number schema type with JSON Schema 2020-12 validation keywords
 */
case class NumberSchema(
    minimum: Option[Double] = None,
    maximum: Option[Double] = None,
    exclusiveMinimum: Option[Double] = None,
    exclusiveMaximum: Option[Double] = None,
    multipleOf: Option[Double] = None,
    const: Option[Double] = None
) extends Schema {

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
        // Inline validation logic
        val minErrors = minimum.fold(List.empty[boogieloops.schema.ValidationError]) { min =>
          if (numberValue < min)
            List(boogieloops.schema.ValidationError.OutOfRange(Some(min), None, numberValue, context.path))
          else Nil
        }

        val maxErrors = maximum.fold(List.empty[boogieloops.schema.ValidationError]) { max =>
          if (numberValue > max)
            List(boogieloops.schema.ValidationError.OutOfRange(None, Some(max), numberValue, context.path))
          else Nil
        }

        val exclusiveMinErrors = exclusiveMinimum.fold(List.empty[boogieloops.schema.ValidationError]) { min =>
          if (numberValue <= min)
            List(boogieloops.schema.ValidationError.OutOfRange(Some(min), None, numberValue, context.path))
          else Nil
        }

        val exclusiveMaxErrors = exclusiveMaximum.fold(List.empty[boogieloops.schema.ValidationError]) { max =>
          if (numberValue >= max)
            List(boogieloops.schema.ValidationError.OutOfRange(None, Some(max), numberValue, context.path))
          else Nil
        }

        val multipleErrors = multipleOf.fold(List.empty[boogieloops.schema.ValidationError]) { mul =>
          if (numberValue % mul != 0)
            List(boogieloops.schema.ValidationError.MultipleOfViolation(mul, numberValue, context.path))
          else Nil
        }

        val constErrors = const.fold(List.empty[boogieloops.schema.ValidationError]) { c =>
          if (numberValue != c)
            List(boogieloops.schema.ValidationError.TypeMismatch(c.toString, numberValue.toString, context.path))
          else Nil
        }

        val allErrors =
          minErrors ++ maxErrors ++ exclusiveMinErrors ++ exclusiveMaxErrors ++ multipleErrors ++ constErrors

        if (allErrors.isEmpty) {
          ValidationResult.valid()
        } else {
          ValidationResult.invalid(allErrors)
        }
      case _ =>
        // Non-number ujson.Value type - return TypeMismatch error
        val error = boogieloops.schema.ValidationError.TypeMismatch("number", getValueType(value), context.path)
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
