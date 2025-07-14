package chez.complex

import chez.Chez
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
   * Validate an array value against this schema
   */
  def validate(value: List[ujson.Value]): List[chez.ValidationError] = {
    var errors = List.empty[chez.ValidationError]

    // Min items validation
    minItems.foreach { min =>
      if (value.length < min) {
        errors = chez.ValidationError.MinItemsViolation(min, value.length, "/") :: errors
      }
    }

    // Max items validation
    maxItems.foreach { max =>
      if (value.length > max) {
        errors = chez.ValidationError.MaxItemsViolation(max, value.length, "/") :: errors
      }
    }

    // Unique items validation
    uniqueItems.foreach { unique =>
      if (unique && value.distinct.length != value.length) {
        errors = chez.ValidationError.UniqueViolation("/") :: errors
      }
    }

    // Validate each item against the items schema
    value.zipWithIndex.foreach { case (item, index) =>
    // For now, we'll implement basic validation
    // In practice, we'd need to validate each item against the items schema
    // This is a placeholder for proper item validation
    }

    errors.reverse
  }
}
