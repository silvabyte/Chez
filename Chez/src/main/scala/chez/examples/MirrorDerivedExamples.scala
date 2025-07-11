package chez.examples

import upickle.default.*
import chez.*
import chez.derivation.{Schema, ValidatedReadWriter}

/**
 * This showcases Mirror-Based Schema Derivation enabling the syntax: case class User derives Schema
 */
object MirrorDerivedExamples {

  def runAllExamples(): Unit = {
    println("🧪 Mirror-Based Schema Derivation Examples")
    println("=" * 45)

    basicDerivedSchemaExample()
    nestedCaseClassExample()
    optionalFieldsExample()
    validatedReadWriterExample()

    println("\n✅ All Mirror-based derivation examples completed!")
  }

  /**
   * Example 1: Basic automatic schema derivation
   */
  def basicDerivedSchemaExample(): Unit = {
    println("\n🔄 Example 1: Basic Derived Schema")
    println("-" * 35)

    // Define case class with derives Schema
    case class User(id: String, name: String, age: Option[Int] = None) derives Schema, ReadWriter

    // Get the automatically derived schema
    val userSchema = Schema[User]

    println(s"Derived json schema: ${userSchema.toJsonSchema}")

    // Test data with type-safe field access
    val user = User("user123", "Alice Smith")

    println(s"\nInstance: $user")
    println(s"Type-safe access:")
    println(s"  user.id = ${user.id}")
    println(s"  user.name = ${user.name}")

    // JSON serialization using case class ReadWriter
    val json = write(user)
    val deserialized = read[User](json)

    println(s"\nSerialization:")
    println(s"  JSON: $json")
    println(s"  Roundtrip: $deserialized")
    println(s"  Type-safe access: id=${deserialized.id}, name=${deserialized.name}")
  }

  /**
   * Example 2: Nested case classes with derivation
   */
  def nestedCaseClassExample(): Unit = {
    println("\n🏠 Example 2: Nested Case Classes")
    println("-" * 33)

    case class Address(street: String, city: String, zipCode: String) derives Schema, ReadWriter
    case class Person(name: String, address: Address, contacts: List[String]) derives Schema, ReadWriter

    val addressSchema = Schema[Address]
    val personSchema = Schema[Person]

    println(s"Address schema: $addressSchema")
    println(s"Person schema: $personSchema")

    val person = Person(
      name = "John Doe",
      address = Address("123 Main St", "Springfield", "12345"),
      contacts = List("john@email.com", "555-1234")
    )

    println(s"\nNested instance: $person")

    // Type-safe nested access
    println(s"\nType-safe nested field access:")
    println(s"  person.name = ${person.name}")
    println(s"  person.address.street = ${person.address.street}")
    println(s"  person.address.city = ${person.address.city}")
    println(s"  person.contacts = ${person.contacts}")

    // JSON roundtrip
    val json = write(person)
    val roundtrip = read[Person](json)

    println(s"\nJSON roundtrip:")
    println(s"  Serialized: $json")
    println(s"  Success: ${person == roundtrip}")
  }

  /**
   * Example 3: Optional fields automatic handling
   */
  def optionalFieldsExample(): Unit = {
    println("\n❓ Example 3: Optional Fields")
    println("-" * 28)

    case class Profile(
        username: String,
        email: Option[String],
        bio: Option[String],
        score: Option[Int]
    ) derives Schema,
          ReadWriter

    val profileSchema = Schema[Profile]
    println(s"Profile schema: $profileSchema")

    val fullProfile = Profile(
      username = "alice_dev",
      email = Some("alice@dev.com"),
      bio = Some("Software Developer"),
      score = Some(95)
    )

    val minimalProfile = Profile(
      username = "bob_dev",
      email = None,
      bio = None,
      score = None
    )

    List(("Full", fullProfile), ("Minimal", minimalProfile)).foreach { case (label, profile) =>
      println(s"\n$label profile:")
      println(s"  Data: $profile")

      // Type-safe optional field access
      println(s"  username: ${profile.username}")
      println(s"  email: ${profile.email.getOrElse("not provided")}")
      println(s"  bio: ${profile.bio.getOrElse("not provided")}")
      println(s"  score: ${profile.score.getOrElse("not scored")}")

      val json = write(profile)
      println(s"  JSON: $json")
    }
  }

  /**
   * Example 4: Validated ReadWriter with derived schema
   */
  def validatedReadWriterExample(): Unit = {
    println("\n🛡️ Example 4: Validated ReadWriter")
    println("-" * 35)

    case class Product(id: String, name: String, price: Double) derives Schema, ReadWriter

    // Get both the derived schema and a validated ReadWriter
    val productSchema = Schema[Product]
    given validatedRW: ReadWriter[Product] = ValidatedReadWriter.derived[Product]

    println(s"Product schema: $productSchema")
    println(s"Validated ReadWriter created for schema validation")

    val product = Product("prod123", "Laptop", 999.99)
    val validJson = """{"id": "prod456", "name": "Mouse", "price": 29.99}"""

    println(s"\nValid product: $product")
    println(s"Valid JSON: $validJson")

    // Use validated ReadWriter
    val serialized = write(product)(using validatedRW)
    val deserialized = read[Product](validJson)(using validatedRW)

    println(s"Validated serialization: $serialized")
    println(s"Validated deserialization: $deserialized")
    println(s"Type-safe access: ${deserialized.name} costs $${deserialized.price}")

    // Future: Schema validation would happen here
    println(s"\n📋 Schema validation integration:")
    println(s"  - Schema provides validation rules")
    println(s"  - ReadWriter enforces them during deserialization")
    println(s"  - Type safety + runtime validation combined")
  }
}

/**
 * Main method for running examples standalone
 */
@main def runMirrorDerivedExamples(): Unit = {
  MirrorDerivedExamples.runAllExamples()
}
