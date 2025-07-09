package chez.examples

import chez.*
import upickle.default.*
import scala.util.{Try, Success, Failure}

/**
 * Advanced schema composition examples demonstrating complex JSON Schema 2020-12 features
 */
object ComplexTypes {

  def main(args: Array[String]): Unit = {
    println("🎨 Chez Complex Types - Advanced Schema Composition!")
    println("=" * 60)

    // 1. Recursive schemas with references
    println("\n1. Recursive Schemas with References:")

    val personSchema = Chez.Object(
      "name" -> Chez.String(minLength = Some(1)),
      "age" -> Chez.Integer(minimum = Some(0)),
      "children" -> Chez.Array(Chez.Ref("#/$defs/Person")).optional
    )

    val familyTreeSchema = Chez
      .Object(
        "root" -> Chez.Ref("#/$defs/Person")
      )
      .withDefs(
        "Person" -> personSchema
      )

    println(s"Family Tree Schema: ${familyTreeSchema.toJsonSchema}")

    // 2. Complex union types with discriminators
    println("\n2. Complex Union Types with Discriminators:")

    val shapeSchema = Chez.OneOf(
      // Circle
      Chez.Object(
        "type" -> Chez.String(const = Some("circle")),
        "radius" -> Chez.Number(minimum = Some(0))
      ),
      // Rectangle
      Chez.Object(
        "type" -> Chez.String(const = Some("rectangle")),
        "width" -> Chez.Number(minimum = Some(0)),
        "height" -> Chez.Number(minimum = Some(0))
      ),
      // Triangle
      Chez.Object(
        "type" -> Chez.String(const = Some("triangle")),
        "base" -> Chez.Number(minimum = Some(0)),
        "height" -> Chez.Number(minimum = Some(0))
      )
    )

    println(s"Shape Schema: ${shapeSchema.toJsonSchema}")

    // 3. Conditional schemas with complex logic
    println("\n3. Conditional Schemas with Complex Logic:")

    val userAccountSchema = Chez.AllOf(
      // Base user properties
      Chez.Object(
        "id" -> Chez.String(),
        "username" -> Chez.String(minLength = Some(3)),
        "email" -> Chez.String(format = Some("email")),
        "role" -> Chez.String(enumValues = Some(List("user", "admin", "moderator")))
      ),
      // Conditional properties based on role
      Chez.If(
        condition = Chez.Object("role" -> Chez.String(const = Some("admin"))),
        thenSchema = Chez.Object(
          "permissions" -> Chez.Array(Chez.String()),
          "accessLevel" -> Chez.Integer(minimum = Some(1), maximum = Some(10))
        ),
        elseSchema = Chez.If(
          condition = Chez.Object("role" -> Chez.String(const = Some("moderator"))),
          thenSchema = Chez.Object(
            "moderatedChannels" -> Chez.Array(Chez.String())
          ),
          elseSchema = Chez.Object(
            "preferences" -> Chez
              .Object(
                "theme" -> Chez.String(enumValues = Some(List("light", "dark"))).optional,
                "notifications" -> Chez.Boolean().optional
              )
              .optional
          )
        )
      )
    )

    println(s"User Account Schema: ${userAccountSchema.toJsonSchema}")

    // 4. Pattern properties and additional properties
    println("\n4. Pattern Properties and Additional Properties:")

    val dynamicConfigSchema = Chez.Object(
      // Fixed properties
      properties = Map(
        "version" -> Chez.String(pattern = Some("^\\d+\\.\\d+\\.\\d+$")),
        "environment" -> Chez.String(enumValues = Some(List("dev", "staging", "prod")))
      ),
      // Pattern properties for dynamic configuration
      patternProperties = Map(
        "^feature_[a-z_]+$" -> Chez.Object(
          "enabled" -> Chez.Boolean(),
          "config" -> Chez.Object().optional
        ),
        "^cache_[a-z_]+$" -> Chez.Object(
          "ttl" -> Chez.Integer(minimum = Some(0)),
          "maxSize" -> Chez.Integer(minimum = Some(1)).optional
        ),
        "^service_[a-z_]+_url$" -> Chez.String(format = Some("uri"))
      ),
      // Additional properties allowed but must be strings
      additionalProperties = Some(false)
    )

    println(s"Dynamic Config Schema: ${dynamicConfigSchema.toJsonSchema}")

    // 5. Nested arrays with complex item schemas
    println("\n5. Nested Arrays with Complex Item Schemas:")

    val matrixSchema = Chez.Array(
      Chez.Array(
        Chez.Number(),
        minItems = Some(3),
        maxItems = Some(3)
      ),
      minItems = Some(3),
      maxItems = Some(3)
    )

    val datasetSchema = Chez.Object(
      "metadata" -> Chez.Object(
        "name" -> Chez.String(),
        "version" -> Chez.String(),
        "description" -> Chez.String().optional
      ),
      "data" -> Chez.Array(
        Chez.Object(
          "id" -> Chez.String(),
          "features" -> Chez.Array(Chez.Number()),
          "labels" -> Chez.Array(Chez.String()).optional,
          "metadata" -> Chez.Object().optional
        )
      ),
      "transformations" -> Chez
        .Array(
          Chez.OneOf(
            Chez.Object(
              "type" -> Chez.String(const = Some("normalize")),
              "mean" -> Chez.Number(),
              "std" -> Chez.Number()
            ),
            Chez.Object(
              "type" -> Chez.String(const = Some("scale")),
              "factor" -> Chez.Number(minimum = Some(0))
            ),
            Chez.Object(
              "type" -> Chez.String(const = Some("filter")),
              "condition" -> Chez.String()
            )
          )
        )
        .optional
    )

    println(s"Matrix Schema: ${matrixSchema.toJsonSchema}")
    println(s"Dataset Schema: ${datasetSchema.toJsonSchema}")

    // 6. API schema with comprehensive error handling
    println("\n6. API Schema with Comprehensive Error Handling:")

    val apiErrorSchema = Chez.Object(
      "error" -> Chez.Object(
        "code" -> Chez.String(),
        "message" -> Chez.String(),
        "details" -> Chez.Object().optional,
        "timestamp" -> Chez.String(format = Some("date-time")),
        "requestId" -> Chez.String().optional
      )
    )

    val paginationSchema = Chez.Object(
      "page" -> Chez.Integer(minimum = Some(1)),
      "pageSize" -> Chez.Integer(minimum = Some(1), maximum = Some(100)),
      "totalItems" -> Chez.Integer(minimum = Some(0)),
      "totalPages" -> Chez.Integer(minimum = Some(0))
    )

    val apiResponseSchema = Chez.OneOf(
      // Success response
      Chez.Object(
        "success" -> Chez.Boolean(const = Some(true)),
        "data" -> Chez.Object(), // Generic data
        "pagination" -> paginationSchema.optional
      ),
      // Error response
      Chez.Object(
        "success" -> Chez.Boolean(const = Some(false)),
        "error" -> apiErrorSchema
      )
    )

    println(s"API Response Schema: ${apiResponseSchema.toJsonSchema}")

    // 7. Schema composition with dependencies
    println("\n7. Schema Composition with Dependencies:")

    val paymentMethodSchema = Chez.OneOf(
      Chez.Object(
        "type" -> Chez.String(const = Some("credit_card")),
        "cardNumber" -> Chez.String(pattern = Some("^\\d{16}$")),
        "expiryDate" -> Chez.String(pattern = Some("^\\d{2}/\\d{2}$")),
        "cvv" -> Chez.String(pattern = Some("^\\d{3,4}$"))
      ),
      Chez.Object(
        "type" -> Chez.String(const = Some("paypal")),
        "email" -> Chez.String(format = Some("email"))
      ),
      Chez.Object(
        "type" -> Chez.String(const = Some("bank_transfer")),
        "accountNumber" -> Chez.String(),
        "routingNumber" -> Chez.String()
      )
    )

    println(s"Payment Method Schema: ${paymentMethodSchema.toJsonSchema}")

    // 8. Advanced string patterns and formats
    println("\n8. Advanced String Patterns and Formats:")

    val validationSchema = Chez.Object(
      "email" -> Chez.String(format = Some("email")),
      "phoneNumber" -> Chez.String(pattern = Some("^\\+?[1-9]\\d{1,14}$")),
      "url" -> Chez.String(format = Some("uri")),
      "ipAddress" -> Chez.String(format = Some("ipv4")),
      "uuid" -> Chez.String(format = Some("uuid")),
      "date" -> Chez.String(format = Some("date")),
      "dateTime" -> Chez.String(format = Some("date-time")),
      "time" -> Chez.String(format = Some("time")),
      "password" -> Chez.String(
        minLength = Some(8),
        pattern = Some("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]")
      ),
      "slug" -> Chez.String(pattern = Some("^[a-z0-9]+(?:-[a-z0-9]+)*$")),
      "hexColor" -> Chez.String(pattern = Some("^#[0-9a-fA-F]{6}$")),
      "base64" -> Chez.String(pattern = Some("^[A-Za-z0-9+/]*={0,2}$"))
    )

    println(s"Validation Schema: ${validationSchema.toJsonSchema}")

    // 9. Schema with content encoding and media types
    println("\n9. Schema with Content Encoding and Media Types:")

    val fileUploadSchema = Chez.Object(
      "filename" -> Chez.String(),
      "mimeType" -> Chez.String(const = Some("application/octet-stream")),
      "size" -> Chez.Integer(minimum = Some(0)),
      "content" -> Chez.String(),
      "checksum" -> Chez
        .Object(
          "algorithm" -> Chez.String(enumValues = Some(List("md5", "sha1", "sha256"))),
          "value" -> Chez.String()
        )
        .optional
    )

    println(s"File Upload Schema: ${fileUploadSchema.toJsonSchema}")

    // 10. Meta-schema with extensive annotations
    println("\n10. Meta-Schema with Extensive Annotations:")

    val annotatedSchema = Chez
      .Object(
        "product" -> Chez.Object(
          "id" -> Chez
            .String()
            .withTitle("Product ID")
            .withDescription("Unique identifier for the product"),
          "name" -> Chez
            .String(minLength = Some(1))
            .withTitle("Product Name")
            .withDescription("Display name of the product"),
          "price" -> Chez
            .Number(minimum = Some(0))
            .withTitle("Price")
            .withDescription("Price in USD"),
          "category" -> Chez
            .String(enumValues = Some(List("electronics", "clothing", "books")))
            .withTitle("Category")
            .withDescription("Product category")
        )
      )
      .withTitle("Product Schema")
      .withDescription("Schema for product objects in our e-commerce system")
      .withSchema(Chez.MetaSchemaUrl)
      .withId("https://example.com/schemas/product")

    println(s"Annotated Schema: ${annotatedSchema.toJsonSchema}")

    println("\n🎯 Complex Types Examples Complete!")
    println("These examples demonstrate advanced JSON Schema 2020-12 features:")
    println("- Recursive schemas with references")
    println("- Complex union types with discriminators")
    println("- Conditional schemas with nested logic")
    println("- Pattern properties and dynamic configurations")
    println("- Nested arrays with complex item schemas")
    println("- API schemas with comprehensive error handling")
    println("- Schema composition with dependencies")
    println("- Advanced string patterns and formats")
    println("- Content encoding and media types")
    println("- Meta-schema with extensive annotations")
  }
}
