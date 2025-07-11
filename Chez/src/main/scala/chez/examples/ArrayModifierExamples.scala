package chez.examples

import upickle.default.*
import chez.*
import chez.complex.{ArrayChez, ObjectChez}
import chez.primitives.*
import chez.derivation.*

/**
 * Examples demonstrating automatic ReadWriter derivation for ArrayChez and modifier types
 * 
 * This showcases Batch 3 implementation: Array and Modifier Handling
 */
object ArrayModifierExamples {

  def runAllExamples(): Unit = {
    println("🧪 Array and Modifier ReadWriter Derivation Examples")
    println("=" * 55)
    
    simpleArrayExample()
    integerArrayExample()
    validationExample()
    
    println("\n✅ All array and modifier ReadWriter derivation examples completed!")
  }
  
  /**
   * Example 1: Simple array via recursive derivation
   */
  def simpleArrayExample(): Unit = {
    println("\n📚 Example 1: Simple String Array (Recursive)")
    println("-" * 45)
    
    // Use the recursive array ReadWriter that works with Any
    val stringArraySchema = Chez.Array(Chez.String())
    
    // Create test data
    val stringArray = List("apple", "banana", "cherry")
    
    println(s"Schema: Array of String")
    println(s"Data: $stringArray")
    
    // Test the recursive ReadWriter directly
    val arrayRW = ArrayReadWriter.deriveArrayReadWriterRecursive(stringArraySchema)
    
    // Serialize to JSON
    val json = writeJs(stringArray)(using arrayRW)
    println(s"JSON: $json")
    
    // Deserialize back
    val roundtrip = read[List[Any]](json)(using arrayRW)
    println(s"Roundtrip: $roundtrip")
    println(s"Match: ${stringArray == roundtrip}")
  }
  
  /**
   * Example 2: Integer array to demonstrate type handling
   */
  def integerArrayExample(): Unit = {
    println("\n🔢 Example 2: Integer Array")
    println("-" * 28)
    
    // Define integer array schema
    val intArraySchema = Chez.Array(Chez.Integer())
    
    // Test the recursive ReadWriter directly
    val arrayRW = ArrayReadWriter.deriveArrayReadWriterRecursive(intArraySchema)
    
    // Create test data
    val intArray = List(1, 2, 3, 42, 100)
    
    println(s"Schema: Array of Integer")
    println(s"Data: $intArray")
    
    // Serialize to JSON
    val json = writeJs(intArray)(using arrayRW)
    println(s"JSON: $json")
    
    // Deserialize back
    val roundtrip = read[List[Any]](json)(using arrayRW)
    println(s"Roundtrip: $roundtrip")
    println(s"Match: ${intArray == roundtrip}")
  }
  
  /**
   * Example 3: Array validation constraints
   */
  def validationExample(): Unit = {
    println("\n✅ Example 3: Array Validation")
    println("-" * 32)
    
    // Define validated array schema
    val validatedArraySchema = Chez.Array(
      items = Chez.String(),
      minItems = Some(2),
      maxItems = Some(4),
      uniqueItems = Some(true)
    )
    
    val arrayRW = ArrayReadWriter.deriveArrayReadWriterRecursive(validatedArraySchema)
    
    // Valid data
    val validData = List("red", "green", "blue")
    println("✅ Valid data (3 unique items within limits):")
    println(s"Data: $validData")
    try {
      val json = writeJs(validData)(using arrayRW)
      val roundtrip = read[List[Any]](json)(using arrayRW)
      println(s"JSON: $json")
      println(s"Success: $roundtrip")
    } catch {
      case e: Exception => println(s"Error: ${e.getMessage}")
    }
    
    // Invalid data - too few items
    val tooFewData = List("red")
    println("\n❌ Invalid data (too few items):")
    println(s"Data: $tooFewData")
    try {
      val json = writeJs(tooFewData)(using arrayRW)
      println(s"Unexpected success: $json")
    } catch {
      case e: Exception => println(s"Expected error: ${e.getMessage}")
    }
    
    // Invalid data - duplicate items
    val duplicateData = List("red", "green", "red")
    println("\n❌ Invalid data (duplicate items):")
    println(s"Data: $duplicateData")
    try {
      val json = writeJs(duplicateData)(using arrayRW)
      println(s"Unexpected success: $json")
    } catch {
      case e: Exception => println(s"Expected error: ${e.getMessage}")
    }
  }
}

/**
 * Main method for running examples standalone
 */
@main def runArrayModifierExamples(): Unit = {
  ArrayModifierExamples.runAllExamples()
}