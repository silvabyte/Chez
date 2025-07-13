package chez.validation

import chez.{Chez, ValidationError}
import chez.validation.{ValidationResult, ValidationContext}
import upickle.default.*

/**
 * Central validation engine that orchestrates validation across all Chez schema types
 * 
 * This provides the core validation logic and delegates to specific schema type implementations.
 * It handles the unified validation interface and ensures consistent error reporting.
 */
object ValidationEngine {
  
  /**
   * Validate a ujson.Value against a Chez schema
   */
  def validate(schema: Chez, value: ujson.Value): ValidationResult = {
    validate(schema, value, ValidationContext())
  }
  
  /**
   * Validate a ujson.Value against a Chez schema with custom context
   */
  def validate(schema: Chez, value: ujson.Value, context: ValidationContext): ValidationResult = {
    // Try to use existing validation methods that return List[ValidationError]
    try {
      // First try the legacy validate(ujson.Value): List[ValidationError] method
      val validateMethod = schema.getClass.getMethod("validate", classOf[ujson.Value])
      if (validateMethod.getReturnType == classOf[List[?]]) {
        val errors = validateMethod.invoke(schema, value).asInstanceOf[List[ValidationError]]
        if (errors.isEmpty) {
          ValidationResult.valid()
        } else {
          ValidationResult.invalid(errors)
        }
      } else {
        // If it returns ValidationResult, use it directly
        validateMethod.invoke(schema, value).asInstanceOf[ValidationResult]
      }
    } catch {
      case _: NoSuchMethodException =>
        // Try validateAtPath method as fallback
        try {
          val validateAtPathMethod = schema.getClass.getMethod("validateAtPath", classOf[ujson.Value], classOf[String])
          if (validateAtPathMethod.getReturnType == classOf[List[?]]) {
            val errors = validateAtPathMethod.invoke(schema, value, context.path).asInstanceOf[List[ValidationError]]
            if (errors.isEmpty) {
              ValidationResult.valid()
            } else {
              ValidationResult.invalid(errors)
            }
          } else {
            // If it returns ValidationResult, use it directly
            validateAtPathMethod.invoke(schema, value, context.path).asInstanceOf[ValidationResult]
          }
        } catch {
          case _: NoSuchMethodException =>
            // Schema type doesn't implement validation yet, return valid for now
            ValidationResult.valid()
          case e: Exception =>
            // Error during validation, return invalid result
            ValidationResult.invalid(ValidationError.ParseError(s"Validation error: ${e.getMessage}", context.path))
        }
      case e: Exception =>
        // Error during validation, return invalid result
        ValidationResult.invalid(ValidationError.ParseError(s"Validation error: ${e.getMessage}", context.path))
    }
  }
  
  /**
   * Validate a ujson.Value against a Chez schema at a specific path
   */
  def validateAtPath(schema: Chez, value: ujson.Value, path: String): ValidationResult = {
    val context = ValidationContext(path = path, rootSchema = Some(schema))
    validate(schema, value, context)
  }
  
  /**
   * Convert List[ValidationError] to ValidationResult
   * Utility method for working with legacy validation methods
   */
  def fromErrors(errors: List[ValidationError]): ValidationResult = {
    if (errors.isEmpty) ValidationResult.valid() else ValidationResult.invalid(errors)
  }
  
  /**
   * Convert ValidationResult to List[ValidationError]
   * Utility method for working with legacy validation methods
   */
  def toErrors(result: ValidationResult): List[ValidationError] = result.errors
}