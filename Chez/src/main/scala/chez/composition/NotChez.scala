package chez.composition

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
import upickle.default.*

/**
 * Not composition schema - validates if the instance is NOT valid against the schema
 */
case class NotChez(
    schema: Chez
) extends Chez {

  override def toJsonSchema: ujson.Value = {
    val schemaObj = ujson.Obj()

    schemaObj("not") = schema.toJsonSchema

    title.foreach(t => schemaObj("title") = ujson.Str(t))
    description.foreach(d => schemaObj("description") = ujson.Str(d))
    default.foreach(d => schemaObj("default") = d)
    examples.foreach(e => schemaObj("examples") = ujson.Arr(e*))

    schemaObj
  }

  /**
   * Validate a ujson.Value against this not schema
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    // For not, the schema must NOT validate successfully
    val result = schema.validate(value, context)

    if (result.isValid) {
      // Schema validated successfully, so NOT validation fails
      val error =
        chez.ValidationError.CompositionError("Value must NOT match the schema", context.path)
      ValidationResult.invalid(error)
    } else {
      // Schema validation failed, so NOT validation succeeds
      ValidationResult.valid()
    }
  }
}
