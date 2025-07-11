package chez.derivation

import chez.*
import chez.complex.ObjectChez

/**
 * Analysis utilities for ObjectChez schemas
 * 
 * This module provides compile-time and runtime analysis of object schemas,
 * extracting field information needed for ReadWriter generation.
 */
object ObjectAnalysis {
  
  /**
   * Information about a single object field
   */
  case class FieldInfo(
    name: String,
    schema: Chez,
    required: Boolean,
    hasDefault: Boolean = false
  )
  
  /**
   * Complete analysis of an ObjectChez schema
   */
  case class ObjectInfo(
    fields: List[FieldInfo],
    additionalProperties: Option[Boolean],
    constraints: ObjectConstraints
  )
  
  /**
   * Object-level validation constraints
   */
  case class ObjectConstraints(
    minProperties: Option[Int],
    maxProperties: Option[Int],
    patternProperties: Map[String, Chez],
    propertyNames: Option[Chez]
  )
  
  /**
   * Analyze an ObjectChez schema and extract field information
   */
  def analyzeObjectSchema(objectChez: ObjectChez): ObjectInfo = {
    val fields = objectChez.properties.map { case (name, schema) =>
      FieldInfo(
        name = name,
        schema = schema,
        required = objectChez.required.contains(name),
        hasDefault = schema.default.isDefined
      )
    }.toList.sortBy(_.name) // Sort for consistent ordering
    
    val constraints = ObjectConstraints(
      minProperties = objectChez.minProperties,
      maxProperties = objectChez.maxProperties,
      patternProperties = objectChez.patternProperties,
      propertyNames = objectChez.propertyNames
    )
    
    ObjectInfo(
      fields = fields,
      additionalProperties = objectChez.additionalProperties,
      constraints = constraints
    )
  }
  
  /**
   * Get all required field names from an ObjectChez
   */
  def getRequiredFields(objectChez: ObjectChez): Set[String] = {
    objectChez.required
  }
  
  /**
   * Get all optional field names from an ObjectChez
   */
  def getOptionalFields(objectChez: ObjectChez): Set[String] = {
    objectChez.properties.keySet -- objectChez.required
  }
  
  /**
   * Check if a field is required in the schema
   */
  def isFieldRequired(objectChez: ObjectChez, fieldName: String): Boolean = {
    objectChez.required.contains(fieldName)
  }
  
  /**
   * Get the schema for a specific field
   */
  def getFieldSchema(objectChez: ObjectChez, fieldName: String): Option[Chez] = {
    objectChez.properties.get(fieldName)
  }
  
  /**
   * Validate that a field set is compatible with the object schema
   */
  def validateFieldSet(objectChez: ObjectChez, fieldNames: Set[String]): List[String] = {
    val errors = scala.collection.mutable.ListBuffer[String]()
    
    // Check for missing required fields
    val missingRequired = objectChez.required -- fieldNames
    if (missingRequired.nonEmpty) {
      errors += s"Missing required fields: ${missingRequired.mkString(", ")}"
    }
    
    // Check for unknown fields (if additionalProperties is false)
    objectChez.additionalProperties match {
      case Some(false) =>
        val unknownFields = fieldNames -- objectChez.properties.keySet
        if (unknownFields.nonEmpty) {
          errors += s"Unknown fields (additionalProperties=false): ${unknownFields.mkString(", ")}"
        }
      case _ => // Allow additional properties
    }
    
    // Check property count constraints
    objectChez.minProperties.foreach { min =>
      if (fieldNames.size < min) {
        errors += s"Too few properties: ${fieldNames.size} < $min"
      }
    }
    
    objectChez.maxProperties.foreach { max =>
      if (fieldNames.size > max) {
        errors += s"Too many properties: ${fieldNames.size} > $max"
      }
    }
    
    errors.toList
  }
  
  /**
   * Generate a type-safe field name list for use in generated code
   */
  def generateFieldNameList(objectChez: ObjectChez): List[String] = {
    objectChez.properties.keys.toList.sorted
  }
  
  /**
   * Check if the object schema is simple (no complex validation rules)
   */
  def isSimpleObjectSchema(objectChez: ObjectChez): Boolean = {
    objectChez.patternProperties.isEmpty &&
    objectChez.propertyNames.isEmpty &&
    objectChez.dependentRequired.isEmpty &&
    objectChez.dependentSchemas.isEmpty &&
    objectChez.unevaluatedProperties.isEmpty
  }
}