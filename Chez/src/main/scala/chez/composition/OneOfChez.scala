package chez.composition

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
import upickle.default.*

/**
 * OneOf composition schema - validates if the instance is valid against exactly one of the schemas
 */
case class OneOfChez(
    schemas: List[Chez]
) extends Chez {

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj()

    schema("oneOf") = ujson.Arr(schemas.map(_.toJsonSchema)*)

    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))

    schema
  }

  /**
   * Validate a ujson.Value against this oneOf schema
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    // For oneOf, exactly one schema must validate successfully
    val results = schemas.map(_.validate(value, context))
    val successCount = results.count(_.isValid)

    if (successCount == 1) {
      ValidationResult.valid()
    } else if (successCount == 0) {
      val allErrors = results.flatMap(_.errors)
      val error = chez.ValidationError.CompositionError(
        "Value does not match any of the schemas in oneOf",
        context.path
      )
      ValidationResult.invalid(error :: allErrors)
    } else {
      val error = chez.ValidationError.CompositionError(
        "Value matches more than one schema in oneOf",
        context.path
      )
      ValidationResult.invalid(error)
    }
  }
}
