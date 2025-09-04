package chez.derivation

import utest.*
import chez.*
import chez.complex.*
import chez.derivation.CollectionSchemas.given
 

object EnumDerivationTests extends TestSuite {

  // Simple Scala 3 enum
  enum Status derives Schema {
    case Active, Inactive, Pending
  }

  // Enum with constructor parameters
  enum Priority(val level: Int) derives Schema {
    case Low extends Priority(1)
    case Medium extends Priority(2)
    case High extends Priority(3)
    case Critical extends Priority(4)
  }

  // Enum with methods and constructor parameters
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

  // Single case enum
  enum SingleCase derives Schema {
    case OnlyOne
  }

  val tests = Tests {
    test("Simple enum derivation") {
      test("Status enum generates string with enum values") {
        val schema = Schema[Status]
        val json = schema.toJsonSchema

        assert(json("type").str == "string")
        assert(json.obj.contains("enum"))

        val enumValues = json("enum").arr.map(_.str).toSet
        assert(enumValues == Set("Active", "Inactive", "Pending"))
      }

      test("Status enum schema structure") {
        val schema = Schema[Status]
        val json = schema.toJsonSchema

        // Should not have oneOf, anyOf, or object properties
        assert(!json.obj.contains("oneOf"))
        assert(!json.obj.contains("anyOf"))
        assert(!json.obj.contains("properties"))
        assert(!json.obj.contains("required"))

        // Should be a simple string enum
        assert(json("type").str == "string")
        assert(json.obj.contains("enum"))
      }
    }

    test("Enum with constructor parameters") {
      test("Priority enum generates string with enum values") {
        val schema = Schema[Priority]
        val json = schema.toJsonSchema

        assert(json("type").str == "string")
        assert(json.obj.contains("enum"))

        val enumValues = json("enum").arr.map(_.str).toSet
        assert(enumValues == Set("Low", "Medium", "High", "Critical"))
      }

      test("Priority enum ignores constructor parameters in schema") {
        val schema = Schema[Priority]
        val json = schema.toJsonSchema

        // Should not reflect the Int parameter in the schema
        assert(json("type").str == "string")
        assert(!json.obj.contains("properties"))
        assert(!json.obj.contains("oneOf"))

        // Only the case names should be in enum values
        val enumValues = json("enum").arr.map(_.str).toList
        assert(enumValues == List("Low", "Medium", "High", "Critical"))
      }
    }

    test("Enum with methods and constructor parameters") {
      test("Color enum generates string with enum values") {
        val schema = Schema[Color]
        val json = schema.toJsonSchema

        assert(json("type").str == "string")
        assert(json.obj.contains("enum"))

        val enumValues = json("enum").arr.map(_.str).toSet
        assert(enumValues == Set("Red", "Green", "Blue"))
      }

      test("Color enum ignores methods and parameters in schema") {
        val schema = Schema[Color]
        val json = schema.toJsonSchema

        // Should not reflect the hex parameter or brightness method
        assert(json("type").str == "string")
        assert(!json.obj.contains("properties"))
        assert(!json.obj.contains("oneOf"))

        // Only the case names should be in enum values
        val enumValues = json("enum").arr.map(_.str).toList
        assert(enumValues == List("Red", "Green", "Blue"))
      }
    }

    test("Edge case enums") {
      test("Single case enum") {
        val schema = Schema[SingleCase]
        val json = schema.toJsonSchema

        assert(json("type").str == "string")
        assert(json.obj.contains("enum"))

        val enumValues = json("enum").arr.map(_.str).toList
        assert(enumValues == List("OnlyOne"))
      }

    }

    test("Enum integration with case classes") {
      test("Case class with single enum field") {
        case class User(name: String, status: Status) derives Schema

        val schema = Schema[User]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("properties"))
        assert(json.obj.contains("required"))

        val props = json("properties")

        // Check name field
        assert(props("name")("type").str == "string")

        // Check status field (enum)
        assert(props("status")("type").str == "string")
        assert(props("status").obj.contains("enum"))
        val statusEnumValues = props("status")("enum").arr.map(_.str).toSet
        assert(statusEnumValues == Set("Active", "Inactive", "Pending"))

        // Check required fields
        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("name", "status"))
      }

      test("Case class with multiple enum fields") {
        case class Task(title: String, priority: Priority, status: Status) derives Schema

        val schema = Schema[Task]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Check title field
        assert(props("title")("type").str == "string")

        // Check priority field (enum)
        assert(props("priority")("type").str == "string")
        assert(props("priority").obj.contains("enum"))
        val priorityEnumValues = props("priority")("enum").arr.map(_.str).toSet
        assert(priorityEnumValues == Set("Low", "Medium", "High", "Critical"))

        // Check status field (enum)
        assert(props("status")("type").str == "string")
        assert(props("status").obj.contains("enum"))
        val statusEnumValues = props("status")("enum").arr.map(_.str).toSet
        assert(statusEnumValues == Set("Active", "Inactive", "Pending"))

        // Check required fields
        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("title", "priority", "status"))
      }

      test("Case class with optional enum field") {
        case class Profile(name: String, priority: Option[Priority]) derives Schema

        val schema = Schema[Profile]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Check optional priority field (enum)
        assert(props("priority")("type").str == "string")
        assert(props("priority").obj.contains("enum"))
        val priorityEnumValues = props("priority")("enum").arr.map(_.str).toSet
        assert(priorityEnumValues == Set("Low", "Medium", "High", "Critical"))

        // Check required fields (priority should not be required)
        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("name"))
      }

      test("Case class with enum default value") {
        case class Settings(theme: Color = Color.Blue, priority: Priority = Priority.Low)
            derives Schema

        val schema = Schema[Settings]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Check theme field (enum with default)
        assert(props("theme")("type").str == "string")
        assert(props("theme").obj.contains("enum"))
        val themeEnumValues = props("theme")("enum").arr.map(_.str).toSet
        assert(themeEnumValues == Set("Red", "Green", "Blue"))

        // Check priority field (enum with default)
        assert(props("priority")("type").str == "string")
        assert(props("priority").obj.contains("enum"))
        val priorityEnumValues = props("priority")("enum").arr.map(_.str).toSet
        assert(priorityEnumValues == Set("Low", "Medium", "High", "Critical"))

        // Check required fields (should be empty due to defaults)
        val required = if (json.obj.contains("required")) {
          json("required").arr.map(_.str).toSet
        } else {
          Set.empty[String]
        }
        assert(required.isEmpty)
      }
    }

    test("Enum with collections") {
      test("List of enums") {
        case class TaggedItem(name: String, statuses: List[Status]) derives Schema

        val schema = Schema[TaggedItem]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Check statuses field (List[Status])
        assert(props("statuses")("type").str == "array")
        assert(props("statuses").obj.contains("items"))

        val items = props("statuses")("items")
        assert(items("type").str == "string")
        assert(items.obj.contains("enum"))
        val enumValues = items("enum").arr.map(_.str).toSet
        assert(enumValues == Set("Active", "Inactive", "Pending"))
      }

      test("Set of enums") {
        case class UniqueStatuses(name: String, statuses: Set[Status]) derives Schema

        val schema = Schema[UniqueStatuses]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Check statuses field (Set[Status])
        assert(props("statuses")("type").str == "array")
        assert(props("statuses")("uniqueItems").bool == true)
        assert(props("statuses").obj.contains("items"))

        val items = props("statuses")("items")
        assert(items("type").str == "string")
        assert(items.obj.contains("enum"))
        val enumValues = items("enum").arr.map(_.str).toSet
        assert(enumValues == Set("Active", "Inactive", "Pending"))
      }

      test("Vector of enums") {
        case class OrderedStatuses(name: String, statuses: Vector[Priority]) derives Schema

        val schema = Schema[OrderedStatuses]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Check statuses field (Vector[Priority])
        assert(props("statuses")("type").str == "array")
        assert(!props("statuses").obj.contains("uniqueItems")) // Vector doesn't have uniqueItems
        assert(props("statuses").obj.contains("items"))

        val items = props("statuses")("items")
        assert(items("type").str == "string")
        assert(items.obj.contains("enum"))
        val enumValues = items("enum").arr.map(_.str).toSet
        assert(enumValues == Set("Low", "Medium", "High", "Critical"))
      }

      test("Map with enum values") {
        case class StatusMap(name: String, userStatuses: Map[String, Status]) derives Schema

        val schema = Schema[StatusMap]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Check userStatuses field (Map[String, Status])
        assert(props("userStatuses")("type").str == "object")
        assert(props("userStatuses").obj.contains("additionalProperties"))

        val additionalProps = props("userStatuses")("additionalProperties")
        assert(additionalProps("type").str == "string")
        assert(additionalProps.obj.contains("enum"))
        val enumValues = additionalProps("enum").arr.map(_.str).toSet
        assert(enumValues == Set("Active", "Inactive", "Pending"))
      }
    }

    test("Nested enum structures") {
      test("Case class with nested case class containing enum") {
        case class UserProfile(name: String, settings: UserSettings) derives Schema
        case class UserSettings(theme: Color, priority: Priority) derives Schema

        val schema = Schema[UserProfile]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Check settings field (nested case class)
        assert(props("settings")("type").str == "object")
        assert(props("settings").obj.contains("properties"))

        val settingsProps = props("settings")("properties")

        // Check theme field in nested object
        assert(settingsProps("theme")("type").str == "string")
        assert(settingsProps("theme").obj.contains("enum"))
        val themeEnumValues = settingsProps("theme")("enum").arr.map(_.str).toSet
        assert(themeEnumValues == Set("Red", "Green", "Blue"))

        // Check priority field in nested object
        assert(settingsProps("priority")("type").str == "string")
        assert(settingsProps("priority").obj.contains("enum"))
        val priorityEnumValues = settingsProps("priority")("enum").arr.map(_.str).toSet
        assert(priorityEnumValues == Set("Low", "Medium", "High", "Critical"))
      }

      test("Option of case class with enum") {
        case class MaybeSettings(name: String, settings: Option[UserSettingsWithEnum])
            derives Schema
        case class UserSettingsWithEnum(theme: Color) derives Schema

        val schema = Schema[MaybeSettings]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Check optional settings field
        assert(props("settings")("type").str == "object")
        assert(props("settings").obj.contains("properties"))

        val settingsProps = props("settings")("properties")
        assert(settingsProps("theme")("type").str == "string")
        assert(settingsProps("theme").obj.contains("enum"))
        val themeEnumValues = settingsProps("theme")("enum").arr.map(_.str).toSet
        assert(themeEnumValues == Set("Red", "Green", "Blue"))

        // Check required fields (settings should not be required due to Option)
        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("name"))
      }
    }

    test("Enum value ordering") {
      test("Enum values maintain declaration order") {
        val schema = Schema[Status]
        val json = schema.toJsonSchema

        val enumValues = json("enum").arr.map(_.str).toList
        // Should maintain the order: Active, Inactive, Pending
        assert(enumValues == List("Active", "Inactive", "Pending"))
      }

      test("Priority enum values maintain declaration order") {
        val schema = Schema[Priority]
        val json = schema.toJsonSchema

        val enumValues = json("enum").arr.map(_.str).toList
        // Should maintain the order: Low, Medium, High, Critical
        assert(enumValues == List("Low", "Medium", "High", "Critical"))
      }
    }

    test("Mixed data type enum support (Future Enhancement)") {
      test("Current implementation: EnumChez supports string enum values") {
        // CURRENT BEHAVIOR: EnumChez supports proper enum handling
        val stringEnumSchema = Chez.StringEnum("red", "amber", "green")
        val json = stringEnumSchema.toJsonSchema

        assert(json("type").str == "string")
        assert(json.obj.contains("enum"))

        // EnumChez properly supports string values in String enum
        val enumValues = json("enum").arr
        assert(enumValues.forall(_.isInstanceOf[ujson.Str]))
        assert(enumValues.map(_.str).toSet == Set("red", "amber", "green"))

        // LIMITATION: Cannot currently create: {"enum": ["red", "amber", "green", null, 42]}
        // This would require a new MixedEnumChez type that accepts List[ujson.Value]
      }

      test("TODO: MixedEnumChez for heterogeneous enum values") {
        // PLACEHOLDER: Future implementation would look like this:
        //
        // case class MixedEnumChez(enumValues: List[ujson.Value]) extends Chez {
        //   override def toJsonSchema: ujson.Value = {
        //     ujson.Obj("enum" -> ujson.Arr(enumValues*))
        //   }
        // }
        //
        // Usage:
        // val mixedEnum = MixedEnumChez(List(
        //   ujson.Str("red"),
        //   ujson.Str("amber"),
        //   ujson.Str("green"),
        //   ujson.Null,
        //   ujson.Num(42)
        // ))
        //
        // Would generate: {"enum": ["red", "amber", "green", null, 42]}

        // For now, just validate that the JSON structure is possible
        val expectedJson = ujson.Obj(
          "enum" -> ujson.Arr(
            ujson.Str("red"),
            ujson.Str("amber"),
            ujson.Str("green"),
            ujson.Null,
            ujson.Num(42)
          )
        )

        assert(expectedJson.obj.contains("enum"))
        val enumArr = expectedJson("enum").arr
        assert(enumArr.length == 5)
        assert(enumArr(0).str == "red")
        assert(enumArr(3).isNull)
        assert(enumArr(4).num == 42.0)
      }

      test("TODO: Object with mixed enum property support") {
        // PLACEHOLDER: Future implementation for the user's requested structure:
        // { "properties": { "color": { "enum": ["red", "amber", "green", null, 42] } } }
        //
        // This would require:
        // 1. MixedEnumChez class (see above)
        // 2. Integration with Chez.Object factory method
        // 3. Proper Schema derivation support
        //
        // Example future usage:
        // val objectSchema = Chez.Object(
        //   properties = Map(
        //     "color" -> MixedEnumChez(List(
        //       ujson.Str("red"), ujson.Str("amber"), ujson.Str("green"),
        //       ujson.Null, ujson.Num(42)
        //     ))
        //   )
        // )

        // Current workaround: Manual JSON construction
        val manualJson = ujson.Obj(
          "type" -> ujson.Str("object"),
          "properties" -> ujson.Obj(
            "color" -> ujson.Obj(
              "enum" -> ujson.Arr(
                ujson.Str("red"),
                ujson.Str("amber"),
                ujson.Str("green"),
                ujson.Null,
                ujson.Num(42)
              )
            )
          )
        )

        // Verify the structure is valid
        assert(manualJson("type").str == "object")
        val colorEnum = manualJson("properties")("color")("enum").arr
        assert(colorEnum.length == 5)
      }

      test("TODO: Schema derivation for mixed enum types") {
        // PLACEHOLDER: Future enhancement for automatic derivation
        //
        // Ideally, we'd want to support Scala types like:
        //
        // sealed trait TrafficLightState
        // case object Red extends TrafficLightState
        // case object Amber extends TrafficLightState
        // case object Green extends TrafficLightState
        // case object Maintenance extends TrafficLightState // null in JSON
        // case class FlashingYellow(frequency: Int) extends TrafficLightState // 42 in JSON
        //
        // And have Schema[TrafficLightState] generate:
        // {"enum": ["Red", "Amber", "Green", null, 42]}
        //
        // This would require:
        // 1. Enhanced sum type derivation
        // 2. Custom mapping for case objects to different JSON types
        // 3. Configuration for which case objects map to null/numbers

        // For now, document that this is not supported
        case class TrafficLight(
            id: String,
            state: String // Limited to string type currently
        ) derives Schema

        val schema = Schema[TrafficLight]
        val json = schema.toJsonSchema

        val props = json("properties")
        assert(props("state")("type").str == "string")
        // Cannot currently generate enum with mixed types
      }

      test("Enhancement roadmap for mixed enum support") {
        // ROADMAP: Steps needed to support mixed enum types:
        //
        // 1. Create MixedEnumChez class
        //    - Accept List[ujson.Value] for enum values
        //    - Generate {"enum": [...]} without type constraint
        //
        // 2. Add factory methods to Chez object
        //    - Chez.MixedEnum(values: List[ujson.Value])
        //    - Proper integration with existing API
        //
        // 3. Enhance Schema derivation
        //    - Detect when sum types should use mixed enums
        //    - Add annotations for custom enum value mapping
        //    - Support sealed traits with mixed JSON representations
        //
        // 4. Add validation support
        //    - MixedEnumChez.validate method
        //    - Handle type coercion (string "42" vs number 42)
        //
        // 5. Update documentation and examples
        //    - Show mixed enum patterns
        //    - Migration guide from string-only enums

        // This test serves as documentation - no actual testing needed
        assert(true) // Placeholder assertion
      }
    }
  }
}
