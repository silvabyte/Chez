package chez.references

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}

/**
 * JSON Schema $dynamicRef keyword implementation
 *
 * Represents a dynamic reference that can be resolved differently based on the dynamic scope.
 * This is a JSON Schema 2020-12 feature that allows for more flexible reference resolution
 * in recursive schemas and schema composition scenarios.
 *
 * $dynamicRef works in conjunction with $dynamicAnchor to enable dynamic reference resolution
 * that can vary based on the evaluation context.
 */
case class DynamicRefChez(
    dynamicRef: String
) extends Chez {

  override def $dynamicRef: Option[String] = Some(dynamicRef)

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("$dynamicRef" -> ujson.Str(dynamicRef))

    // Add meta-data keywords if present
    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))

    schema
  }

  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    // TODO: implement this validation
    // In a complete implementation, this would:
    // 1. Search the dynamic scope for a matching $dynamicAnchor
    // 2. If found, resolve to that schema and validate
    // 3. Otherwise, treat as a normal $ref and resolve statically
    // 4. Validate the value against the resolved schema
    //
    // For now, we return an error indicating unresolved dynamic reference
    ValidationResult.invalid(List(chez.ValidationError.ReferenceError(
      s"$dynamicRef (dynamic)",
      context.path
    )))
  }

}
