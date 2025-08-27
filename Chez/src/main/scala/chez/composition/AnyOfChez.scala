package chez.composition

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}

/**
 * AnyOf composition schema - validates if the instance is valid against any of the schemas
 */
case class AnyOfChez(
    schemas: List[Chez]
) extends Chez {

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj()

    schema("anyOf") = ujson.Arr(schemas.map(_.toJsonSchema)*)

    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))

    schema
  }

  /**
   * Validate a ujson.Value against this anyOf schema
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    // For anyOf, at least one schema must validate successfully
    val results = schemas.map(_.validate(value, context))
    val hasSuccess = results.exists(_.isValid)

    if (hasSuccess) {
      ValidationResult.valid()
    } else {
      // If all schemas fail, return all collected errors
      val allErrors = results.flatMap(_.errors)
      ValidationResult.invalid(allErrors)
    }
  }
}
