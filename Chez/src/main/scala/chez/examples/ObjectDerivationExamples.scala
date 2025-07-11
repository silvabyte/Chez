package chez.examples

import upickle.default.*
import chez.*
import chez.complex.ObjectChez
import chez.primitives.*
import chez.derivation.*
import scala.util.{Try, Success, Failure}

/**
 * Examples demonstrating automatic ReadWriter derivation for ObjectChez schemas
 * 
 * This showcases Batch 2 implementation: Object Schema Analysis with Map[String, Any] representation
 */
object ObjectDerivationExamples {

  def runAllExamples(): Unit = {
    println("🧪 ObjectChez ReadWriter Derivation Examples")
    println("=" * 50)
    
    simpleObjectExample()
    nestedObjectExample() 
    validationErrorExample()
    roundtripTestExample()
    
    println("\n✅ All object ReadWriter derivation examples completed!")
  }
  
  /**
   * Example 1: Simple object with basic fields
   */
  def simpleObjectExample(): Unit = {
    println("\n📋 Example 1: Simple User Object")
    println("-" * 30)
    
    // Define user schema
    val userSchema = Chez.Object(
      properties = Map(
        "name" -> Chez.String(minLength = Some(1), maxLength = Some(100)),
        "email" -> Chez.String(format = Some("email")),
        "age" -> Chez.Integer(minimum = Some(0), maximum = Some(150))
      ),
      required = Set("name", "email")
    )
    
    // Derive ReadWriter automatically
    given userReadWriter: ReadWriter[Map[String, Any]] = userSchema.deriveReadWriter[Map[String, Any]]
    
    // Create test data
    val userData = Map[String, Any](
      "name" -> "Alice Smith",
      "email" -> "alice@example.com", 
      "age" -> 28
    )
    
    println(s"Schema: $userSchema")
    println(s"Data: $userData")
    
    // Serialize to JSON
    val json = writeJs(userData)
    println(s"JSON: $json")
    
    // Deserialize back
    val roundtrip = upickle.default.read[Map[String, Any]](json)
    println(s"Roundtrip: $roundtrip")
    println(s"Match: ${userData == roundtrip}")
  }
  
  /**
   * Example 2: Nested object with address schema
   */
  def nestedObjectExample(): Unit = {
    println("\n🏠 Example 2: Nested Address Object")
    println("-" * 35)
    
    // Define address schema
    val addressSchema = Chez.Object(
      properties = Map(
        "street" -> Chez.String(),
        "city" -> Chez.String(),
        "zipCode" -> Chez.String(pattern = Some("\\d{5}"))
      ),
      required = Set("street", "city")
    )
    
    // Define person schema with nested address
    val personSchema = Chez.Object(
      properties = Map(
        "name" -> Chez.String(),
        "address" -> addressSchema
      ),
      required = Set("name", "address")
    )
    
    given personReadWriter: ReadWriter[Map[String, Any]] = personSchema.deriveReadWriter[Map[String, Any]]
    
    // Create nested test data
    val personData = Map[String, Any](
      "name" -> "Bob Johnson",
      "address" -> Map[String, Any](
        "street" -> "123 Main St",
        "city" -> "Springfield",
        "zipCode" -> "12345"
      )
    )
    
    println(s"Nested Data: $personData")
    
    // Test serialization/deserialization
    val json = writeJs(personData)
    println(s"JSON: $json")
    
    val roundtrip = upickle.default.read[Map[String, Any]](json)
    println(s"Roundtrip: $roundtrip")
    println(s"Match: ${personData == roundtrip}")
  }
  
  /**
   * Example 3: Validation error handling
   */
  def validationErrorExample(): Unit = {
    println("\n❌ Example 3: Validation Error Handling")
    println("-" * 40)
    
    val strictSchema = Chez.Object(
      properties = Map(
        "id" -> Chez.Integer(minimum = Some(1)),
        "name" -> Chez.String(minLength = Some(2))
      ),
      required = Set("id", "name")
    )
    
    given strictReadWriter: ReadWriter[Map[String, Any]] = strictSchema.deriveReadWriter[Map[String, Any]]
    
    // Valid data
    val validData = Map[String, Any](
      "id" -> 42,
      "name" -> "Valid Name"
    )
    
    println("✅ Valid data:")
    println(s"Data: $validData")
    try {
      val json = writeJs(validData)
      val roundtrip = upickle.default.read[Map[String, Any]](json)
      println(s"Success: $roundtrip")
    } catch {
      case e: Exception => println(s"Error: ${e.getMessage}")
    }
    
    // Invalid data - missing required field
    val invalidData = Map[String, Any](
      "name" -> "No ID provided"
    )
    
    println("\n❌ Invalid data (missing required field):")
    println(s"Data: $invalidData")
    try {
      val json = writeJs(invalidData)
      println(s"JSON: $json")
      // This will work for serialization but would fail validation if we add it
    } catch {
      case e: Exception => println(s"Serialization Error: ${e.getMessage}")
    }
  }
  
  /**
   * Example 4: Comprehensive roundtrip testing
   */
  def roundtripTestExample(): Unit = {
    println("\n🔄 Example 4: Roundtrip Testing")
    println("-" * 32)
    
    val complexSchema = Chez.Object(
      properties = Map(
        "stringField" -> Chez.String(),
        "numberField" -> Chez.Number(),
        "integerField" -> Chez.Integer(),
        "booleanField" -> Chez.Boolean(),
        "optionalField" -> Chez.String().optional
      ),
      required = Set("stringField", "numberField", "integerField", "booleanField")
    )
    
    given complexReadWriter: ReadWriter[Map[String, Any]] = complexSchema.deriveReadWriter[Map[String, Any]]
    
    val testCases = List(
      scala.collection.immutable.Map[String, Any](
        "stringField" -> "test string",
        "numberField" -> 3.14,
        "integerField" -> 42,
        "booleanField" -> true,
        "optionalField" -> "present"
      ),
      scala.collection.immutable.Map[String, Any](
        "stringField" -> "another test",
        "numberField" -> 2.71,
        "integerField" -> 100,
        "booleanField" -> false
        // optionalField omitted
      ),
      scala.collection.immutable.Map[String, Any](
        "stringField" -> "",
        "numberField" -> 0.0,
        "integerField" -> 0,
        "booleanField" -> false,
        "optionalField" -> ""
      )
    )
    
    testCases.zipWithIndex.foreach { case (testData, index) =>
      println(s"\nTest case ${index + 1}:")
      println(s"Original: $testData")
      
      try {
        val json = writeJs(testData)
        val roundtrip = upickle.default.read[Map[String, Any]](json)
        
        // Deep equality check for Map contents
        val isMatch = testData.size == roundtrip.size && 
                     testData.forall { case (k, v) => 
                       roundtrip.get(k).exists(_ == v) 
                     }
        
        println(s"JSON: $json")
        println(s"Roundtrip: $roundtrip") 
        println(s"Match: $isMatch")
        
        if (!isMatch) {
          println("❌ ROUNDTRIP FAILED!")
          // Show differences
          testData.foreach { case (k, v) =>
            roundtrip.get(k) match {
              case Some(rv) if rv != v => 
                println(s"  Field '$k': expected $v (${v.getClass.getSimpleName}), got $rv (${rv.getClass.getSimpleName})")
              case None => 
                println(s"  Field '$k': missing in roundtrip")
              case _ => // matches
            }
          }
        } else {
          println("✅ Roundtrip successful")
        }
      } catch {
        case e: Exception => 
          println(s"❌ Error: ${e.getMessage}")
      }
    }
  }
}

/**
 * Main method for running examples standalone
 */
@main def runObjectExamples(): Unit = {
  ObjectDerivationExamples.runAllExamples()
}