package chez.validation

import chez.Chez

/**
 * Context for validation operations that tracks the current JSON path and schema context
 * 
 * This enables proper error reporting with accurate JSON paths during recursive validation.
 * The path follows JSONPath notation (e.g., "/user/name", "/items/0", "/properties/address/street").
 */
case class ValidationContext(
  path: String = "/",
  rootSchema: Option[Chez] = None
) {
  /**
   * Create a new context with an extended path for property access
   */
  def withProperty(propertyName: String): ValidationContext = {
    val newPath = if (path == "/") s"/$propertyName" else s"$path/$propertyName"
    copy(path = newPath)
  }
  
  /**
   * Create a new context with an extended path for array index access
   */
  def withIndex(index: Int): ValidationContext = {
    val newPath = if (path == "/") s"/$index" else s"$path/$index"
    copy(path = newPath)
  }
  
  /**
   * Create a new context with a custom path segment
   */
  def withPath(segment: String): ValidationContext = {
    val newPath = if (path == "/") s"/$segment" else s"$path/$segment"
    copy(path = newPath)
  }
  
  /**
   * Create a new context with the root schema set
   */
  def withRootSchema(schema: Chez): ValidationContext = {
    copy(rootSchema = Some(schema))
  }
}