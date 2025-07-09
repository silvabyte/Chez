package chez.examples

import chez.*
import upickle.default.*

/**
 * Demonstrating Chez integration with the lihaoyi ecosystem
 */
object LihaoyiEcosystem {
  
  def main(args: Array[String]): Unit = {
    println("🌟 Chez + Lihaoyi Ecosystem Integration!")
    println("=" * 50)
    
    // Define schemas for a simple API
    val userSchema = Chez.Object(
      "id" -> Chez.String(),
      "name" -> Chez.String(minLength = Some(1)),
      "email" -> Chez.String(format = Some("email")),
      "age" -> Chez.Integer(minimum = Some(0), maximum = Some(150)).optional
    )
    
    val createUserRequestSchema = Chez.Object(
      "name" -> Chez.String(minLength = Some(1)),
      "email" -> Chez.String(format = Some("email")),
      "age" -> Chez.Integer(minimum = Some(0), maximum = Some(150)).optional
    )
    
    val errorSchema = Chez.Object(
      "error" -> Chez.String(),
      "message" -> Chez.String(),
      "code" -> Chez.Integer()
    )
    
    println("\n1. Schema Definitions:")
    println(s"User Schema: ${userSchema.toJsonSchema}")
    println(s"Create User Request Schema: ${createUserRequestSchema.toJsonSchema}")
    println(s"Error Schema: ${errorSchema.toJsonSchema}")
    
    // Demonstrate JSON serialization/deserialization
    println("\n2. JSON Serialization with upickle:")
    
    // Sample data
    val userData = ujson.Obj(
      "id" -> ujson.Str("user123"),
      "name" -> ujson.Str("John Doe"),
      "email" -> ujson.Str("john@example.com"),
      "age" -> ujson.Num(30)
    )
    
    val createUserData = ujson.Obj(
      "name" -> ujson.Str("Jane Smith"),
      "email" -> ujson.Str("jane@example.com")
    )
    
    val errorData = ujson.Obj(
      "error" -> ujson.Str("ValidationError"),
      "message" -> ujson.Str("Invalid email format"),
      "code" -> ujson.Num(400)
    )
    
    // Convert to JSON strings
    val userJson = userData.toString()
    val createUserJson = createUserData.toString()
    val errorJson = errorData.toString()
    
    println(s"User JSON: $userJson")
    println(s"Create User Request JSON: $createUserJson")
    println(s"Error JSON: $errorJson")
    
    // Demonstrate validation (basic example)
    println("\n3. Basic Validation:")
    
    def validateJson(schema: Chez, data: ujson.Value, schemaName: String): Unit = {
      // This is a simplified validation - in practice we'd implement full JSON Schema validation
      println(s"✓ $schemaName validation would check:")
      println(s"  - Schema: ${schema.toJsonSchema}")
      println(s"  - Data: $data")
      println(s"  - Result: Valid (placeholder)")
    }
    
    validateJson(userSchema, userData, "User")
    validateJson(createUserRequestSchema, createUserData, "CreateUserRequest")
    validateJson(errorSchema, errorData, "Error")
    
    // Demonstrate schema composition for API responses
    println("\n4. API Response Schema Composition:")
    
    val apiResponseSchema = Chez.OneOf(
      // Success response
      Chez.Object(
        "success" -> Chez.Boolean(const = Some(true)),
        "data" -> userSchema
      ),
      // Error response
      Chez.Object(
        "success" -> Chez.Boolean(const = Some(false)),
        "error" -> errorSchema
      )
    )
    
    println(s"API Response Schema: ${apiResponseSchema.toJsonSchema}")
    
    // Sample API responses
    val successResponse = ujson.Obj(
      "success" -> ujson.Bool(true),
      "data" -> userData
    )
    
    val errorResponse = ujson.Obj(
      "success" -> ujson.Bool(false),
      "error" -> errorData
    )
    
    println(s"Success Response: $successResponse")
    println(s"Error Response: $errorResponse")
    
    // Configuration schema example (for use with os-lib)
    println("\n5. Configuration Schema (os-lib integration):")
    
    val configSchema = Chez.Object(
      "database" -> Chez.Object(
        "host" -> Chez.String(),
        "port" -> Chez.Integer(minimum = Some(1024), maximum = Some(65535)),
        "name" -> Chez.String(),
        "ssl" -> Chez.Boolean().optional
      ),
      "server" -> Chez.Object(
        "host" -> Chez.String(),
        "port" -> Chez.Integer(minimum = Some(1024), maximum = Some(65535)),
        "cors" -> Chez.Object(
          "enabled" -> Chez.Boolean(),
          "origins" -> Chez.Array(Chez.String()).optional
        ).optional
      ),
      "logging" -> Chez.Object(
        "level" -> Chez.OneOf(
          Chez.String(enumValues = Some(List("DEBUG", "INFO", "WARN", "ERROR")))
        ),
        "file" -> Chez.String().optional
      ).optional
    )
    
    println(s"Configuration Schema: ${configSchema.toJsonSchema}")
    
    // Sample configuration
    val sampleConfig = ujson.Obj(
      "database" -> ujson.Obj(
        "host" -> ujson.Str("localhost"),
        "port" -> ujson.Num(5432),
        "name" -> ujson.Str("myapp"),
        "ssl" -> ujson.Bool(true)
      ),
      "server" -> ujson.Obj(
        "host" -> ujson.Str("0.0.0.0"),
        "port" -> ujson.Num(8080),
        "cors" -> ujson.Obj(
          "enabled" -> ujson.Bool(true),
          "origins" -> ujson.Arr(ujson.Str("https://example.com"))
        )
      ),
      "logging" -> ujson.Obj(
        "level" -> ujson.Str("INFO"),
        "file" -> ujson.Str("/var/log/myapp.log")
      )
    )
    
    println(s"Sample Configuration: $sampleConfig")
    
    // Demonstrate advanced features
    println("\n6. Advanced JSON Schema 2020-12 Features:")
    
    // Conditional validation
    val conditionalUserSchema = Chez.If(
      condition = Chez.Object("role" -> Chez.String(enumValues = Some(List("admin")))),
      thenSchema = Chez.Object(
        "permissions" -> Chez.Array(Chez.String()).optional
      ),
      elseSchema = Chez.Object(
        "department" -> Chez.String().optional
      )
    )
    
    println(s"Conditional User Schema: ${conditionalUserSchema.toJsonSchema}")
    
    // Pattern properties
    val dynamicConfigSchema = Chez.Object(
      properties = Map(
        "version" -> Chez.String()
      ),
      patternProperties = Map(
        "^feature_.*" -> Chez.Boolean(),
        "^cache_.*" -> Chez.Object(
          "ttl" -> Chez.Integer(minimum = Some(0)),
          "enabled" -> Chez.Boolean()
        )
      )
    )
    
    println(s"Dynamic Config Schema: ${dynamicConfigSchema.toJsonSchema}")
    
    // Meta-schema with proper JSON Schema 2020-12 compliance
    println("\n7. JSON Schema 2020-12 Meta-Schema Compliance:")
    
    val metaCompliantSchema = Chez.Object(
      "product" -> Chez.Object(
        "id" -> Chez.String(),
        "name" -> Chez.String()
      )
    )
    
    // Add meta-schema information
    val schemaWithMeta = ujson.Obj(
      "$schema" -> ujson.Str("https://json-schema.org/draft/2020-12/schema"),
      "$id" -> ujson.Str("https://example.com/schemas/product"),
      "title" -> ujson.Str("Product Schema"),
      "description" -> ujson.Str("A schema for product objects"),
      "type" -> ujson.Str("object"),
      "properties" -> ujson.Obj(
        "product" -> metaCompliantSchema.toJsonSchema
      ),
      "required" -> ujson.Arr(ujson.Str("product"))
    )
    
    println(s"Meta-compliant Schema: $schemaWithMeta")
    
    println("\n🎯 Lihaoyi Ecosystem Integration Examples Complete!")
    println("Chez seamlessly integrates with upickle, os-lib, and other lihaoyi tools!")
    println("Perfect for building type-safe APIs, configuration validation, and more!")
  }
}