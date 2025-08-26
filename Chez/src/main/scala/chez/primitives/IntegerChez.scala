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
        // Inline validation logic
        val intValue = numberValue.toInt

        val minErrors = minimum.fold(List.empty[chez.ValidationError]) { min =>
          if (intValue < min)
            List(chez.ValidationError.OutOfRange(Some(min.toDouble), None, intValue.toDouble, context.path))
          else Nil
        }

        val maxErrors = maximum.fold(List.empty[chez.ValidationError]) { max =>
          if (intValue > max)
            List(chez.ValidationError.OutOfRange(None, Some(max.toDouble), intValue.toDouble, context.path))
          else Nil
        }

        // Exclusive minimum validation
        val exclusiveMinErrors = exclusiveMinimum.fold(List.empty[chez.ValidationError]) { min =>
          if (intValue <= min)
            List(chez.ValidationError.OutOfRange(Some(min.toDouble), None, intValue.toDouble, context.path))
          else Nil
        }

        // Exclusive maximum validation
        val exclusiveMaxErrors = exclusiveMaximum.fold(List.empty[chez.ValidationError]) { max =>
          if (intValue >= max)
            List(chez.ValidationError.OutOfRange(None, Some(max.toDouble), intValue.toDouble, context.path))
          else Nil
        }

        // Multiple of validation
        val multipleErrors = multipleOf.fold(List.empty[chez.ValidationError]) { mul =>
          if (intValue % mul != 0)
            List(chez.ValidationError.MultipleOfViolation(mul.toDouble, intValue.toDouble, context.path))
          else Nil
        }

        // Const validation
        val constErrors = const.fold(List.empty[chez.ValidationError]) { c =>
          if (intValue != c)
            List(chez.ValidationError.TypeMismatch(c.toString, intValue.toString, context.path))
          else Nil
        }

        val allErrors =
          minErrors ++ maxErrors ++ exclusiveMinErrors ++ exclusiveMaxErrors ++ multipleErrors ++ constErrors

        if (allErrors.isEmpty) {
          ValidationResult.valid()
        } else {
          ValidationResult.invalid(allErrors)
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
