package chez.composition

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
import upickle.default.*

/**
 * AllOf composition schema - validates if the instance is valid against all of the schemas
 */
case class AllOfChez(
    schemas: List[Chez]
) extends Chez {

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj()

    schema("allOf") = ujson.Arr(schemas.map(_.toJsonSchema)*)

    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))

    schema
  }

  /**
   * Validate a ujson.Value against this allOf schema
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    // For allOf, all schemas must validate successfully
    var allErrors = List.empty[chez.ValidationError]
    
    schemas.foreach { schema =>
      val result = schema.validate(value, context)
      if (!result.isValid) {
        allErrors = result.errors ++ allErrors
      }
    }

    if (allErrors.isEmpty) {
      ValidationResult.valid()
    } else {
      ValidationResult.invalid(allErrors)
    }
  }

}
