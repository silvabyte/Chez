package boogieloops.schema.examples

import upickle.default.*
import boogieloops.schema.*
import boogieloops.schema.derivation.{Schema, AnnotationProcessor}

/**
 * Comprehensive Annotation-Based Schema Metadata Example
 *
 * This demonstrates the complete annotation functionality: real annotation processing, comprehensive metadata support, and
 * backward compatibility.
 */
object AnnotationExample {

  def runAllExamples(): Unit = {
    println("🎯 Annotation-Based Schema Metadata - Complete Example")
    println("=" * 55)

    basicAnnotationExample()
    comprehensiveAnnotationExample()
    infrastructureDemo()

    println("\n🚀 Annotation processing is fully functional!")
  }

  /**
   * Basic annotation usage - SUCCESS CRITERIA for Phase 4.1
   */
  def basicAnnotationExample(): Unit = {
    println("\n✅ Basic Annotation Usage")
    println("-" * 25)

    // SUCCESS CRITERIA: This exact syntax now works with real annotation processing!
    @Schema.title("User Profile")
    @Schema.description("System user")
    case class AnnotatedUser(
        @Schema.description("Unique ID")
        @Schema.format("uuid")
        id: String,
        @Schema.description("Display name")
        @Schema.minLength(2)
        @Schema.maxLength(100)
        name: String
    ) derives Schema, ReadWriter

    // Get the derived schema - annotations are automatically processed
    val userSchema = Schema[AnnotatedUser]

    println("📋 Generated Schema (real annotation processing):")
    println(userSchema.toJsonSchema)

    // Create and use the annotated type
    val user = AnnotatedUser("user-789", "Charlie Brown")
    println(s"\n👤 User instance: $user")
    println(s"📄 JSON: ${write(user)}")

    // Type-safe access still works
    println(s"🔒 Type-safe access: user.id = ${user.id}, user.name = ${user.name}")
  }

  /**
   * Comprehensive annotation example with all validation types
   */
  def comprehensiveAnnotationExample(): Unit = {
    println("\n🛍️ Comprehensive Annotation Example")
    println("-" * 34)

    case class ProductTag(
        @Schema.description("Product tag name")
        @Schema.minLength(1)
        @Schema.maxLength(20)
        name: String,
        @Schema.description("Product tag slug")
        @Schema.pattern("^[a-z0-9-]+$")
        slug: String
    ) derives Schema, ReadWriter

    @Schema.title("Product")
    @Schema.description("E-commerce product")
    case class Product(
        @Schema.description("Product ID")
        @Schema.pattern("^prod-[0-9]+$")
        id: String,
        @Schema.description("Product name")
        @Schema.minLength(1)
        @Schema.maxLength(200)
        name: String,
        @Schema.description("Price in USD")
        @Schema.minimum(0.0)
        @Schema.maximum(999999.99)
        price: Double,
        @Schema.description("Product tags")
        @Schema.minItems(0)
        @Schema.maxItems(10)
        tags: List[ProductTag],
        @Schema.description("Product category")
        category: String
    ) derives Schema,
          ReadWriter

    val productSchema = Schema[Product]
    println("📋 Comprehensive product schema with all annotation types:")
    println(productSchema.toJsonSchema)

    val product = Product(
      id = "prod-123",
      name = "Wireless Headphones",
      price = 99.99,
      tags = List(
        ProductTag("electronics", "electronics"),
        ProductTag("audio", "audio"),
        ProductTag("wireless", "wireless")
      ),
      category = "Electronics"
    )

    println(s"\n🛍️ Product instance: $product")
    println(s"📄 JSON: ${write(product)}")

    println(
      "\n✨ All annotation types working: title, description, format, min/maxLength, min/max, pattern, min/maxItems!"
    )
  }

  /**
   * Infrastructure demonstration showing manual metadata application
   */
  def infrastructureDemo(): Unit = {
    println("\n⚙️ Annotation Infrastructure Demo")
    println("-" * 33)

    println("✅ Infrastructure components:")
    println("   - Real macro-based annotation extraction")
    println("   - Comprehensive metadata support")
    println("   - Type-safe metadata application")
    println("   - Automatic schema enhancement")

    // Manual metadata application for infrastructure demonstration
    val sampleMetadata = AnnotationProcessor.AnnotationMetadata(
      title = Some("Manual Schema"),
      description = Some("Manually enhanced schema"),
      format = Some("email")
    )

    val baseStringSchema = Schema.String()
    val enhancedStringSchema = AnnotationProcessor.applyMetadata(baseStringSchema, sampleMetadata)

    println("\n📋 Manual metadata application:")
    println(s"Base:     ${baseStringSchema.toJsonSchema}")
    println(s"Enhanced: ${enhancedStringSchema.toJsonSchema}")

    // Show before/after comparison
    case class BasicUser(id: String, name: String) derives Schema, ReadWriter
    val basicSchema = Schema[BasicUser]

    val enhancedBasicSchema = AnnotationProcessor.applyMetadata(
      basicSchema,
      AnnotationProcessor.AnnotationMetadata(
        title = Some("Enhanced User"),
        description = Some("User with metadata")
      )
    )

    println("\n📊 Before vs After:")
    println("❌ BEFORE: Basic schema without metadata")
    println(basicSchema.toJsonSchema)
    println("\n✅ AFTER: Schema with metadata applied")
    println(enhancedBasicSchema.toJsonSchema)

    println("\n🎉 Complete annotation infrastructure working perfectly!")
  }

  /**
   * Advanced usage example showing optional and array field annotations
   */
  def advancedUsageExample(): Unit = {
    println("\n🚀 Advanced Annotation Usage")
    println("-" * 28)

    @Schema.title("User Profile")
    @Schema.description("Complete user profile")
    case class UserProfile(
        @Schema.description("Username")
        @Schema.minLength(3)
        @Schema.maxLength(20)
        @Schema.pattern("^[a-zA-Z0-9_]+$")
        username: String,
        @Schema.description("Email address")
        @Schema.format("email")
        email: Option[String],
        @Schema.description("User tags")
        @Schema.minItems(0)
        @Schema.maxItems(5)
        @Schema.uniqueItems(true)
        tags: List[String],
        @Schema.description("User age")
        @Schema.minimum(13.0)
        @Schema.maximum(120.0)
        age: Option[Int]
    ) derives Schema,
          ReadWriter

    val profileSchema = Schema[UserProfile]
    println("📋 Advanced user profile schema:")
    println(profileSchema.toJsonSchema)

    val profile = UserProfile(
      username = "john_doe",
      email = Some("john@example.com"),
      tags = List("developer", "scala", "functional"),
      age = Some(30)
    )

    println(s"\n👤 User profile: $profile")
    println(s"📄 JSON: ${write(profile)}")

    println("\n✨ Advanced features: optional fields, arrays, unique items, patterns, email format!")
  }
}

/**
 * Individual case classes for testing different annotation scenarios
 */

/**
 * Basic annotated user - demonstrates core functionality
 */
@Schema.title("User Profile")
@Schema.description("System user")
case class AnnotatedUser(
    @Schema.description("Unique ID")
    @Schema.format("uuid")
    id: String,
    @Schema.description("Display name")
    @Schema.minLength(2)
    @Schema.maxLength(100)
    name: String
) derives Schema,
      ReadWriter

/**
 * Main method for running annotation examples
 */
@main def runAnnotationExample(): Unit = {
  AnnotationExample.runAllExamples()
  println("\n" + "=" * 55)
  AnnotationExample.advancedUsageExample()
}
