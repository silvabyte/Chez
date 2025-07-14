package chez.complex

import chez.Chez
import chez.validation.{ValidationResult, ValidationContext}
import upickle.default.*
import scala.util.matching.Regex

/**
 * Object schema type with JSON Schema 2020-12 validation keywords
 */
case class ObjectChez(
    properties: Map[String, Chez] = Map.empty,
    required: Set[String] = Set.empty,
    minProperties: Option[Int] = None,
    maxProperties: Option[Int] = None,
    additionalProperties: Option[Boolean] = None,
    additionalPropertiesSchema: Option[Chez] = None,
    patternProperties: Map[String, Chez] = Map.empty,
    propertyNames: Option[Chez] = None,
    dependentRequired: Map[String, Set[String]] = Map.empty,
    dependentSchemas: Map[String, Chez] = Map.empty,
    unevaluatedProperties: Option[Boolean] = None
) extends Chez {

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("type" -> ujson.Str("object"))

    // Properties
    if (properties.nonEmpty) {
      val propsMap = properties.map { case (key, value) =>
        key -> value.toJsonSchema
      }
      schema("properties") = ujson.Obj.from(propsMap)
    }

    // Required properties
    if (required.nonEmpty) {
      schema("required") = ujson.Arr(required.map(ujson.Str(_)).toSeq*)
    }

    // Validation keywords
    minProperties.foreach(min => schema("minProperties") = ujson.Num(min))
    maxProperties.foreach(max => schema("maxProperties") = ujson.Num(max))

    // Additional properties - can be boolean or schema
    additionalProperties.foreach(add => schema("additionalProperties") = ujson.Bool(add))
    additionalPropertiesSchema.foreach(aps => schema("additionalProperties") = aps.toJsonSchema)

    // Pattern properties
    if (patternProperties.nonEmpty) {
      val patternPropsMap = patternProperties.map { case (pattern, schemaValue) =>
        pattern -> schemaValue.toJsonSchema
      }
      schema("patternProperties") = ujson.Obj.from(patternPropsMap)
    }

    // Property names validation
    propertyNames.foreach(pn => schema("propertyNames") = pn.toJsonSchema)

    // Dependent validation (2020-12 features)
    if (dependentRequired.nonEmpty) {
      val depReqMap = dependentRequired.map { case (prop, deps) =>
        prop -> ujson.Arr(deps.map(ujson.Str(_)).toSeq*)
      }
      schema("dependentRequired") = ujson.Obj.from(depReqMap)
    }

    if (dependentSchemas.nonEmpty) {
      val depSchemaMap = dependentSchemas.map { case (prop, schemaValue) =>
        prop -> schemaValue.toJsonSchema
      }
      schema("dependentSchemas") = ujson.Obj.from(depSchemaMap)
    }

    // Unevaluated properties (2020-12 feature)
    unevaluatedProperties.foreach(unevaluated =>
      schema("unevaluatedProperties") = ujson.Bool(unevaluated)
    )

    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))

    schema
  }

  /**
   * Validate a ujson.Value against this object schema
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    value match {
      case obj: ujson.Obj =>
        // Inline validation logic
        val errors = validateObject(obj, context)
        if (errors.isEmpty) {
          ValidationResult.valid()
        } else {
          ValidationResult.invalid(errors)
        }
      case _ =>
        // Non-object ujson.Value type - return TypeMismatch error
        val error = chez.ValidationError.TypeMismatch("object", getValueType(value), context.path)
        ValidationResult.invalid(error)
    }
  }

  /**
   * Validate an object value against this schema with proper context tracking
   */
  private def validateObject(value: ujson.Obj, context: ValidationContext): List[chez.ValidationError] = {
    var errors = List.empty[chez.ValidationError]

    // Min properties validation
    minProperties.foreach { min =>
      if (value.value.size < min) {
        errors =
          chez.ValidationError.MinPropertiesViolation(min, value.value.size, context.path) :: errors
      }
    }

    // Max properties validation
    maxProperties.foreach { max =>
      if (value.value.size > max) {
        errors =
          chez.ValidationError.MaxPropertiesViolation(max, value.value.size, context.path) :: errors
      }
    }

    // Required properties validation
    required.foreach { prop =>
      if (!value.value.contains(prop)) {
        errors = chez.ValidationError.MissingField(prop, context.path) :: errors
      }
    }

    // Property validation - validate each property against its schema
    properties.foreach { case (propName, propSchema) =>
      value.value.get(propName).foreach { propValue =>
        val propContext = context.withProperty(propName)
        val validationResult = propSchema.validate(propValue, propContext)
        if (!validationResult.isValid) {
          errors = validationResult.errors ++ errors
        }
      }
    }

    // Pattern properties validation
    patternProperties.foreach { case (pattern, patternSchema) =>
      val regex = new Regex(pattern)
      value.value.foreach { case (propName, propValue) =>
        if (regex.findFirstIn(propName).isDefined && !properties.contains(propName)) {
          val propContext = context.withProperty(propName)
          val validationResult = patternSchema.validate(propValue, propContext)
          if (!validationResult.isValid) {
            errors = validationResult.errors ++ errors
          }
        }
      }
    }

    // Additional properties validation
    val definedProperties = properties.keySet
    val patternMatchedProperties = patternProperties.keys.flatMap { pattern =>
      val regex = new Regex(pattern)
      value.value.keys.filter(propName => regex.findFirstIn(propName).isDefined)
    }.toSet

    val additionalProps = value.value.keySet -- definedProperties -- patternMatchedProperties

    additionalProperties match {
      case Some(false) =>
        // Additional properties not allowed
        additionalProps.foreach { prop =>
          errors = chez.ValidationError.AdditionalProperty(prop, context.path) :: errors
        }
      case _ =>
        // Additional properties allowed or unspecified
        additionalPropertiesSchema match {
          case Some(additionalSchema) =>
            // Validate additional properties against the additional properties schema
            additionalProps.foreach { propName =>
              value.value.get(propName).foreach { propValue =>
                val propContext = context.withProperty(propName)
                val validationResult = additionalSchema.validate(propValue, propContext)
                if (!validationResult.isValid) {
                  errors = validationResult.errors ++ errors
                }
              }
            }
          case None =>
          // No additional properties schema - allow any additional properties
        }
    }

    errors.reverse
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
