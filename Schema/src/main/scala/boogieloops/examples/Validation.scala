package boogieloops.schema.examples

import boogieloops.schema.*
import boogieloops.schema.primitives.*
import boogieloops.schema.complex.*
import boogieloops.schema.composition.*
import boogieloops.schema.validation.*
import boogieloops.schema.{Schema as bl}

/**
 * Schema validation examples demonstrating the validation capabilities of BoogieLoops
 */
object Validation {

  def main(args: Array[String]): Unit = {
    println("🔍 BoogieLoops Validation - Schema Validation Examples!")
    println("=" * 55)

    // 1. Basic primitive validation
    println("\n1. Basic Primitive Validation:")

    val emailSchema = bl.String(format = Some("email"))
    val ageSchema = bl.Integer(minimum = Some(0), maximum = Some(25))
    val passwordSchema = bl.String(
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

    // ValidationResult framework examples with primitive types
    println("\n📋 ValidationResult Framework Examples:")
    testValidationResult(
      emailSchema,
      ujson.Str("test@example.com"),
      "valid email with ValidationResult"
    )
    testValidationResult(
      emailSchema,
      ujson.Str("invalid-email"),
      "invalid email with ValidationResult"
    )
    testValidationResult(ageSchema, ujson.Num(25), "valid age with ValidationResult")
    testValidationResult(ageSchema, ujson.Num(-5), "invalid age with ValidationResult")

    // 2. Object validation with required fields
    println("\n2. Object Validation with Required Fields:")

    val userSchema = bl.Object(
      properties = Map(
        "id" -> bl.String(),
        "name" -> bl.String(minLength = Some(1)),
        "email" -> bl.String(format = Some("email")),
        "age" -> bl.Integer(minimum = Some(0))
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

    // ValidationResult framework with simple objects (avoiding arrays/complex types)
    println("\n📋 ValidationResult Framework with Objects:")

    val simpleUserSchema = bl.Object(
      properties = Map(
        "id" -> bl.String(),
        "name" -> bl.String(minLength = Some(1)),
        "email" -> bl.String(format = Some("email"))
      ),
      required = Set("id", "name", "email")
    )

    val validSimpleUser = ujson.Obj(
      "id" -> ujson.Str("123"),
      "name" -> ujson.Str("John Doe"),
      "email" -> ujson.Str("john@example.com")
    )

    val invalidSimpleUser = ujson.Obj(
      "id" -> ujson.Str("123"),
      "name" -> ujson.Str(""), // violates minLength
      "email" -> ujson.Str("invalid-email") // invalid format
      // missing required field: email removed for this test
    )

    testValidationResultObject(
      simpleUserSchema,
      validSimpleUser,
      "valid simple user with ValidationResult"
    )
    testValidationResultObject(
      simpleUserSchema,
      invalidSimpleUser,
      "invalid simple user with ValidationResult"
    )

    // Nested validation with error path tracking (simple nesting without arrays)
    println("\n🗺️ Error Path Tracking in Nested Objects:")

    val addressSchema = bl.Object(
      properties = Map(
        "street" -> bl.String(minLength = Some(1)),
        "city" -> bl.String(minLength = Some(1)),
        "zipCode" -> bl.String(pattern = Some("^\\d{5}$"))
      ),
      required = Set("street", "city", "zipCode")
    )

    val personSchema = bl.Object(
      properties = Map(
        "name" -> bl.String(minLength = Some(1)),
        "address" -> addressSchema
      ),
      required = Set("name", "address")
    )

    val validPerson = ujson.Obj(
      "name" -> ujson.Str("John Doe"),
      "address" -> ujson.Obj(
        "street" -> ujson.Str("123 Main St"),
        "city" -> ujson.Str("Springfield"),
        "zipCode" -> ujson.Str("12345")
      )
    )

    val invalidPerson = ujson.Obj(
      "name" -> ujson.Str("Jane"),
      "address" -> ujson.Obj(
        "street" -> ujson.Str(""), // violates minLength at /address/street
        "city" -> ujson.Str("Boston"),
        "zipCode" -> ujson.Str("INVALID") // violates pattern at /address/zipCode
      )
    )

    testValidationResultObject(personSchema, validPerson, "valid nested person")
    testValidationResultObject(
      personSchema,
      invalidPerson,
      "invalid nested person with path errors"
    )

    // 3. Array validation
    println("\n3. Array Validation:")

    val stringArraySchema = bl.Array(
      bl.String(),
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

    println("\nEnhanced Array Validation (Tuple & Contains):")

    // Tuple validation with prefixItems
    val tupleSchema = ArraySchema(
      items = bl.String(), // fallback for items beyond prefix
      prefixItems = Some(List(
        bl.Integer(minimum = Some(0)), // First item: positive integer
        bl.String(minLength = Some(1)), // Second item: non-empty string
        bl.Boolean() // Third item: boolean
      )),
      minItems = Some(2),
      maxItems = Some(5)
    )

    val validTuple =
      ujson.Arr(ujson.Num(42), ujson.Str("hello"), ujson.Bool(true), ujson.Str("extra"))
    val invalidTuple =
      ujson.Arr(ujson.Num(-5), ujson.Str(""), ujson.Bool(false)) // negative number, empty string

    testArrayValidation(tupleSchema, validTuple, "valid tuple with prefixItems")
    testArrayValidation(tupleSchema, invalidTuple, "invalid tuple with constraint violations")

    // Contains validation
    val containsSchema = ArraySchema(
      items = bl.String(),
      contains =
        Some(bl.String(pattern = Some("^test.*"))), // Must contain strings starting with "test"
      minContains = Some(1),
      maxContains = Some(2)
    )

    val validContainsArray = ujson.Arr(ujson.Str("test1"), ujson.Str("other"), ujson.Str("test2"))
    val tooFewContainsArray = ujson.Arr(ujson.Str("other"), ujson.Str("another"))
    val tooManyContainsArray = ujson.Arr(ujson.Str("test1"), ujson.Str("test2"), ujson.Str("test3"))

    testArrayValidation(containsSchema, validContainsArray, "valid contains validation")
    testArrayValidation(containsSchema, tooFewContainsArray, "too few matching items")
    testArrayValidation(containsSchema, tooManyContainsArray, "too many matching items")

    // Complex scenario: both tuple and contains validation
    val complexArraySchema = ArraySchema(
      items = bl.String(),
      prefixItems = Some(List(
        bl.Integer(minimum = Some(1)),
        bl.String(minLength = Some(3))
      )),
      contains = Some(bl.String(pattern = Some("^valid.*"))),
      minContains = Some(1),
      minItems = Some(3),
      maxItems = Some(6)
    )

    val validComplexArray =
      ujson.Arr(ujson.Num(10), ujson.Str("hello"), ujson.Str("validstring"), ujson.Str("other"))
    testArrayValidation(
      complexArraySchema,
      validComplexArray,
      "complex array with tuple + contains validation"
    )

    // 4. Number validation with ranges
    println("\n4. Number Validation with Ranges:")

    val priceSchema = bl.Number(
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

    val phoneSchema = bl.String(pattern = Some("^\\+?[1-9]\\d{1,14}$"))
    val uuidSchema = bl.String(format = Some("uuid"))
    val dateSchema = bl.String(format = Some("date"))

    testStringValidation(phoneSchema, "+1234567890", "valid phone")
    testStringValidation(phoneSchema, "invalid-phone", "invalid phone")
    testStringValidation(uuidSchema, "550e8400-e29b-41d4-a716-446655440000", "valid UUID")
    testStringValidation(uuidSchema, "not-a-uuid", "invalid UUID")
    testStringValidation(dateSchema, "2023-12-25", "valid date")
    testStringValidation(dateSchema, "2023-13-25", "invalid date")

    // 6. Enum validation
    println("\n6. Enum Validation:")

    val statusSchema = bl.StringEnum("active", "inactive", "pending")
    val prioritySchema = EnumSchema.fromInts(1, 2, 3, 4, 5)

    testEnumValidation(statusSchema, "active", "valid status")
    testEnumValidation(statusSchema, "unknown", "invalid status")
    testEnumValidation(prioritySchema, 3, "valid priority")
    testEnumValidation(prioritySchema, 10, "invalid priority")

    // 7. Nullable and optional validation
    println("\n7. Nullable and Optional Validation:")

    val optionalSchema = bl.String().optional
    val nullableSchema = bl.String().nullable
    val optionalNullableSchema = bl.String().optional.nullable

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

    val stringOrNumberSchema = bl.OneOf(
      bl.String(),
      bl.Number()
    )

    val personAndEmployeeSchema = bl.AllOf(
      bl.Object("name" -> bl.String(), "age" -> bl.Integer()),
      bl.Object("employeeId" -> bl.String(), "department" -> bl.String())
    )

    val notStringSchema = bl.Not(bl.String())

    println("OneOf validation:")
    println(s"- Schema: ${stringOrNumberSchema.toJsonSchema}")
    println("- Must match exactly one of the schemas")

    println("\nAllOf validation:")
    println(s"- Schema: ${personAndEmployeeSchema.toJsonSchema}")
    println("- Must match all of the schemas")

    println("\nNot validation:")
    println(s"- Schema: ${notStringSchema.toJsonSchema}")
    println("- Must NOT match the schema")

    println("\nEnhanced Composition Validation:")

    // AnyOf validation examples
    println("\nAnyOf - Validates if at least one schema matches:")
    val flexibleValueSchema = AnyOfSchema(List(
      StringSchema(format = Some("email")),
      StringSchema(format = Some("uuid")),
      IntegerSchema(minimum = Some(0))
    ))

    val validEmailValue = ujson.Str("user@example.com")
    val validUuidValue = ujson.Str("550e8400-e29b-41d4-a716-446655440000")
    val validIntegerValue = ujson.Num(42)
    val invalidValue = ujson.Str("invalid-format")

    testValidationResult(flexibleValueSchema, validEmailValue, "valid email in anyOf")
    testValidationResult(flexibleValueSchema, validUuidValue, "valid UUID in anyOf")
    testValidationResult(flexibleValueSchema, validIntegerValue, "valid integer in anyOf")
    testValidationResult(flexibleValueSchema, invalidValue, "invalid value in anyOf")

    // OneOf validation examples with discriminated union
    println("\nOneOf - Validates if exactly one schema matches:")
    val shapeSchema = OneOfSchema(List(
      ObjectSchema(
        properties = Map(
          "type" -> StringSchema(const = Some("circle")),
          "radius" -> NumberSchema(minimum = Some(0))
        ),
        required = Set("type", "radius")
      ),
      ObjectSchema(
        properties = Map(
          "type" -> StringSchema(const = Some("rectangle")),
          "width" -> NumberSchema(minimum = Some(0)),
          "height" -> NumberSchema(minimum = Some(0))
        ),
        required = Set("type", "width", "height")
      )
    ))

    val validCircle = ujson.Obj(
      "type" -> ujson.Str("circle"),
      "radius" -> ujson.Num(5.0)
    )
    val validRectangle = ujson.Obj(
      "type" -> ujson.Str("rectangle"),
      "width" -> ujson.Num(10.0),
      "height" -> ujson.Num(5.0)
    )
    val invalidShape = ujson.Obj(
      "type" -> ujson.Str("triangle"),
      "sides" -> ujson.Num(3)
    )

    testValidationResult(shapeSchema, validCircle, "valid circle in oneOf")
    testValidationResult(shapeSchema, validRectangle, "valid rectangle in oneOf")
    testValidationResult(shapeSchema, invalidShape, "invalid shape in oneOf")

    // Nested composition example
    println("\nNested Composition - Complex schema structures:")
    val apiResponseSchema = OneOfSchema(List(
      ObjectSchema(
        properties = Map(
          "status" -> StringSchema(const = Some("success")),
          "data" -> AnyOfSchema(List(
            ObjectSchema(),
            ArraySchema(ObjectSchema())
          ))
        ),
        required = Set("status", "data")
      ),
      ObjectSchema(
        properties = Map(
          "status" -> StringSchema(const = Some("error")),
          "message" -> StringSchema(),
          "code" -> IntegerSchema()
        ),
        required = Set("status", "message")
      )
    ))

    val successResponse = ujson.Obj(
      "status" -> ujson.Str("success"),
      "data" -> ujson.Obj("result" -> ujson.Str("OK"))
    )
    val errorResponse = ujson.Obj(
      "status" -> ujson.Str("error"),
      "message" -> ujson.Str("Not found"),
      "code" -> ujson.Num(404)
    )

    testValidationResult(apiResponseSchema, successResponse, "valid success response")
    testValidationResult(apiResponseSchema, errorResponse, "valid error response")

    // 9. Conditional validation
    println("\n9. Conditional Validation:")

    val conditionalUserSchema = bl.If(
      condition = bl.Object("role" -> bl.String(const = Some("admin"))),
      thenSchema = bl.Object("adminLevel" -> bl.String()), // replaced array with simple string
      elseSchema = bl.Object("department" -> bl.String())
    )

    println("Conditional validation:")
    println(s"- Schema: ${conditionalUserSchema.toJsonSchema}")
    println("- Admin user validation would check for adminLevel")
    println("- Regular user validation would check for department")

    // 10. Real-world API validation example
    println("\n10. Real-World API Validation Example:")

    val createProductSchema = bl.Object(
      properties = Map(
        "name" -> bl.String(minLength = Some(1), maxLength = Some(100)),
        "description" -> bl.String(maxLength = Some(1000)).optional,
        "price" -> bl.Number(minimum = Some(0.01), multipleOf = Some(0.01)),
        "category" -> bl.StringEnum("electronics", "clothing", "books", "food"),
        "sku" -> bl.String(pattern = Some("^[A-Z]{3}-\\d{6}$")),
        "inStock" -> bl.Boolean(),
        "brand" -> bl.String().optional, // replaced tags array with simple brand string
        "metadata" -> bl
          .Object(
            Map(),
            patternProperties = Map(
              "^[a-zA-Z_][a-zA-Z0-9_]*$" -> bl.String()
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
      "brand" -> ujson.Str("TechCorp"), // replaced tags array with simple brand
      "metadata" -> ujson.Obj(
        "manufacturer" -> ujson.Str("TechCorp"),
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

    // Pattern properties example with ValidationResult (using only String/Boolean/Integer types)
    println("\n🎯 Pattern Properties with ValidationResult:")

    val dynamicConfigSchema = bl.Object(
      properties = Map(
        "appName" -> bl.String(minLength = Some(1))
      ),
      patternProperties = Map(
        "^config_" -> bl.String(minLength = Some(3)),
        "^flag_" -> bl.Boolean()
      ),
      required = Set("appName")
    )

    val validDynamicConfig = ujson.Obj(
      "appName" -> ujson.Str("MyApp"),
      "config_database" -> ujson.Str("postgres://localhost"),
      "flag_enabled" -> ujson.Bool(true)
    )

    val invalidDynamicConfig = ujson.Obj(
      "appName" -> ujson.Str("MyApp"),
      "config_db" -> ujson.Str("hi"), // too short for minLength=3
      "flag_enabled" -> ujson.Str("not_boolean") // wrong type
    )

    testValidationResultObject(dynamicConfigSchema, validDynamicConfig, "valid pattern properties")
    testValidationResultObject(
      dynamicConfigSchema,
      invalidDynamicConfig,
      "invalid pattern properties"
    )

    // Additional properties with schema validation
    println("\n🔧 Additional Properties Schema Validation:")

    val flexibleSchema = bl.Object(
      properties = Map("name" -> bl.String()),
      additionalPropertiesSchema = Some(bl.String(minLength = Some(2)))
    )

    val validFlexible = ujson.Obj(
      "name" -> ujson.Str("Test"),
      "extra1" -> ujson.Str("hello"),
      "extra2" -> ujson.Str("world")
    )

    val invalidFlexible = ujson.Obj(
      "name" -> ujson.Str("Test"),
      "extra1" -> ujson.Str("h"), // too short for minLength=2
      "extra2" -> ujson.Str("ok")
    )

    testValidationResultObject(
      flexibleSchema,
      validFlexible,
      "valid additional properties with schema"
    )
    testValidationResultObject(
      flexibleSchema,
      invalidFlexible,
      "invalid additional properties with schema"
    )

    println("\nAdvanced Composition Validation (AllOf, Not, IfThenElse):")

    // AllOf Example: String with multiple constraints
    println("\n📋 AllOf - All schemas must validate:")
    val strongPasswordSchema = AllOfSchema(List(
      bl.String(minLength = Some(8)), // At least 8 characters
      bl.String(pattern = Some(".*[A-Z].*")), // Contains uppercase
      bl.String(pattern = Some(".*[0-9].*")) // Contains digit
    ))

    testValidationResult(
      strongPasswordSchema,
      ujson.Str("Password123"),
      "valid strong password (all constraints met)"
    )
    testValidationResult(
      strongPasswordSchema,
      ujson.Str("weak"),
      "invalid weak password (fails multiple constraints)"
    )

    // Not Example: Exclude specific values
    println("\n🚫 Not - Schema must NOT validate:")
    val nonAdminSchema = NotSchema(
      bl.Object(
        properties = Map("role" -> bl.String(const = Some("admin"))),
        required = Set("role")
      )
    )

    testValidationResult(
      nonAdminSchema,
      ujson.Obj("role" -> ujson.Str("user")),
      "valid non-admin user"
    )
    testValidationResult(
      nonAdminSchema,
      ujson.Obj("role" -> ujson.Str("admin")),
      "invalid admin user (not allowed)"
    )

    // IfThenElse Example: Role-based validation
    println("\n🔀 IfThenElse - Conditional validation:")
    val roleBasedSchema = IfThenElseSchema(
      condition = bl.Object(
        properties = Map("role" -> bl.String(const = Some("admin")))
      ),
      thenSchema = Some(bl.Object(
        properties = Map(
          "permissions" -> ArraySchema(bl.String(), minItems = Some(1)),
          "department" -> bl.String()
        ),
        required = Set("permissions", "department")
      )),
      elseSchema = Some(bl.Object(
        properties = Map("supervisor" -> bl.String()),
        required = Set("supervisor")
      ))
    )

    testValidationResult(
      roleBasedSchema,
      ujson.Obj(
        "role" -> ujson.Str("admin"),
        "permissions" -> ujson.Arr(ujson.Str("read"), ujson.Str("write")),
        "department" -> ujson.Str("IT")
      ),
      "valid admin with required fields"
    )
    testValidationResult(
      roleBasedSchema,
      ujson.Obj(
        "role" -> ujson.Str("user"),
        "supervisor" -> ujson.Str("John Smith")
      ),
      "valid regular user with supervisor"
    )

    // Complex nested composition example
    println("\n🎯 Complex Nested Composition:")
    val complexUserSchema = AllOfSchema(List(
      // Base user schema
      bl.Object(
        properties = Map(
          "id" -> bl.String(),
          "email" -> bl.String(format = Some("email"))
        ),
        required = Set("id", "email")
      ),
      // Not a test user
      NotSchema(
        bl.Object(
          properties = Map("email" -> bl.String(pattern = Some(".*@test\\.com$")))
        )
      ),
      // Role-specific requirements
      IfThenElseSchema(
        condition = bl.Object(
          properties = Map("role" -> bl.String(const = Some("premium")))
        ),
        thenSchema = Some(bl.Object(
          properties = Map("subscription" -> bl.String()),
          required = Set("subscription")
        ))
      )
    ))

    testValidationResult(
      complexUserSchema,
      ujson.Obj(
        "id" -> ujson.Str("user123"),
        "email" -> ujson.Str("john@example.com"),
        "role" -> ujson.Str("basic")
      ),
      "valid basic user (all constraints met)"
    )
    testValidationResult(
      complexUserSchema,
      ujson.Obj(
        "id" -> ujson.Str("user123"),
        "email" -> ujson.Str("test@test.com") // Test email not allowed
      ),
      "invalid test user (not schema fails)"
    )

    println("\n🎯 Validation Examples Complete!")
    println("These examples demonstrate comprehensive validation capabilities:")
    println("\n📋 Core Features:")
    println("- Primitive type validation (string, number, integer, boolean)")
    println("- Object validation with required fields")
    println("- Array validation with constraints")
    println("- Number range and multiple validation")
    println("- String pattern and format validation")
    println("- Enum validation")
    println("- Nullable and optional field handling")
    println("\n🔄 Advanced Features:")
    println("- Composition validation (anyOf, oneOf, allOf, not)")
    println("- Conditional validation (if/then/else)")
    println("- Real-world API validation scenarios")
    println("\n📋 ValidationResult Framework (T4):")
    println("- ValidationResult.valid() and ValidationResult.invalid() states")
    println("- Error path tracking for nested objects (e.g., /address/street)")
    println("- Pattern properties validation with regex matching")
    println("- Additional properties schema validation")
    println("- Type checking with TypeMismatch errors")
    println("- Custom ValidationContext paths")
  }

  // Helper methods for testing validation
  private def testStringValidation(schema: StringSchema, value: String, description: String): Unit = {
    val result = schema.validate(ujson.Str(value), ValidationContext())
    if (result.isValid) {
      println(s"✓ $description: '$value' - Valid")
    } else {
      println(s"✗ $description: '$value' - Errors: ${result.errors.mkString(", ")}")
    }
  }

  private def testIntegerValidation(schema: Schema, value: Int, description: String): Unit = {
    schema match {
      case intSchema: boogieloops.schema.primitives.IntegerSchema =>
        val result = intSchema.validate(ujson.Num(value), ValidationContext())
        if (result.isValid) {
          println(s"✓ $description: $value - Valid")
        } else {
          println(s"✗ $description: $value - Errors: ${result.errors.mkString(", ")}")
        }
      case _ => println(s"? $description: $value - Cannot validate (not an IntegerSchema)")
    }
  }

  private def testNumberValidation(schema: Schema, value: Double, description: String): Unit = {
    schema match {
      case numSchema: boogieloops.schema.primitives.NumberSchema =>
        val result = numSchema.validate(ujson.Num(value), ValidationContext())
        if (result.isValid) {
          println(s"✓ $description: $value - Valid")
        } else {
          println(s"✗ $description: $value - Errors: ${result.errors.mkString(", ")}")
        }
      case _ => println(s"? $description: $value - Cannot validate (not a NumberSchema)")
    }
  }

  private def testObjectValidation(schema: Schema, value: ujson.Obj, description: String): Unit = {
    schema match {
      case objSchema: boogieloops.schema.complex.ObjectSchema =>
        val result = objSchema.validate(value, ValidationContext())
        if (result.isValid) {
          println(s"✓ $description: Valid")
        } else {
          println(s"✗ $description: Errors: ${result.errors.mkString(", ")}")
        }
      case _ => println(s"? $description: Cannot validate (not an ObjectSchema)")
    }
  }

  private def testArrayValidation(schema: Schema, value: ujson.Arr, description: String): Unit = {
    schema match {
      case arrSchema: boogieloops.schema.complex.ArraySchema[_] =>
        val result = arrSchema.validate(value, ValidationContext())
        if (result.isValid) {
          println(s"✓ $description: Valid")
        } else {
          println(s"✗ $description: Errors: ${result.errors.mkString(", ")}")
        }
      case _ => println(s"? $description: Cannot validate (not an ArraySchema)")
    }
  }

  private def testEnumValidation(schema: EnumSchema, value: Any, description: String): Unit = {
    val ujsonValue = Option(value) match {
      case None => ujson.Null
      case Some(s: String) => ujson.Str(s)
      case Some(i: Int) => ujson.Num(i)
      case Some(d: Double) => ujson.Num(d)
      case Some(b: Boolean) => if (b) ujson.True else ujson.False
      case Some(other) => ujson.Str(other.toString)
    }

    val result = schema.validate(ujsonValue, ValidationContext())
    if (result.isValid) {
      println(s"✓ $description: '$value' - Valid")
    } else {
      println(s"✗ $description: '$value' - Errors: ${result.errors.mkString(", ")}")
    }
  }

  // ValidationResult framework test methods
  private def testValidationResult(schema: Schema, value: ujson.Value, description: String): Unit = {
    // Only use new validation method for types that support it
    schema match {
      case stringSchema: boogieloops.schema.primitives.StringSchema =>
        val result = stringSchema.validate(value, ValidationContext())
        if (result.isValid) {
          println(s"✓ $description: Valid (${result.getClass.getSimpleName})")
        } else {
          println(s"✗ $description: Invalid (${result.errors.length} errors)")
          result.errors.foreach { error =>
            println(s"    - $error")
          }
        }
      case intSchema: boogieloops.schema.primitives.IntegerSchema =>
        val result = intSchema.validate(value, ValidationContext())
        if (result.isValid) {
          println(s"✓ $description: Valid (${result.getClass.getSimpleName})")
        } else {
          println(s"✗ $description: Invalid (${result.errors.length} errors)")
          result.errors.foreach { error =>
            println(s"    - $error")
          }
        }
      case _ =>
        // Fall back to legacy validation for types that don't support ValidationResult yet
        println(
          s"? $description: ValidationResult not yet supported for ${schema.getClass.getSimpleName}"
        )
    }
  }

  private def testValidationResultObject(
      schema: Schema,
      value: ujson.Obj,
      description: String
  ): Unit = {
    schema match {
      case objSchema: boogieloops.schema.complex.ObjectSchema =>
        val result = objSchema.validate(value: ujson.Value, ValidationContext())
        if (result.isValid) {
          println(s"✓ $description: Valid (${result.getClass.getSimpleName})")
        } else {
          println(s"✗ $description: Invalid (${result.errors.length} errors)")
          result.errors.foreach { error =>
            val path = error match {
              case boogieloops.schema.ValidationError.MinLengthViolation(_, _, path) => s"[$path]"
              case boogieloops.schema.ValidationError.PatternMismatch(_, _, path) => s"[$path]"
              case boogieloops.schema.ValidationError.MissingField(_, path) => s"[$path]"
              case boogieloops.schema.ValidationError.InvalidFormat(_, _, path) => s"[$path]"
              case boogieloops.schema.ValidationError.TypeMismatch(_, _, path) => s"[$path]"
              case boogieloops.schema.ValidationError.OutOfRange(_, _, _, path) => s"[$path]"
              case boogieloops.schema.ValidationError.AdditionalProperty(_, path) => s"[$path]"
              case _ => ""
            }
            println(s"    $path $error")
          }
        }
      case _ => println(s"? $description: Cannot validate (not an ObjectSchema)")
    }
  }
}
