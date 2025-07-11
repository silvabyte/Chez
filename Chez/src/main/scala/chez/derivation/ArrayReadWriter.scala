package chez.derivation

import upickle.default.*
import chez.*
import chez.complex.ArrayChez

/**
 * ReadWriter generation for ArrayChez schemas
 * 
 * This module generates ReadWriter[List[T]] instances for ArrayChez,
 * providing automatic serialization/deserialization of array data.
 */
object ArrayReadWriter {
  
  /**
   * Generate a ReadWriter[List[T]] for an ArrayChez schema
   * 
   * @param arrayChez The array schema definition
   * @param itemReadWriter ReadWriter for the array item type
   */
  def deriveArrayReadWriter[T](arrayChez: ArrayChez[?], itemReadWriter: ReadWriter[T]): ReadWriter[List[T]] = {
    
    readwriter[ujson.Value].bimap[List[T]](
      // Writer: List[T] -> ujson.Value
      list => writeArrayToJson(list, arrayChez)(using itemReadWriter),
      // Reader: ujson.Value -> List[T]  
      json => readArrayFromJson(json, arrayChez)(using itemReadWriter)
    )
  }
  
  /**
   * Write a List[T] to JSON using the array schema
   */
  private def writeArrayToJson[T](list: List[T], arrayChez: ArrayChez[?])(using itemReadWriter: ReadWriter[T]): ujson.Value = {
    // Validate array constraints before serialization
    validateArrayConstraints(list, arrayChez) match {
      case Nil => // No validation errors
        ujson.Arr(list.map(item => writeJs(item))*)
      case errors =>
        throw new RuntimeException(s"Array validation failed: ${errors.map(_.toString).mkString(", ")}")
    }
  }
  
  /**
   * Read a List[T] from JSON using the array schema
   */
  private def readArrayFromJson[T](json: ujson.Value, arrayChez: ArrayChez[?])(using itemReadWriter: ReadWriter[T]): List[T] = {
    json match {
      case arr: ujson.Arr =>
        try {
          val items = arr.arr.map(itemJson => read[T](itemJson)).toList
          
          // Validate the resulting array
          validateArrayConstraints(items, arrayChez) match {
            case Nil => items
            case errors =>
              throw new RuntimeException(s"Array validation failed: ${errors.map(_.toString).mkString(", ")}")
          }
        } catch {
          case e: Exception =>
            throw new RuntimeException(s"Failed to deserialize array items: ${e.getMessage}", e)
        }
        
      case _ =>
        throw new RuntimeException(s"Expected JSON array, got ${json.getClass.getSimpleName}")
    }
  }
  
  /**
   * Validate array constraints (minItems, maxItems, uniqueItems)
   */
  private def validateArrayConstraints[T](items: List[T], arrayChez: ArrayChez[?]): List[String] = {
    val errors = scala.collection.mutable.ListBuffer[String]()
    
    // Min items validation
    arrayChez.minItems.foreach { min =>
      if (items.length < min) {
        errors += s"Array has ${items.length} items, minimum required: $min"
      }
    }
    
    // Max items validation
    arrayChez.maxItems.foreach { max =>
      if (items.length > max) {
        errors += s"Array has ${items.length} items, maximum allowed: $max"
      }
    }
    
    // Unique items validation
    arrayChez.uniqueItems.foreach { unique =>
      if (unique && items.distinct.length != items.length) {
        errors += s"Array contains duplicate items but uniqueItems=true"
      }
    }
    
    errors.toList
  }
  
  /**
   * Generate ReadWriter for ArrayChez using recursive schema analysis
   */
  def deriveArrayReadWriterRecursive(arrayChez: ArrayChez[?]): ReadWriter[List[Any]] = {
    readwriter[ujson.Value].bimap[List[Any]](
      // Writer: List[Any] -> ujson.Value
      list => {
        validateArrayConstraints(list, arrayChez) match {
          case Nil => 
            ujson.Arr(list.map(item => writeValueWithSchema(item, arrayChez.items))*)
          case errors =>
            throw new RuntimeException(s"Array validation failed: ${errors.mkString(", ")}")
        }
      },
      // Reader: ujson.Value -> List[Any]
      json => json match {
        case arr: ujson.Arr =>
          val items = arr.arr.map(itemJson => readValueWithSchema(itemJson, arrayChez.items)).toList
          validateArrayConstraints(items, arrayChez) match {
            case Nil => items
            case errors =>
              throw new RuntimeException(s"Array validation failed: ${errors.mkString(", ")}")
          }
        case _ =>
          throw new RuntimeException(s"Expected JSON array, got ${json.getClass.getSimpleName}")
      }
    )
  }
  
  /**
   * Write a value using a specific Chez schema (recursive helper)
   */
  private def writeValueWithSchema(value: Any, schema: Chez): ujson.Value = {
    // Delegate to ObjectReadWriter for consistency
    ObjectReadWriter.writeValueWithSchema(value, schema)
  }
  
  /**
   * Read a value using a specific Chez schema (recursive helper)
   */
  private def readValueWithSchema(json: ujson.Value, schema: Chez): Any = {
    // Delegate to ObjectReadWriter for consistency
    ObjectReadWriter.readValueWithSchema(json, schema)
  }
}