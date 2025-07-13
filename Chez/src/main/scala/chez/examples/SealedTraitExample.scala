package chez.examples

import chez.*
import chez.derivation.{Schema, CollectionSchemas}
import chez.derivation.CollectionSchemas.given
import scala.util.{Try, Success, Failure}

/**
 * Comprehensive examples demonstrating sealed trait schema derivation
 * with automatic discriminated union generation using JSON Schema 2020-12 oneOf.
 * 
 * Sealed traits in Scala represent algebraic data types that are automatically
 * transformed into discriminated unions with type discriminator fields.
 */

// ============================================================================
// 1. BASIC SEALED TRAITS - Simple discriminated unions
// ============================================================================

// Basic geometric shapes - demonstrates fundamental discriminated union pattern
sealed trait Shape derives Schema
case class Circle(radius: Double) extends Shape derives Schema
case class Rectangle(width: Double, height: Double) extends Shape derives Schema
case class Triangle(base: Double, height: Double) extends Shape derives Schema

// ============================================================================
// 2. SEALED TRAITS WITH OPTIONAL FIELDS
// ============================================================================

// Vehicle hierarchy with optional and mixed field types
sealed trait Vehicle derives Schema
case class Car(make: String, model: String, year: Option[Int]) extends Vehicle derives Schema
case class Bicycle(brand: String, gears: Int) extends Vehicle derives Schema
case class Boat(name: String, length: Double, hasMotor: Boolean) extends Vehicle derives Schema

// ============================================================================
// 3. NESTED AND COMPLEX SEALED TRAITS
// ============================================================================

// Nested sealed traits - sealed traits containing other sealed traits
sealed trait Transport derives Schema
case class LandTransport(vehicle: Vehicle) extends Transport derives Schema
case class WaterTransport(boat: Boat) extends Transport derives Schema
case class AirTransport(aircraft: String, capacity: Int) extends Transport derives Schema

// ============================================================================
// 4. PARAMETRIC SEALED TRAITS
// ============================================================================

// Generic sealed trait with type parameters
sealed trait Container[T] derives Schema
case class Box[T](contents: T, size: String) extends Container[T] derives Schema
case class Bag[T](items: List[T], material: String) extends Container[T] derives Schema

// ============================================================================
// 5. EDGE CASES AND SPECIAL SCENARIOS
// ============================================================================

// Single variant sealed trait
sealed trait SingleOption derives Schema
case class OnlyChoice(value: String) extends SingleOption derives Schema

// Empty case class in sealed trait
sealed trait WithEmpty derives Schema
case class EmptyCase() extends WithEmpty derives Schema
case class NonEmptyCase(data: String) extends WithEmpty derives Schema

// Mixed complexity sealed trait
sealed trait MixedComplexity derives Schema
case class SimpleVariant(id: Int) extends MixedComplexity derives Schema
case class ComplexVariant(metadata: Map[String, String], tags: List[String], active: Boolean) extends MixedComplexity derives Schema

object SealedTraitExample {
  def main(args: Array[String]): Unit = {
    println("🔍 Chez Sealed Trait Schema Derivation - Discriminated Union Examples")
    println("=" * 75)
    println("Demonstrates automatic oneOf schema generation with type discriminators")
    println("for Scala sealed traits using JSON Schema 2020-12 specification.")
    println()

    demonstrateBasicSealedTraits()
    demonstrateOptionalFields()
    demonstrateNestedSealedTraits()
    demonstrateParametricSealedTraits()
    demonstrateEdgeCases()
    demonstrateIndividualCaseClasses()
    demonstrateValidationAndSerialization()
    
    println("\n🎉 All sealed trait examples completed successfully!")
    println("=" * 75)
  }

  def demonstrateBasicSealedTraits(): Unit = {
    println("\n📐 Section 1: Basic Sealed Trait Discriminated Unions")
    println("-" * 55)
    println("Sealed traits automatically generate oneOf schemas with type discriminators.")
    println()

    val shapeSchema = summon[Schema[Shape]]
    val shapeJson = shapeSchema.schema.toJsonSchema
    
    println("🔹 Shape sealed trait schema:")
    println(ujson.write(shapeJson, indent = 2))
    println()

    // Verify discriminated union structure
    if (shapeJson.obj.contains("oneOf")) {
      val variants = shapeJson("oneOf").arr
      println(s"✅ SUCCESS: Generated oneOf with ${variants.length} variants (Circle, Rectangle, Triangle)")
      
      // Verify each variant has type discriminator
      val hasTypeDiscriminators = variants.forall { variant =>
        variant.obj.contains("properties") && 
        variant("properties").obj.contains("type") &&
        variant("properties")("type").obj.contains("const")
      }
      
      if (hasTypeDiscriminators) {
        println("✅ SUCCESS: All variants have type discriminator fields")
      } else {
        println("❌ ISSUE: Missing type discriminators in some variants")
      }
    } else {
      println("❌ ISSUE: Sealed trait did not generate oneOf schema")
    }
  }

  def demonstrateOptionalFields(): Unit = {
    println("\n🚗 Section 2: Sealed Traits with Optional Fields")
    println("-" * 48)
    println("Demonstrates how optional fields are handled in discriminated unions.")
    println()

    val vehicleSchema = summon[Schema[Vehicle]]
    val vehicleJson = vehicleSchema.schema.toJsonSchema
    
    println("🔹 Vehicle sealed trait schema:")
    println(ujson.write(vehicleJson, indent = 2))
    println()

    if (vehicleJson.obj.contains("oneOf")) {
      val variants = vehicleJson("oneOf").arr
      println(s"✅ SUCCESS: Generated oneOf with ${variants.length} vehicle variants")
      
      // Check for Car variant with optional year field
      val carVariant = variants.find { variant =>
        variant("properties").obj.contains("year") && 
        variant("properties")("type")("const").str == "Car"
      }
      
      carVariant match {
        case Some(car) =>
          val required = car("required").arr.map(_.str).toSet
          if (!required.contains("year")) {
            println("✅ SUCCESS: Optional 'year' field correctly excluded from required")
          } else {
            println("❌ ISSUE: Optional 'year' field incorrectly marked as required")
          }
        case None =>
          println("❌ ISSUE: Could not find Car variant in schema")
      }
    }
  }

  def demonstrateNestedSealedTraits(): Unit = {
    println("\n🔗 Section 3: Nested Sealed Trait Hierarchies")
    println("-" * 42)
    println("Shows how sealed traits containing other sealed traits are handled.")
    println()

    val transportSchema = summon[Schema[Transport]]
    val transportJson = transportSchema.schema.toJsonSchema
    
    println("🔹 Transport sealed trait with nested Vehicle:")
    println(ujson.write(transportJson, indent = 2))
    println()

    if (transportJson.obj.contains("oneOf")) {
      val variants = transportJson("oneOf").arr
      println(s"✅ SUCCESS: Generated oneOf with ${variants.length} transport variants")
      
      // Check for nested Vehicle schema in LandTransport
      val landTransportVariant = variants.find { variant =>
        variant("properties").obj.contains("vehicle") &&
        variant("properties")("type")("const").str == "LandTransport"
      }
      
      landTransportVariant match {
        case Some(land) =>
          val vehicleField = land("properties")("vehicle")
          if (vehicleField.obj.contains("oneOf")) {
            println("✅ SUCCESS: Nested Vehicle sealed trait properly generates oneOf schema")
          } else {
            println("❌ ISSUE: Nested Vehicle sealed trait schema incorrect")
          }
        case None =>
          println("❌ ISSUE: Could not find LandTransport variant")
      }
    }
  }

  def demonstrateParametricSealedTraits(): Unit = {
    println("\n📦 Section 4: Parametric Sealed Traits (Generic Types)")
    println("-" * 52)
    println("Demonstrates sealed traits with type parameters.")
    println()

    // Example with String containers
    val stringContainerSchema = summon[Schema[Container[String]]]
    val stringContainerJson = stringContainerSchema.schema.toJsonSchema
    
    println("🔹 Container[String] sealed trait schema:")
    println(ujson.write(stringContainerJson, indent = 2))
    println()

    if (stringContainerJson.obj.contains("oneOf")) {
      val variants = stringContainerJson("oneOf").arr
      println(s"✅ SUCCESS: Parametric sealed trait generated oneOf with ${variants.length} variants")
    }
  }

  def demonstrateEdgeCases(): Unit = {
    println("\n🧪 Section 5: Edge Cases and Special Scenarios")
    println("-" * 44)
    println("Tests challenging scenarios: single variants, empty case classes, etc.")
    println()

    // Single variant sealed trait
    println("🔸 Single variant sealed trait:")
    val singleSchema = summon[Schema[SingleOption]]
    val singleJson = singleSchema.schema.toJsonSchema
    println(ujson.write(singleJson, indent = 2))
    
    if (singleJson.obj.contains("oneOf") && singleJson("oneOf").arr.length == 1) {
      println("✅ SUCCESS: Single variant correctly generates oneOf with 1 element")
    }
    println()

    // Empty case class scenario
    println("🔸 Sealed trait with empty case class:")
    val emptySchema = summon[Schema[WithEmpty]]
    val emptyJson = emptySchema.schema.toJsonSchema
    println(ujson.write(emptyJson, indent = 2))
    
    if (emptyJson.obj.contains("oneOf")) {
      val variants = emptyJson("oneOf").arr
      val emptyVariant = variants.find { variant =>
        variant("properties")("type")("const").str == "EmptyCase"
      }
      
      emptyVariant match {
        case Some(empty) =>
          val props = empty("properties").obj
          if (props.size == 1 && props.contains("type")) {
            println("✅ SUCCESS: Empty case class correctly has only type discriminator")
          }
        case None =>
          println("❌ ISSUE: Could not find EmptyCase variant")
      }
    }
    println()
  }

  def demonstrateIndividualCaseClasses(): Unit = {
    println("\n🎯 Section 6: Individual Case Class Schemas")
    println("-" * 41)
    println("Shows difference between sealed trait variants and standalone case classes.")
    println()

    val circleSchema = summon[Schema[Circle]]
    val circleJson = circleSchema.schema.toJsonSchema
    
    println("🔹 Individual Circle case class schema:")
    println(ujson.write(circleJson, indent = 2))
    println()

    if (circleJson("type").str == "object" && !circleJson("properties").obj.contains("type")) {
      println("✅ SUCCESS: Individual case class generates object schema without type discriminator")
    } else {
      println("❌ ISSUE: Individual case class schema structure incorrect")
    }
  }

  def demonstrateValidationAndSerialization(): Unit = {
    println("\n🔬 Section 7: Validation and Serialization Examples")
    println("-" * 49)
    println("Demonstrates runtime behavior and JSON serialization.")
    println()

    // Create actual instances
    val circle = Circle(5.0)
    val rectangle = Rectangle(10.0, 20.0)
    val car = Car("Toyota", "Camry", Some(2023))
    
    println("🔹 Sample sealed trait instances:")
    println(s"Circle: $circle")
    println(s"Rectangle: $rectangle")
    println(s"Car: $car")
    println()

    // Demonstrate schemas can be summoned for instances
    Try {
      val circleSchema = summon[Schema[Circle]]
      val rectangleSchema = summon[Schema[Rectangle]] 
      val carSchema = summon[Schema[Car]]
      
      println("✅ SUCCESS: All case class schemas derived successfully")
      
      // Verify sealed trait schemas
      val shapeSchema = summon[Schema[Shape]]
      val vehicleSchema = summon[Schema[Vehicle]]
      
      println("✅ SUCCESS: All sealed trait schemas derived successfully")
      
    } match {
      case Success(_) => println("✅ SUCCESS: Schema derivation working correctly")
      case Failure(e) => println(s"❌ FAILURE: Schema derivation failed: ${e.getMessage}")
    }
  }
}