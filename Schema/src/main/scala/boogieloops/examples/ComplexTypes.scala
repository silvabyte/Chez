package boogieloops.schema.examples

import boogieloops.schema.*
import boogieloops.schema.{Schema as bl}

/**
 * Advanced schema composition examples demonstrating complex JSON Schema 2020-12 features
 */
object ComplexTypes {

  def main(args: Array[String]): Unit = {
    println("🎨 BoogieLoops Complex Types - Advanced Schema Composition!")
    println("=" * 60)

    // 1. Recursive schemas with references
    println("\n1. Recursive Schemas with References:")

    val personSchema = bl.Object(
      "name" -> bl.String(minLength = Some(1)),
      "age" -> bl.Integer(minimum = Some(0)),
      "children" -> bl.Array(bl.Ref("#/$defs/Person")).optional
    )

    val familyTreeSchema = bl
      .Object(
        "root" -> bl.Ref("#/$defs/Person")
      )
      .withDefs(
        "Person" -> personSchema
      )

    println(s"Family Tree Schema: ${familyTreeSchema.toJsonSchema}")

    // 2. Complex union types with discriminators
    println("\n2. Complex Union Types with Discriminators:")

    val shapeSchema = bl.OneOf(
      // Circle
      bl.Object(
        "type" -> bl.String(const = Some("circle")),
        "radius" -> bl.Number(minimum = Some(0))
      ),
      // Rectangle
      bl.Object(
        "type" -> bl.String(const = Some("rectangle")),
        "width" -> bl.Number(minimum = Some(0)),
        "height" -> bl.Number(minimum = Some(0))
      ),
      // Triangle
      bl.Object(
        "type" -> bl.String(const = Some("triangle")),
        "base" -> bl.Number(minimum = Some(0)),
        "height" -> bl.Number(minimum = Some(0))
      )
    )

    println(s"Shape Schema: ${shapeSchema.toJsonSchema}")

    // 3. Conditional schemas with complex logic
    println("\n3. Conditional Schemas with Complex Logic:")

    val userAccountSchema = bl.AllOf(
      // Base user properties
      bl.Object(
        "id" -> bl.String(),
        "username" -> bl.String(minLength = Some(3)),
        "email" -> bl.String(format = Some("email")),
        "role" -> bl.StringEnum("user", "admin", "moderator")
      ),
      // Conditional properties based on role
      bl.If(
        condition = bl.Object("role" -> bl.String(const = Some("admin"))),
        thenSchema = bl.Object(
          "permissions" -> bl.Array(bl.String()),
          "accessLevel" -> bl.Integer(minimum = Some(1), maximum = Some(10))
        ),
        elseSchema = bl.If(
          condition = bl.Object("role" -> bl.String(const = Some("moderator"))),
          thenSchema = bl.Object(
            "moderatedChannels" -> bl.Array(bl.String())
          ),
          elseSchema = bl.Object(
            "preferences" -> bl
              .Object(
                "theme" -> bl.StringEnum("light", "dark").optional,
                "notifications" -> bl.Boolean().optional
              )
              .optional
          )
        )
      )
    )

    println(s"User Account Schema: ${userAccountSchema.toJsonSchema}")

    // 4. Pattern properties and additional properties
    println("\n4. Pattern Properties and Additional Properties:")

    val dynamicConfigSchema = bl.Object(
      // Fixed properties
      properties = Map(
        "version" -> bl.String(pattern = Some("^\\d+\\.\\d+\\.\\d+$")),
        "environment" -> bl.StringEnum("dev", "staging", "prod")
      ),
      // Pattern properties for dynamic configuration
      patternProperties = Map(
        "^feature_[a-z_]+$" -> bl.Object(
          "enabled" -> bl.Boolean(),
          "config" -> bl.Object().optional
        ),
        "^cache_[a-z_]+$" -> bl.Object(
          "ttl" -> bl.Integer(minimum = Some(0)),
          "maxSize" -> bl.Integer(minimum = Some(1)).optional
        ),
        "^service_[a-z_]+_url$" -> bl.String(format = Some("uri"))
      ),
      // Additional properties allowed but must be strings
      additionalProperties = Some(false)
    )

    println(s"Dynamic Config Schema: ${dynamicConfigSchema.toJsonSchema}")

    // 5. Nested arrays with complex item schemas
    println("\n5. Nested Arrays with Complex Item Schemas:")

    val matrixSchema = bl.Array(
      bl.Array(
        bl.Number(),
        minItems = Some(3),
        maxItems = Some(3)
      ),
      minItems = Some(3),
      maxItems = Some(3)
    )

    val datasetSchema = bl.Object(
      "metadata" -> bl.Object(
        "name" -> bl.String(),
        "version" -> bl.String(),
        "description" -> bl.String().optional
      ),
      "data" -> bl.Array(
        bl.Object(
          "id" -> bl.String(),
          "features" -> bl.Array(bl.Number()),
          "labels" -> bl.Array(bl.String()).optional,
          "metadata" -> bl.Object().optional
        )
      ),
      "transformations" -> bl
        .Array(
          bl.OneOf(
            bl.Object(
              "type" -> bl.String(const = Some("normalize")),
              "mean" -> bl.Number(),
              "std" -> bl.Number()
            ),
            bl.Object(
              "type" -> bl.String(const = Some("scale")),
              "factor" -> bl.Number(minimum = Some(0))
            ),
            bl.Object(
              "type" -> bl.String(const = Some("filter")),
              "condition" -> bl.String()
            )
          )
        )
        .optional
    )

    println(s"Matrix Schema: ${matrixSchema.toJsonSchema}")
    println(s"Dataset Schema: ${datasetSchema.toJsonSchema}")

    // 6. API schema with comprehensive error handling
    println("\n6. API Schema with Comprehensive Error Handling:")

    val apiErrorSchema = bl.Object(
      "error" -> bl.Object(
        "code" -> bl.String(),
        "message" -> bl.String(),
        "details" -> bl.Object().optional,
        "timestamp" -> bl.String(format = Some("date-time")),
        "requestId" -> bl.String().optional
      )
    )

    val paginationSchema = bl.Object(
      "page" -> bl.Integer(minimum = Some(1)),
      "pageSize" -> bl.Integer(minimum = Some(1), maximum = Some(100)),
      "totalItems" -> bl.Integer(minimum = Some(0)),
      "totalPages" -> bl.Integer(minimum = Some(0))
    )

    val apiResponseSchema = bl.OneOf(
      // Success response
      bl.Object(
        "success" -> bl.Boolean(const = Some(true)),
        "data" -> bl.Object(), // Generic data
        "pagination" -> paginationSchema.optional
      ),
      // Error response
      bl.Object(
        "success" -> bl.Boolean(const = Some(false)),
        "error" -> apiErrorSchema
      )
    )

    println(s"API Response Schema: ${apiResponseSchema.toJsonSchema}")

    // 7. Schema composition with dependencies
    println("\n7. Schema Composition with Dependencies:")

    val paymentMethodSchema = bl.OneOf(
      bl.Object(
        "type" -> bl.String(const = Some("credit_card")),
        "cardNumber" -> bl.String(pattern = Some("^\\d{16}$")),
        "expiryDate" -> bl.String(pattern = Some("^\\d{2}/\\d{2}$")),
        "cvv" -> bl.String(pattern = Some("^\\d{3,4}$"))
      ),
      bl.Object(
        "type" -> bl.String(const = Some("paypal")),
        "email" -> bl.String(format = Some("email"))
      ),
      bl.Object(
        "type" -> bl.String(const = Some("bank_transfer")),
        "accountNumber" -> bl.String(),
        "routingNumber" -> bl.String()
      )
    )

    println(s"Payment Method Schema: ${paymentMethodSchema.toJsonSchema}")

    // 8. Advanced string patterns and formats
    println("\n8. Advanced String Patterns and Formats:")

    val validationSchema = bl.Object(
      "email" -> bl.String(format = Some("email")),
      "phoneNumber" -> bl.String(pattern = Some("^\\+?[1-9]\\d{1,14}$")),
      "url" -> bl.String(format = Some("uri")),
      "ipAddress" -> bl.String(format = Some("ipv4")),
      "uuid" -> bl.String(format = Some("uuid")),
      "date" -> bl.String(format = Some("date")),
      "dateTime" -> bl.String(format = Some("date-time")),
      "time" -> bl.String(format = Some("time")),
      "password" -> bl.String(
        minLength = Some(8),
        pattern = Some("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]")
      ),
      "slug" -> bl.String(pattern = Some("^[a-z0-9]+(?:-[a-z0-9]+)*$")),
      "hexColor" -> bl.String(pattern = Some("^#[0-9a-fA-F]{6}$")),
      "base64" -> bl.String(pattern = Some("^[A-Za-z0-9+/]*={0,2}$"))
    )

    println(s"Validation Schema: ${validationSchema.toJsonSchema}")

    // 9. Schema with content encoding and media types
    println("\n9. Schema with Content Encoding and Media Types:")

    val fileUploadSchema = bl.Object(
      "filename" -> bl.String(),
      "mimeType" -> bl.String(const = Some("application/octet-stream")),
      "size" -> bl.Integer(minimum = Some(0)),
      "content" -> bl.String(),
      "checksum" -> bl
        .Object(
          "algorithm" -> bl.StringEnum("md5", "sha1", "sha256"),
          "value" -> bl.String()
        )
        .optional
    )

    println(s"File Upload Schema: ${fileUploadSchema.toJsonSchema}")

    // 10. Meta-schema with extensive annotations
    println("\n10. Meta-Schema with Extensive Annotations:")

    val annotatedSchema = bl
      .Object(
        "product" -> bl.Object(
          "id" -> bl
            .String()
            .withTitle("Product ID")
            .withDescription("Unique identifier for the product"),
          "name" -> bl
            .String(minLength = Some(1))
            .withTitle("Product Name")
            .withDescription("Display name of the product"),
          "price" -> bl
            .Number(minimum = Some(0))
            .withTitle("Price")
            .withDescription("Price in USD"),
          "category" -> bl
            .StringEnum("electronics", "clothing", "books")
            .withTitle("Category")
            .withDescription("Product category")
        )
      )
      .withTitle("Product Schema")
      .withDescription("Schema for product objects in our e-commerce system")
      .withSchema(bl.MetaSchemaUrl)
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
