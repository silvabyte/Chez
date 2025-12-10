package boogieloops.schema.validation

import boogieloops.schema.ValidationError

/**
 * Result of a validation operation
 *
 * Represents either a successful validation (valid) or a failed validation with errors (invalid).
 * This provides a unified result type for all validation operations in the Chez library.
 */
sealed trait ValidationResult {
  def isValid: Boolean
  def errors: List[ValidationError]

  /**
   * Combine this result with another validation result
   */
  def combine(other: ValidationResult): ValidationResult = (this, other) match {
    case (ValidationResult.Valid, ValidationResult.Valid) => ValidationResult.Valid
    case (ValidationResult.Valid, invalid @ ValidationResult.Invalid(_)) => invalid
    case (invalid @ ValidationResult.Invalid(_), ValidationResult.Valid) => invalid
    case (ValidationResult.Invalid(errors1), ValidationResult.Invalid(errors2)) =>
      ValidationResult.Invalid(errors1 ++ errors2)
  }
}

object ValidationResult {

  /**
   * Successful validation result
   */
  case object Valid extends ValidationResult {
    def isValid: Boolean = true
    def errors: List[ValidationError] = List.empty
  }

  /**
   * Failed validation result with errors
   */
  case class Invalid(errors: List[ValidationError]) extends ValidationResult {
    def isValid: Boolean = false
  }

  /**
   * Create a valid result
   */
  def valid(): ValidationResult = Valid

  /**
   * Create an invalid result with a single error
   */
  def invalid(error: ValidationError): ValidationResult = Invalid(List(error))

  /**
   * Create an invalid result with multiple errors
   */
  def invalid(errors: List[ValidationError]): ValidationResult = Invalid(errors)

  /**
   * Combine multiple validation results
   */
  def combine(results: List[ValidationResult]): ValidationResult = {
    results.foldLeft[ValidationResult](Valid)(_.combine(_))
  }
}
