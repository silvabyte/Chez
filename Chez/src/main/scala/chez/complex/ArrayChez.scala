package chez.complex

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
import upickle.default.*

/**
 * Array schema type with JSON Schema 2020-12 validation keywords
 */
case class ArrayChez[T <: Chez](
    items: T,
    minItems: Option[Int] = None,
    maxItems: Option[Int] = None,
    uniqueItems: Option[Boolean] = None,
    prefixItems: Option[List[Chez]] = None,
    contains: Option[Chez] = None,
    minContains: Option[Int] = None,
    maxContains: Option[Int] = None,
    unevaluatedItems: Option[Boolean] = None
) extends Chez {

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("type" -> ujson.Str("array"))

    // Items schema
    schema("items") = items.toJsonSchema

    // Validation keywords
    minItems.foreach(min => schema("minItems") = ujson.Num(min))
    maxItems.foreach(max => schema("maxItems") = ujson.Num(max))
    uniqueItems.foreach(unique => schema("uniqueItems") = ujson.Bool(unique))

    // Prefix items (2020-12 feature for tuple validation)
    prefixItems.foreach(prefix =>
      schema("prefixItems") = ujson.Arr(prefix.map(_.toJsonSchema)*)
    )

    // Contains validation
    contains.foreach(c => schema("contains") = c.toJsonSchema)
    minContains.foreach(min => schema("minContains") = ujson.Num(min))
    maxContains.foreach(max => schema("maxContains") = ujson.Num(max))

    // Unevaluated items (2020-12 feature)
    unevaluatedItems.foreach(unevaluated =>
      schema("unevaluatedItems") = ujson.Bool(unevaluated)
    )

    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))

    schema
  }

  /**
   * Validate a ujson.Value against this array schema
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    value match {
      case arr: ujson.Arr =>
        val arrayValue = arr.arr.toList
        var errors = List.empty[chez.ValidationError]

        // Min items validation
        minItems.foreach { min =>
          if (arrayValue.length < min) {
            errors = chez.ValidationError.MinItemsViolation(min, arrayValue.length, context.path) :: errors
          }
        }

        // Max items validation
        maxItems.foreach { max =>
          if (arrayValue.length > max) {
            errors = chez.ValidationError.MaxItemsViolation(max, arrayValue.length, context.path) :: errors
          }
        }

        // Unique items validation
        uniqueItems.foreach { unique =>
          if (unique && arrayValue.distinct.length != arrayValue.length) {
            errors = chez.ValidationError.UniqueViolation(context.path) :: errors
          }
        }

        // Validate each item against the items schema
        arrayValue.zipWithIndex.foreach { case (item, index) =>
          val itemContext = context.withIndex(index)
          val itemResult = items.validate(item, itemContext)
          if (!itemResult.isValid) {
            errors = itemResult.errors ++ errors
          }
        }

        if (errors.isEmpty) {
          ValidationResult.valid()
        } else {
          ValidationResult.invalid(errors.reverse)
        }
      case _ =>
        val error = chez.ValidationError.TypeMismatch("array", getValueType(value), context.path)
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
