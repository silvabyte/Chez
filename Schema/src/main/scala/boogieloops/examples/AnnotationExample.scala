package boogieloops.schema.examples

import upickle.default.*
import boogieloops.schema.*
import boogieloops.schema.derivation.{Schematic, AnnotationProcessor}

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
    @Schematic.title("User Profile")
    @Schematic.description("System user")
    case class AnnotatedUser(
        @Schematic.description("Unique ID")
        @Schematic.format("uuid")
        id: String,
        @Schematic.description("Display name")
        @Schematic.minLength(2)
        @Schematic.maxLength(100)
        name: String
    ) derives Schematic, ReadWriter

    // Get the derived schema - annotations are automatically processed
    val userSchema = Schematic[AnnotatedUser]

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
        @Schematic.description("Product tag name")
        @Schematic.minLength(1)
        @Schematic.maxLength(20)
        name: String,
        @Schematic.description("Product tag slug")
        @Schematic.pattern("^[a-z0-9-]+$")
        slug: String
    ) derives Schematic, ReadWriter

    @Schematic.title("Product")
    @Schematic.description("E-commerce product")
    case class Product(
        @Schematic.description("Product ID")
        @Schematic.pattern("^prod-[0-9]+$")
        id: String,
        @Schematic.description("Product name")
        @Schematic.minLength(1)
        @Schematic.maxLength(200)
        name: String,
        @Schematic.description("Price in USD")
        @Schematic.minimum(0.0)
        @Schematic.maximum(999999.99)
        price: Double,
        @Schematic.description("Product tags")
        @Schematic.minItems(0)
        @Schematic.maxItems(10)
        tags: List[ProductTag],
        @Schematic.description("Product category")
        category: String
    ) derives Schematic,
          ReadWriter

    val productSchema = Schematic[Product]
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

    val baseStringSchema = bl.String()
    val enhancedStringSchema = AnnotationProcessor.applyMetadata(baseStringSchema, sampleMetadata)

    println("\n📋 Manual metadata application:")
    println(s"Base:     ${baseStringSchema.toJsonSchema}")
    println(s"Enhanced: ${enhancedStringSchema.toJsonSchema}")

    // Show before/after comparison
    case class BasicUser(id: String, name: String) derives Schematic, ReadWriter
    val basicSchema = Schematic[BasicUser]

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

    @Schematic.title("User Profile")
    @Schematic.description("Complete user profile")
    case class UserProfile(
        @Schematic.description("Username")
        @Schematic.minLength(3)
        @Schematic.maxLength(20)
        @Schematic.pattern("^[a-zA-Z0-9_]+$")
        username: String,
        @Schematic.description("Email address")
        @Schematic.format("email")
        email: Option[String],
        @Schematic.description("User tags")
        @Schematic.minItems(0)
        @Schematic.maxItems(5)
        @Schematic.uniqueItems(true)
        tags: List[String],
        @Schematic.description("User age")
        @Schematic.minimum(13.0)
        @Schematic.maximum(120.0)
        age: Option[Int]
    ) derives Schematic,
          ReadWriter

    val profileSchema = Schematic[UserProfile]
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
@Schematic.title("User Profile")
@Schematic.description("System user")
case class AnnotatedUser(
    @Schematic.description("Unique ID")
    @Schematic.format("uuid")
    id: String,
    @Schematic.description("Display name")
    @Schematic.minLength(2)
    @Schematic.maxLength(100)
    name: String
) derives Schematic,
      ReadWriter

/**
 * Main method for running annotation examples
 */
@main def runAnnotationExample(): Unit = {
  AnnotationExample.runAllExamples()
  println("\n" + "=" * 55)
  AnnotationExample.advancedUsageExample()
}
