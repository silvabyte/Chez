package chez.examples

import chez.*
import chez.derivation.Schema

/**
 * Example demonstrating Scala case class default parameter detection
 * Tests that fields with default values are not marked as required in JSON Schema
 */

// Test case class with all default parameters
case class AllDefaults(
    name: String = "default_name",
    age: Int = 25,
    active: Boolean = true,
    score: Double = 0.0
) derives Schema

// Test case class with mixed required, optional, and default parameters
case class MixedFields(
    requiredField: String, // Required (no default, not Option)
    optionalField: Option[String], // Not required (Option type)
    defaultField: String = "default_value", // Not required (has default)
    defaultInt: Int = 42 // Not required (has default)
) derives Schema

// Test case class with enum defaults (from existing passing test)
enum Color derives Schema {
  case Red, Green, Blue
}

case class WithEnumDefaults(
    theme: Color = Color.Blue,
    priority: Int = 1
) derives Schema

object DefaultParameterTest {
  def main(args: Array[String]): Unit = {
    println("🧪 Testing Scala Case Class Default Parameter Detection")
    println("=" * 60)

    // Test 1: All defaults case class
    println("\n1. Testing case class with all default parameters:")
    testSchema[AllDefaults]("AllDefaults")

    // Test 2: Mixed fields case class
    println("\n2. Testing case class with mixed field types:")
    testSchema[MixedFields]("MixedFields")

    // Test 3: Enum defaults (known working case)
    println("\n3. Testing case class with enum defaults:")
    testSchema[WithEnumDefaults]("WithEnumDefaults")

    println("\n✅ Default parameter detection test completed!")
  }

  def testSchema[T](name: String)(using schema: Schema[T]): Unit = {
    val json = schema.schema.toJsonSchema

    println(s"📋 Schema for $name:")
    println(ujson.write(json, indent = 2))

    val requiredFields = if (json.obj.contains("required")) {
      json("required").arr.map(_.str).toSet
    } else {
      Set.empty[String]
    }

    println(s"🔍 Required fields: $requiredFields")

    // Analysis based on expected behavior
    name match {
      case "AllDefaults" =>
        val expected = Set.empty[String]
        val correct = requiredFields == expected
        println(s"✓ Expected: $expected, Got: $requiredFields, Correct: $correct")

      case "MixedFields" =>
        val expected = Set("requiredField")
        val correct = requiredFields == expected
        println(s"✓ Expected: $expected, Got: $requiredFields, Correct: $correct")

      case "WithEnumDefaults" =>
        val expected = Set.empty[String]
        val correct = requiredFields == expected
        println(s"✓ Expected: $expected, Got: $requiredFields, Correct: $correct")

      case _ =>
        println(s"📊 Analysis: Fields marked as required: $requiredFields")
    }

    println()
  }
}
