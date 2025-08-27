package chez.composition

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}

/**
 * If-Then-Else conditional schema - JSON Schema 2020-12 conditional validation
 */
case class IfThenElseChez(
    condition: Chez,
    thenSchema: Option[Chez] = None,
    elseSchema: Option[Chez] = None
) extends Chez {

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj()

    schema("if") = condition.toJsonSchema
    thenSchema.foreach(t => schema("then") = t.toJsonSchema)
    elseSchema.foreach(e => schema("else") = e.toJsonSchema)

    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))

    schema
  }

  /**
   * Validate a ujson.Value against this if-then-else schema
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    // For if-then-else, we need to:
    // 1. Check if the condition matches
    // 2. If it does, apply the "then" schema
    // 3. If it doesn't, apply the "else" schema
    //

    // TODO: we can do a little better here....

    val conditionResult = condition.validate(value, context)

    if (conditionResult.isValid) {
      // Condition matched, apply "then" schema if present
      thenSchema match {
        case Some(schema) => schema.validate(value, context)
        case None => ValidationResult.valid()
      }
    } else {
      // Condition didn't match, apply "else" schema if present
      elseSchema match {
        case Some(schema) => schema.validate(value, context)
        case None => ValidationResult.valid()
      }
    }
  }
}
