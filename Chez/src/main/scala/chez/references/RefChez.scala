package chez.references

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}

/**
 * JSON Schema $ref keyword implementation
 *
 * Represents a reference to another schema definition. According to JSON Schema 2020-12, $ref is a URI reference that identifies
 * a schema to be applied to the instance. When $ref is present, other keywords are typically ignored in that schema location.
 */
//TODO: consolidate with modifiers.DefsChez
case class RefChez(
    ref: String
) extends Chez {

  override def $ref: Option[String] = Some(ref)

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("$ref" -> ujson.Str(ref))

    // Add meta-data keywords if present
    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))

    schema
  }

  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    // In a complete implementation, this would:
    // 1. Resolve the reference URI against the base URI
    // 2. Retrieve the target schema
    // 3. Validate the value against the target schema
    //
    // For now, we return an error indicating unresolved reference
    ValidationResult.invalid(List(chez.ValidationError.ReferenceError(ref, context.path)))
  }

}
