# Chez: Core JSON Schema Library

Chez is a comprehensive JSON Schema library for Scala 3 that provides TypeBox-like ergonomics with compile-time type safety and runtime JSON Schema compliance.

## Schema Derivation (Recommended)

The main hotness of Chez is automatic schema derivation from case classes with annotations:

```scala
import chez.derivation.Schema

case class User(
  @Schema.description("User ID")
  @Schema.pattern("^[a-zA-Z0-9]+$")
  id: String,
  
  @Schema.description("User's full name")
  @Schema.minLength(1)
  @Schema.maxLength(100)
  name: String,
  
  @Schema.description("Email address")
  @Schema.format("email")
  email: String,
  
  @Schema.description("User's age")
  @Schema.minimum(0)
  @Schema.maximum(120)
  @Schema.default(18)
  age: Int,
  
  @Schema.description("Whether the user is active")
  @Schema.default(true)
  isActive: Boolean
) derives Schema

// Generate JSON Schema automatically
val userSchema = Schema[User]
val jsonSchema = userSchema.toJsonSchema

// Validation works automatically
val validationResult = userSchema.validate(userData)
```

### Available Annotations

```scala
// Basic metadata
@Schema.title("User Schema")
@Schema.description("A user object")

// String constraints
@Schema.minLength(1)
@Schema.maxLength(100)
@Schema.pattern("^[a-zA-Z]+$")
@Schema.format("email") // email, uri, uuid, date, time, date-time

// Number constraints
@Schema.minimum(0.0)
@Schema.maximum(100.0)
@Schema.exclusiveMinimum(0.0)
@Schema.exclusiveMaximum(100.0)
@Schema.multipleOf(0.1)

// Array constraints
@Schema.minItems(1)
@Schema.maxItems(10)
@Schema.uniqueItems(true)

// Enum and const values
@Schema.enumValues("active", "inactive", "pending")
@Schema.const("fixed-value")

// Default values (type-safe)
@Schema.default("hello")     // String
@Schema.default(42)          // Int
@Schema.default(true)        // Boolean
@Schema.default(3.14)        // Double

// Documentation
@Schema.examples("example1", "example2")
@Schema.readOnly(true)
@Schema.writeOnly(true)
@Schema.deprecated(true)
```

## Features

- 🎯 **Full JSON Schema 2020-12 Compliance**: Supports all vocabularies including core, validation, meta-data, format, content, and composition
- 🚀 **Scala 3 Powered**: Leverages match types, union types, and modern Scala 3 features
- 🔧 **TypeBox-like API**: Familiar syntax for developers coming from TypeScript
- 💎 **Compile-time Type Safety**: Schema definitions provide compile-time type information
- 🌟 **Pragmatic Null Handling**: Distinguishes between optional fields and nullable values
- 🎨 **Composition Support**: Full support for anyOf, oneOf, allOf, not, and conditional schemas
- 🏗️ **Builder Pattern**: Fluent API for schema metadata (title, description, examples, etc.)

## Manual Schema Construction (Lower Level)

For more control, you can construct schemas manually:

### Primitive Types

```scala
import chez.*

// String with constraints
val stringSchema = Chez.String(
  minLength = Some(1),
  maxLength = Some(100),
  pattern = Some("^[a-zA-Z]+$")
)

// Number with constraints
val numberSchema = Chez.Number(
  minimum = Some(0),
  maximum = Some(100),
  multipleOf = Some(0.1)
)

// Integer constraints
val integerSchema = Chez.Integer(
  minimum = Some(0),
  maximum = Some(999),
  multipleOf = Some(2)
)

// Boolean schema
val booleanSchema = Chez.Boolean()

// Null schema
val nullSchema = Chez.Null()
```

### Complex Types

```scala
// Object schema
val userSchema = Chez.Object(
  "id" -> Chez.String(),
  "name" -> Chez.String(minLength = Some(1)),
  "email" -> Chez.String(format = Some("email")),
  "age" -> Chez.Integer(minimum = Some(0)).optional
)

// Array schema
val tagsSchema = Chez.Array(
  items = Chez.String(),
  minItems = Some(1),
  maxItems = Some(10),
  uniqueItems = Some(true)
)

// Generate JSON Schema
val jsonSchema = userSchema.toJsonSchema
println(ujson.write(jsonSchema, indent = 2))
```

## Null Handling

Chez provides pragmatic null handling that distinguishes between optional fields and nullable values:

```scala
// Regular field (required, non-null)
val name = Chez.String()  // String in Scala

// Optional field (may be absent from JSON)
val nickname = Chez.String().optional  // Option[String] in Scala

// Nullable field (present but may be null)
val middleName = Chez.String().nullable  // Option[String] in Scala

// Optional AND nullable (may be absent OR null)
val suffix = Chez.String().optional.nullable  // Option[String] in Scala
```

## Metadata and Builder Pattern

```scala
// Add metadata to schemas using fluent builder pattern
val userSchema = Chez.Object(
  "id" -> Chez.String(),
  "name" -> Chez.String()
)
.withTitle("User Schema")
.withDescription("A schema representing a user")
.withId("https://example.com/schemas/user")
.withSchema("https://json-schema.org/draft/2020-12/schema")
.withExamples(
  ujson.Obj("id" -> "123", "name" -> "John Doe")
)

// Generates complete JSON Schema with metadata:
// {
//   "type": "object",
//   "properties": {...},
//   "title": "User Schema",
//   "description": "A schema representing a user",
//   "$id": "https://example.com/schemas/user",
//   "$schema": "https://json-schema.org/draft/2020-12/schema",
//   "examples": [{"id": "123", "name": "John Doe"}]
// }
```

## JSON Schema 2020-12 Composition

### AnyOf, OneOf, AllOf

```scala
// AnyOf - one or more must match
val anyOfSchema = Chez.AnyOf(
  Chez.String(),
  Chez.Number(),
  Chez.Boolean()
)

// OneOf - exactly one must match
val oneOfSchema = Chez.OneOf(
  Chez.String(format = Some("email")),
  Chez.String(format = Some("uri"))
)

// AllOf - all must match
val allOfSchema = Chez.AllOf(
  Chez.Object("name" -> Chez.String()),
  Chez.Object("age" -> Chez.Integer()),
  Chez.Object("email" -> Chez.String())
)
```

### Not and Conditional Schemas

```scala
// Not - must not match
val notStringSchema = Chez.Not(Chez.String())

// Conditional schemas (if/then/else)
val conditionalSchema = Chez.If(
  condition = Chez.Object("type" -> Chez.String()),
  thenSchema = Chez.Object("name" -> Chez.String()),
  elseSchema = Chez.Object("id" -> Chez.Integer())
)
```

## Advanced Features

### Pattern Properties

```scala
val dynamicSchema = Chez.Object(
  properties = Map("version" -> Chez.String()),
  patternProperties = Map(
    "^feature_.*" -> Chez.Boolean(),
    "^cache_.*" -> Chez.Object(
      "ttl" -> Chez.Integer(minimum = Some(0)),
      "enabled" -> Chez.Boolean()
    )
  ),
  additionalProperties = Some(false)
)
```

### Schema References

```scala
// JSON Pointer references
val refSchema = Chez.Ref("#/$defs/User")

// Dynamic references
val dynamicRefSchema = Chez.DynamicRef("#user")

// Schema with definitions
val schemaWithDefs = Chez.Object(
  "user" -> Chez.Ref("#/$defs/User")
).withDefs(
  "User" -> Chez.Object(
    "id" -> Chez.String(),
    "name" -> Chez.String()
  )
)
```

### Validation

```scala
import chez.validation.*

val schema = Chez.Object(
  "name" -> Chez.String(minLength = Some(1)),
  "age" -> Chez.Integer(minimum = Some(0))
)

// Validate JSON data
val validData = ujson.Obj("name" -> "John", "age" -> 30)
val invalidData = ujson.Obj("name" -> "", "age" -> -5)

val validationResult1 = schema.validate(validData)
val validationResult2 = schema.validate(invalidData)

// Check results
assert(validationResult1.isEmpty) // Valid
assert(validationResult2.nonEmpty) // Invalid - returns validation errors
```

## Architecture

### Core Components

- **Core Trait**: `Chez` trait provides the base functionality
- **Primitive Types**: `StringChez`, `NumberChez`, `IntegerChez`, `BooleanChez`, `NullChez`
- **Complex Types**: `ObjectChez`, `ArrayChez`
- **Composition Types**: `AnyOfChez`, `OneOfChez`, `AllOfChez`, `NotChez`, `IfThenElseChez`
- **References**: `RefChez`, `DynamicRefChez`, `DefsChez`
- **Modifiers**: `OptionalChez`, `NullableChez`, `DefaultChez`
- **Metadata**: `TitleChez`, `DescriptionChez`, `SchemaChez`, `IdChez`, `ExamplesChez`

### Type System Integration

```scala
// Chez provides compile-time type information
type UserType = ChezType[typeof(userSchema)]
// UserType = { id: String, name: String, email: String, age?: Int }

// Integration with upickle for JSON serialization
given ReadWriter[UserType] = userSchema.readWriter
```

## JSON Schema 2020-12 Compliance

Chez provides full compliance with JSON Schema Draft 2020-12, including:

- **Core Vocabulary**: `$schema`, `$id`, `$ref`, `$defs`, `$dynamicRef`, `$dynamicAnchor`
- **Validation Vocabulary**: All validation keywords (type, properties, required, etc.)
- **Meta-Data Vocabulary**: `title`, `description`, `examples`, `default`
- **Format Vocabulary**: Format validation with annotation/assertion modes
- **Content Vocabulary**: `contentMediaType`, `contentEncoding`, `contentSchema`
- **Composition Keywords**: `anyOf`, `oneOf`, `allOf`, `not`, `if`/`then`/`else`
- **Array Keywords**: `prefixItems`, `items`, `minItems`, `maxItems`, `uniqueItems`
- **Object Keywords**: `properties`, `required`, `additionalProperties`, `patternProperties`

## Examples

See the `Chez/src/main/scala/chez/examples/` package for comprehensive usage examples:

- `BasicUsage.scala` - Core functionality demonstration
- `LihaoyiEcosystem.scala` - Integration with lihaoyi ecosystem
- `ComplexTypes.scala` - Advanced schema composition
- `Validation.scala` - Schema validation examples
- `MirrorDerivedExamples.scala` - Schema derivation with annotations

For HTTP integration, see `CaskChez/src/main/scala/caskchez/examples/UserCrudAPI.scala` - a complete user management API.

## Running Examples

```bash
# Run Chez core examples
./mill Chez.runMain chez.examples.BasicUsage
./mill Chez.runMain chez.examples.LihaoyiEcosystem
./mill Chez.runMain chez.examples.ComplexTypes

# Run CaskChez CRUD API example
./mill CaskChez.runMain caskchez.examples.UserCrudAPI
```
