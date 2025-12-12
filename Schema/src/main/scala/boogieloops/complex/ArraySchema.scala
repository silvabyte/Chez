package boogieloops.schema.complex

import boogieloops.schema.Schema
import boogieloops.schema.validation.{ValidationResult, ValidationContext}

/**
 * Array schema type with JSON Schema 2020-12 validation keywords
 */
case class ArraySchema[T <: Schema](
    items: T,
    minItems: Option[Int] = None,
    maxItems: Option[Int] = None,
    uniqueItems: Option[Boolean] = None,
    prefixItems: Option[List[Schema]] = None,
    contains: Option[Schema] = None,
    minContains: Option[Int] = None,
    maxContains: Option[Int] = None,
    unevaluatedItems: Option[Boolean] = None
) extends Schema {

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
        val minItemsErrors = minItems.fold(List.empty[boogieloops.schema.ValidationError]) { min =>
          if (arrayValue.length < min)
            List(boogieloops.schema.ValidationError.MinItemsViolation(min, arrayValue.length, context.path))
          else List.empty
        }

        val maxItemsErrors = maxItems.fold(List.empty[boogieloops.schema.ValidationError]) { max =>
          if (arrayValue.length > max)
            List(boogieloops.schema.ValidationError.MaxItemsViolation(max, arrayValue.length, context.path))
          else List.empty
        }

        val uniqueItemsErrors = uniqueItems.fold(List.empty[boogieloops.schema.ValidationError]) { unique =>
          if (unique && arrayValue.distinct.length != arrayValue.length)
            List(boogieloops.schema.ValidationError.UniqueViolation(context.path))
          else List.empty
        }

        val structuralErrors = minItemsErrors ++ maxItemsErrors ++ uniqueItemsErrors

        // Handle prefixItems (tuple validation) if specified
        val itemErrors = prefixItems match {
          case Some(prefixes) =>
            // Validate prefix items with their specific schemas
            val prefixErrors = prefixes.zipWithIndex.flatMap { case (prefixSchema, index) =>
              if (index < arrayValue.length) {
                val item = arrayValue(index)
                val itemContext = context.withIndex(index)
                val itemResult = prefixSchema.validate(item, itemContext)
                if (!itemResult.isValid) itemResult.errors else Nil
              } else Nil
            }

            // Validate remaining items (beyond prefixItems) against the items schema
            val remainingErrors = arrayValue.zipWithIndex.drop(prefixes.length).flatMap { case (item, index) =>
              val itemContext = context.withIndex(index)
              val itemResult = items.validate(item, itemContext)
              if (!itemResult.isValid) itemResult.errors else Nil
            }

            prefixErrors ++ remainingErrors

          case None =>
            // Standard validation: validate each item against the items schema
            arrayValue.zipWithIndex.flatMap { case (item, index) =>
              val itemContext = context.withIndex(index)
              val itemResult = items.validate(item, itemContext)
              if (!itemResult.isValid) itemResult.errors else Nil
            }
        }

        // Contains validation - check that items matching the contains schema are within bounds
        val containsErrors = contains.fold(List.empty[boogieloops.schema.ValidationError]) { containsSchema =>
          val containsCount = arrayValue.count { item =>
            val itemResult = containsSchema.validate(item, context)
            itemResult.isValid
          }

          val minContainsCheck = minContains.forall(min => containsCount >= min)
          val maxContainsCheck = maxContains.forall(max => containsCount <= max)

          if (!minContainsCheck || !maxContainsCheck) {
            List(boogieloops.schema.ValidationError.ContainsViolation(
              minContains,
              maxContains,
              containsCount,
              context.path
            ))
          } else Nil
        }

        val allErrors = structuralErrors ++ itemErrors ++ containsErrors

        if (allErrors.isEmpty) {
          ValidationResult.valid()
        } else {
          ValidationResult.invalid(allErrors)
        }
      case _ =>
        val error = boogieloops.schema.ValidationError.TypeMismatch("array", getValueType(value), context.path)
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
