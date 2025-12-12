package boogieloops.schema.derivation

import scala.compiletime.*
import scala.annotation.unused
import boogieloops.schema.*
import boogieloops.schema.complex.*

/**
 * Schema derivation for Scala collection types
 *
 * Provides given instances for Map, Set, Vector and other collection types
 * to integrate with the Schema derivation system.
 */
object CollectionSchemas {

  /**
   * Schematic derivation for Map[K, V] types
   *
   * Maps are represented as JSON objects in two ways:
   * - Map[String, V]: Uses additionalPropertiesSchema to allow any string key with V schema
   * - Map[K, V] where K != String: Uses patternProperties with ".*" pattern for any key
   */
  inline given [K, V](using @unused kSchema: Schematic[K], vSchema: Schematic[V]): Schematic[Map[K, V]] = {
    // Use compile-time type checking to determine key type
    inline erasedValue[K] match {
      case _: String =>
        // For Map[String, V], use additionalPropertiesSchema
        Schematic.instance(ObjectSchema(
          additionalPropertiesSchema = Some(vSchema.schema)
        ))
      case _ =>
        // For Map[K, V] where K != String, use patternProperties
        val keyDescription = inline erasedValue[K] match {
          case _: Int => "integer"
          case _: Long => "long"
          case _: Double => "double"
          case _: Float => "float"
          case _: Boolean => "boolean"
          case _ => "unknown"
        }

        Schematic.instance(ObjectSchema(
          patternProperties = Map(".*" -> vSchema.schema)
        ).withDescription(s"Map with ${keyDescription} keys"))
    }
  }

  /**
   * Schematic derivation for Set[T] types
   *
   * Sets are represented as JSON arrays with uniqueItems=true to ensure
   * no duplicate values are allowed in the schema validation.
   */
  inline given [T](using tSchema: Schematic[T]): Schematic[Set[T]] = {
    Schematic.instance(ArraySchema(
      items = tSchema.schema,
      uniqueItems = Some(true)
    ))
  }

  /**
   * Schematic derivation for Vector[T] types
   *
   * Vectors are represented as JSON arrays without uniqueItems constraint,
   * allowing duplicate values and preserving order.
   */
  inline given [T](using tSchema: Schematic[T]): Schematic[Vector[T]] = {
    Schematic.instance(ArraySchema(
      items = tSchema.schema
      // uniqueItems is None (default) to allow duplicates
    ))
  }

  /**
   * Helper to get type name for descriptions
   */
  private inline def typeNameOf[T]: String = {
    inline erasedValue[T] match {
      case _: Int => "Int"
      case _: Long => "Long"
      case _: Double => "Double"
      case _: Float => "Float"
      case _: Boolean => "Boolean"
      case _ => "Unknown"
    }
  }
}
