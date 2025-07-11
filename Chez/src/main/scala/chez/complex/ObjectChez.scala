package chez.complex

import chez.Chez
import upickle.default.*

/**
 * Object schema type with JSON Schema 2020-12 validation keywords
 */
case class ObjectChez(
    properties: Map[String, Chez] = Map.empty,
    required: Set[String] = Set.empty,
    minProperties: Option[Int] = None,
    maxProperties: Option[Int] = None,
    additionalProperties: Option[Boolean] = None,
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
    additionalProperties.foreach(add => schema("additionalProperties") = ujson.Bool(add))

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
    unevaluatedProperties.foreach(unevaluated => schema("unevaluatedProperties") = ujson.Bool(unevaluated))

    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))

    schema
  }

  /**
   * Validate an object value against this schema
   */
  def validate(value: ujson.Obj): List[chez.ValidationError] = {
    var errors = List.empty[chez.ValidationError]

    // Min properties validation
    minProperties.foreach { min =>
      if (value.value.size < min) {
        errors = chez.ValidationError.MinPropertiesViolation(min, value.value.size, "/") :: errors
      }
    }

    // Max properties validation
    maxProperties.foreach { max =>
      if (value.value.size > max) {
        errors = chez.ValidationError.MaxPropertiesViolation(max, value.value.size, "/") :: errors
      }
    }

    // Required properties validation
    required.foreach { prop =>
      if (!value.value.contains(prop)) {
        errors = chez.ValidationError.MissingField(prop, "/") :: errors
      }
    }

    // Additional properties validation
    additionalProperties.foreach { additional =>
      if (!additional) {
        val unknownProps = value.value.keySet -- properties.keySet
        unknownProps.foreach { prop =>
          errors = chez.ValidationError.AdditionalProperty(prop, "/") :: errors
        }
      }
    }

    // Property validation (validate each property against its schema)
    properties.foreach { case (propName, propSchema) =>
      value.value.get(propName).foreach { propValue =>
        // For now, we'll implement basic validation
        // In practice, we'd need to validate each property against its schema
        // This is a placeholder for proper property validation
        // TODO: implement this
      }
    }

    errors.reverse
  }
}
