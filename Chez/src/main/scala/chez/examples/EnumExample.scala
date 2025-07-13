package chez.examples

import chez.*
import chez.derivation.*
import upickle.default.*
import scala.util.{Try, Success, Failure}

/**
 * Example demonstrating Scala 3 enum schema derivation behavior This helps identify current issues and expected behavior for
 * enums
 */
object EnumExample {

  // Simple Scala 3 enum
  enum Status derives Schema {
    case Active, Inactive, Pending
  }

  // Enum with values
  enum Priority(val level: Int) derives Schema {
    case Low extends Priority(1)
    case Medium extends Priority(2)
    case High extends Priority(3)
    case Critical extends Priority(4)
  }

  // Enum with methods
  enum Color(val hex: String) derives Schema {
    case Red extends Color("#FF0000")
    case Green extends Color("#00FF00")
    case Blue extends Color("#0000FF")

    def brightness: Double = {
      val r = Integer.parseInt(hex.substring(1, 3), 16)
      val g = Integer.parseInt(hex.substring(3, 5), 16)
      val b = Integer.parseInt(hex.substring(5, 7), 16)
      (r * 0.299 + g * 0.587 + b * 0.114) / 255.0
    }
  }

  // Case classes using enums
  case class User(name: String, status: Status) derives Schema
  case class Task(title: String, priority: Priority, status: Status) derives Schema
  case class Theme(name: String, primaryColor: Color, secondaryColor: Color) derives Schema

  def main(args: Array[String]): Unit = {
    println("🔍 Chez Enum Schema Derivation - Current Behavior Analysis")
    println("=" * 65)

    // Test 1: Simple enum schema derivation
    println("\n1. Simple Status Enum Schema Derivation:")
    Try {
      val statusSchema = Schema[Status]
      println(s"✅ Status schema derived successfully")
      println(s"Schema: ${statusSchema.toJsonSchema}")

      // Check what type of schema was generated
      val json = statusSchema.toJsonSchema
      if (json.obj.contains("type")) {
        println(s"Generated type: ${json("type")}")
      }
      if (json.obj.contains("enum")) {
        println(s"Generated enum values: ${json("enum")}")
      }
      if (json.obj.contains("oneOf")) {
        println(s"Generated oneOf: ${json("oneOf")}")
      }
      if (json.obj.contains("anyOf")) {
        println(s"Generated anyOf: ${json("anyOf")}")
      }
    } match {
      case Failure(e) =>
        println(s"❌ Error deriving Status schema: ${e.getMessage}")
        e.printStackTrace()
      case Success(_) =>
        println("✅ Status schema derived successfully")
    }

    // Test 2: Enum with values schema derivation
    println("\n2. Priority Enum (with values) Schema Derivation:")
    Try {
      val prioritySchema = Schema[Priority]
      println(s"✅ Priority schema derived successfully")
      println(s"Schema: ${prioritySchema.toJsonSchema}")
    } match {
      case Failure(e) =>
        println(s"❌ Error deriving Priority schema: ${e.getMessage}")
        e.printStackTrace()
      case Success(_) =>
        println("✅ Priority schema derived successfully")
    }

    // Test 3: Complex enum schema derivation
    println("\n3. Color Enum (with methods) Schema Derivation:")
    Try {
      val colorSchema = Schema[Color]
      println(s"✅ Color schema derived successfully")
      println(s"Schema: ${colorSchema.toJsonSchema}")
    } match {
      case Failure(e) =>
        println(s"❌ Error deriving Color schema: ${e.getMessage}")
        e.printStackTrace()
      case Success(_) =>
        println("✅ Color schema derived successfully")
    }

    // Test 4: Case class with enum fields
    println("\n4. User Case Class with Enum Field:")
    Try {
      val userSchema = Schema[User]
      println(s"✅ User schema derived successfully")
      println(s"Schema: ${userSchema.toJsonSchema}")

      // Check enum field specifically
      val json = userSchema.toJsonSchema
      if (json.obj.contains("properties")) {
        val props = json("properties")
        if (props.obj.contains("status")) {
          println(s"Status field schema: ${props("status")}")
        }
      }
    } match {
      case Failure(e) =>
        println(s"❌ Error deriving User schema: ${e.getMessage}")
        e.printStackTrace()
      case Success(_) =>
        println("✅ User schema derived successfully")
    }

    // Test 5: Case class with multiple enum fields
    println("\n5. Task Case Class with Multiple Enum Fields:")
    Try {
      val taskSchema = Schema[Task]
      println(s"✅ Task schema derived successfully")
      println(s"Schema: ${taskSchema.toJsonSchema}")
    } match {
      case Failure(e) =>
        println(s"❌ Error deriving Task schema: ${e.getMessage}")
        e.printStackTrace()
      case Success(_) =>
        println("✅ Task schema derived successfully")
    }

    // Test 6: Case class with complex enum fields
    println("\n6. Theme Case Class with Complex Enum Fields:")
    Try {
      val themeSchema = Schema[Theme]
      println(s"✅ Theme schema derived successfully")
      println(s"Schema: ${themeSchema.toJsonSchema}")
    } match {
      case Failure(e) =>
        println(s"❌ Error deriving Theme schema: ${e.getMessage}")
        e.printStackTrace()
      case Success(_) =>
        println("✅ Theme schema derived successfully")
    }
  }
}
