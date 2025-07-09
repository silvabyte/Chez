package chez.examples

import chez.*
import chez.primitives.*
import upickle.default.*
import scala.util.{Try, Success, Failure}

/**
 * Schema validation examples demonstrating the validation capabilities of Chez
 */
object Validation {

  def main(args: Array[String]): Unit = {
    println("🔍 Chez Validation - Schema Validation Examples!")
    println("=" * 55)

    // 1. Basic primitive validation
    println("\n1. Basic Primitive Validation:")

    val emailSchema = Chez.String(format = Some("email"))
    val ageSchema = Chez.Integer(minimum = Some(0), maximum = Some(25))
    val passwordSchema = Chez.String(
      minLength = Some(8),
      pattern = Some("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)")
    )

    // Test valid values
    println("✓ Valid Values:")
    testStringValidation(emailSchema, "john@example.com", "valid email")
    testIntegerValidation(ageSchema, 25, "valid age")
    testStringValidation(passwordSchema, "Password123", "valid password")

    // Test invalid values
    println("\n✗ Invalid Values:")
    testStringValidation(emailSchema, "not-an-email", "invalid email")
    testIntegerValidation(ageSchema, -5, "negative age")
    testIntegerValidation(ageSchema, 200, "age too high")
    testStringValidation(passwordSchema, "weak", "weak password")

    // 2. Object validation with required fields
    println("\n2. Object Validation with Required Fields:")

    val userSchema = Chez.Object(
      properties = Map(
        "id" -> Chez.String(),
        "name" -> Chez.String(minLength = Some(1)),
        "email" -> Chez.String(format = Some("email")),
        "age" -> Chez.Integer(minimum = Some(0))
      ),
      required = Set("id", "name", "email")
    )

    // Valid object
    val validUser = ujson.Obj(
      "id" -> ujson.Str("123"),
      "name" -> ujson.Str("John Doe"),
      "email" -> ujson.Str("john@example.com"),
      "age" -> ujson.Num(30)
    )

    // Invalid object (missing required field)
    val invalidUser = ujson.Obj(
      "id" -> ujson.Str("123"),
      "name" -> ujson.Str("John Doe")
      // Missing required email field
    )

    testObjectValidation(userSchema, validUser, "valid user")
    testObjectValidation(userSchema, invalidUser, "user missing required field")

    // 3. Array validation
    println("\n3. Array Validation:")

    val stringArraySchema = Chez.Array(
      Chez.String(),
      minItems = Some(1),
      maxItems = Some(5),
      uniqueItems = Some(true)
    )

    val validArray = ujson.Arr(ujson.Str("apple"), ujson.Str("banana"), ujson.Str("cherry"))
    val emptyArray = ujson.Arr()
    val tooLongArray = ujson.Arr(
      ujson.Str("a"),
      ujson.Str("b"),
      ujson.Str("c"),
      ujson.Str("d"),
      ujson.Str("e"),
      ujson.Str("f")
    )
    val nonUniqueArray = ujson.Arr(ujson.Str("apple"), ujson.Str("apple"))

    testArrayValidation(stringArraySchema, validArray, "valid array")
    testArrayValidation(stringArraySchema, emptyArray, "empty array")
    testArrayValidation(stringArraySchema, tooLongArray, "array too long")
    testArrayValidation(stringArraySchema, nonUniqueArray, "non-unique array")

    // 4. Number validation with ranges
    println("\n4. Number Validation with Ranges:")

    val priceSchema = Chez.Number(
      minimum = Some(0.01),
      maximum = Some(999.99),
      multipleOf = Some(0.01)
    )

    testNumberValidation(priceSchema, 19.99, "valid price")
    testNumberValidation(priceSchema, 0.00, "price too low")
    testNumberValidation(priceSchema, 1000.00, "price too high")
    testNumberValidation(priceSchema, 19.999, "price not multiple of 0.01")

    // 5. String pattern validation
    println("\n5. String Pattern Validation:")

    val phoneSchema = Chez.String(pattern = Some("^\\+?[1-9]\\d{1,14}$"))
    val uuidSchema = Chez.String(format = Some("uuid"))
    val dateSchema = Chez.String(format = Some("date"))

    testStringValidation(phoneSchema, "+1234567890", "valid phone")
    testStringValidation(phoneSchema, "invalid-phone", "invalid phone")
    testStringValidation(uuidSchema, "550e8400-e29b-41d4-a716-446655440000", "valid UUID")
    testStringValidation(uuidSchema, "not-a-uuid", "invalid UUID")
    testStringValidation(dateSchema, "2023-12-25", "valid date")
    testStringValidation(dateSchema, "2023-13-25", "invalid date")

    // 6. Enum validation
    println("\n6. Enum Validation:")

    val statusSchema = Chez.String(enumValues = Some(List("active", "inactive", "pending")))
    val prioritySchema = Chez.Integer(enumValues = Some(List(1, 2, 3, 4, 5)))

    testStringValidation(statusSchema, "active", "valid status")
    testStringValidation(statusSchema, "unknown", "invalid status")
    testIntegerValidation(prioritySchema, 3, "valid priority")
    testIntegerValidation(prioritySchema, 10, "invalid priority")

    // 7. Nullable and optional validation
    println("\n7. Nullable and Optional Validation:")

    val optionalSchema = Chez.String().optional
    val nullableSchema = Chez.String().nullable
    val optionalNullableSchema = Chez.String().optional.nullable

    println("Optional field validation:")
    println(s"- Schema: ${optionalSchema.toJsonSchema}")
    println("- Optional fields can be missing from JSON objects")

    println("\nNullable field validation:")
    println(s"- Schema: ${nullableSchema.toJsonSchema}")
    println("- Nullable fields must be present but can be null")

    println("\nOptional nullable field validation:")
    println(s"- Schema: ${optionalNullableSchema.toJsonSchema}")
    println("- Can be missing from JSON OR present as null")

    // 8. Composition validation
    println("\n8. Composition Validation:")

    val stringOrNumberSchema = Chez.OneOf(
      Chez.String(),
      Chez.Number()
    )

    val personAndEmployeeSchema = Chez.AllOf(
      Chez.Object("name" -> Chez.String(), "age" -> Chez.Integer()),
      Chez.Object("employeeId" -> Chez.String(), "department" -> Chez.String())
    )

    val notStringSchema = Chez.Not(Chez.String())

    println("OneOf validation:")
    println(s"- Schema: ${stringOrNumberSchema.toJsonSchema}")
    println("- Must match exactly one of the schemas")

    println("\nAllOf validation:")
    println(s"- Schema: ${personAndEmployeeSchema.toJsonSchema}")
    println("- Must match all of the schemas")

    println("\nNot validation:")
    println(s"- Schema: ${notStringSchema.toJsonSchema}")
    println("- Must NOT match the schema")

    // 9. Conditional validation
    println("\n9. Conditional Validation:")

    val conditionalUserSchema = Chez.If(
      condition = Chez.Object("role" -> Chez.String(const = Some("admin"))),
      thenSchema = Chez.Object("permissions" -> Chez.Array(Chez.String())),
      elseSchema = Chez.Object("department" -> Chez.String())
    )

    val adminUser = ujson.Obj(
      "role" -> ujson.Str("admin"),
      "permissions" -> ujson.Arr(ujson.Str("read"), ujson.Str("write"))
    )

    val regularUser = ujson.Obj(
      "role" -> ujson.Str("user"),
      "department" -> ujson.Str("Engineering")
    )

    println("Conditional validation:")
    println(s"- Schema: ${conditionalUserSchema.toJsonSchema}")
    println("- Admin user validation would check for permissions")
    println("- Regular user validation would check for department")

    // 10. Real-world API validation example
    println("\n10. Real-World API Validation Example:")

    val createProductSchema = Chez.Object(
      properties = Map(
        "name" -> Chez.String(minLength = Some(1), maxLength = Some(100)),
        "description" -> Chez.String(maxLength = Some(1000)).optional,
        "price" -> Chez.Number(minimum = Some(0.01), multipleOf = Some(0.01)),
        "category" -> Chez.String(enumValues = Some(List("electronics", "clothing", "books", "food"))),
        "sku" -> Chez.String(pattern = Some("^[A-Z]{3}-\\d{6}$")),
        "inStock" -> Chez.Boolean(),
        "tags" -> Chez.Array(Chez.String(), maxItems = Some(10)).optional,
        "metadata" -> Chez
          .Object(
            Map(),
            patternProperties = Map(
              "^[a-zA-Z_][a-zA-Z0-9_]*$" -> Chez.String()
            )
          )
          .optional
      ),
      required = Set("name", "price", "category", "sku", "inStock")
    )

    val validProduct = ujson.Obj(
      "name" -> ujson.Str("Wireless Headphones"),
      "description" -> ujson.Str("High-quality wireless headphones with noise cancellation"),
      "price" -> ujson.Num(99.99),
      "category" -> ujson.Str("electronics"),
      "sku" -> ujson.Str("ELE-123456"),
      "inStock" -> ujson.Bool(true),
      "tags" -> ujson.Arr(ujson.Str("wireless"), ujson.Str("bluetooth"), ujson.Str("headphones")),
      "metadata" -> ujson.Obj(
        "brand" -> ujson.Str("TechCorp"),
        "model" -> ujson.Str("WH-1000XM4")
      )
    )

    val invalidProduct = ujson.Obj(
      "name" -> ujson.Str(""), // Too short
      "price" -> ujson.Num(-10.00), // Negative price
      "category" -> ujson.Str("invalid-category"), // Not in enum
      "sku" -> ujson.Str("INVALID"), // Wrong pattern
      "inStock" -> ujson.Bool(true)
    )

    testObjectValidation(createProductSchema, validProduct, "valid product")
    testObjectValidation(createProductSchema, invalidProduct, "invalid product")

    println("\n🎯 Validation Examples Complete!")
    println("These examples demonstrate comprehensive validation capabilities:")
    println("- Primitive type validation (string, number, integer, boolean)")
    println("- Object validation with required fields")
    println("- Array validation with constraints")
    println("- Number range and multiple validation")
    println("- String pattern and format validation")
    println("- Enum validation")
    println("- Nullable and optional field handling")
    println("- Composition validation (anyOf, oneOf, allOf, not)")
    println("- Conditional validation (if/then/else)")
    println("- Real-world API validation scenarios")
  }

  // Helper methods for testing validation
  private def testStringValidation(schema: StringChez, value: String, description: String): Unit = {
    val errors = schema.validate(value)
    if (errors.isEmpty) {
      println(s"✓ $description: '$value' - Valid")
    } else {
      println(s"✗ $description: '$value' - Errors: ${errors.mkString(", ")}")
    }
  }

  private def testIntegerValidation(schema: Chez, value: Int, description: String): Unit = {
    schema match {
      case intSchema: chez.primitives.IntegerChez =>
        val errors = intSchema.validate(value)
        if (errors.isEmpty) {
          println(s"✓ $description: $value - Valid")
        } else {
          println(s"✗ $description: $value - Errors: ${errors.mkString(", ")}")
        }
      case _ => println(s"? $description: $value - Cannot validate (not an IntegerChez)")
    }
  }

  private def testNumberValidation(schema: Chez, value: Double, description: String): Unit = {
    schema match {
      case numSchema: chez.primitives.NumberChez =>
        val errors = numSchema.validate(value)
        if (errors.isEmpty) {
          println(s"✓ $description: $value - Valid")
        } else {
          println(s"✗ $description: $value - Errors: ${errors.mkString(", ")}")
        }
      case _ => println(s"? $description: $value - Cannot validate (not a NumberChez)")
    }
  }

  private def testObjectValidation(schema: Chez, value: ujson.Obj, description: String): Unit = {
    schema match {
      case objSchema: chez.complex.ObjectChez =>
        val errors = objSchema.validate(value)
        if (errors.isEmpty) {
          println(s"✓ $description: Valid")
        } else {
          println(s"✗ $description: Errors: ${errors.mkString(", ")}")
        }
      case _ => println(s"? $description: Cannot validate (not an ObjectChez)")
    }
  }

  private def testArrayValidation(schema: Chez, value: ujson.Arr, description: String): Unit = {
    schema match {
      case arrSchema: chez.complex.ArrayChez[_] =>
        val errors = arrSchema.validate(value.arr.toList)
        if (errors.isEmpty) {
          println(s"✓ $description: Valid")
        } else {
          println(s"✗ $description: Errors: ${errors.mkString(", ")}")
        }
      case _ => println(s"? $description: Cannot validate (not an ArrayChez)")
    }
  }
}
