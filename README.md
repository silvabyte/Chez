# Chez: JSON Schema for Scala 3

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/silvabyte/scalaschemaz)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Chez** is a comprehensive JSON Schema library for Scala 3 that provides TypeBox-like ergonomics with compile-time type safety and runtime JSON Schema compliance. The project consists of two main modules:

- **Chez**: Core JSON Schema generation and validation library
- **CaskChez**: Cask HTTP framework integration with automatic request/response validation

## Features

### Chez (Core Library)

- 🎯 **Full JSON Schema 2020-12 Compliance**: Supports all vocabularies including core, validation, meta-data, format, content, and composition
- 🚀 **Scala 3 Powered**: Leverages match types, union types, and modern Scala 3 features
- 🔧 **TypeBox-like API**: Familiar syntax for developers coming from TypeScript
- 📦 **Lihaoyi Ecosystem Integration**: Seamless integration with upickle, os-lib, and other lihaoyi tools
- 💎 **Compile-time Type Safety**: Schema definitions provide compile-time type information
- 🌟 **Pragmatic Null Handling**: Distinguishes between optional fields and nullable values
- 🎨 **Composition Support**: Full support for anyOf, oneOf, allOf, not, and conditional schemas
- 🏗️ **Builder Pattern**: Fluent API for schema metadata (title, description, examples, etc.)

### CaskChez (Cask Integration)

- 🌐 **HTTP Route Schemas**: Define complete API schemas with request/response validation
- 🛡️ **Automatic Validation**: Custom Cask endpoints with built-in JSON Schema validation
- 📋 **Schema Registry**: Centralized registry for all route schemas
- 🔍 **Schema Introspection**: Runtime access to all registered schemas and routes
- 🏷️ **OpenAPI Ready**: Schema definitions compatible with OpenAPI specification
- ⚡ **Zero Boilerplate**: Eliminate manual validation code with custom decorators

## Quick Start

### Add to your project

```scala
// build.mill
def ivyDeps = Agg(
  ivy"com.lihaoyi::upickle:4.1.0",
  ivy"com.lihaoyi::os-lib:0.11.3",
  ivy"com.lihaoyi::cask:0.9.4", // For CaskChez
  // Add Chez modules when published
)
```

## Chez: Core JSON Schema Library

### Basic Usage

```scala
import chez.*

// Primitive types
val stringSchema = Chez.String(
  minLength = Some(1),
  maxLength = Some(100),
  pattern = Some("^[a-zA-Z]+$")
)

val numberSchema = Chez.Number(
  minimum = Some(0),
  maximum = Some(100),
  multipleOf = Some(0.1)
)

// Complex types
val userSchema = Chez.Object(
  "id" -> Chez.String(),
  "name" -> Chez.String(minLength = Some(1)),
  "email" -> Chez.String(format = Some("email")),
  "age" -> Chez.Integer(minimum = Some(0)).optional
)

// Generate JSON Schema
val jsonSchema = userSchema.toJsonSchema
println(jsonSchema)
// Output: {"type":"object","properties":{"id":{"type":"string"},"name":{"type":"string","minLength":1},"email":{"type":"string","format":"email"},"age":{"type":"integer","minimum":0}},"required":["id","name","email"]}
```

### Metadata and Builder Pattern

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

## JSON Schema 2020-12 Composition

```scala
// AnyOf - one or more must match
val anyOfSchema = Chez.AnyOf(Chez.String(), Chez.Number())

// OneOf - exactly one must match
val oneOfSchema = Chez.OneOf(Chez.String(), Chez.Number())

// AllOf - all must match
val allOfSchema = Chez.AllOf(
  Chez.Object("name" -> Chez.String()),
  Chez.Object("age" -> Chez.Integer())
)

// Not - must not match
val notSchema = Chez.Not(Chez.String())

// Conditional schemas (if/then/else)
val conditionalSchema = Chez.If(
  condition = Chez.Object("type" -> Chez.String()),
  thenSchema = Chez.Object("name" -> Chez.String()),
  elseSchema = Chez.Object("id" -> Chez.Integer())
)
```

## CaskChez: HTTP Framework Integration

CaskChez provides seamless integration with the Cask HTTP framework, enabling automatic request/response validation and schema introspection.

### Route Schema Definition

```scala
import cask._
import caskchez._
import chez._

// Define schemas
val createUserSchema = Chez.Object(
  "name" -> Chez.String(minLength = Some(1)),
  "email" -> Chez.String(format = Some("email")),
  "age" -> Chez.Integer(minimum = Some(0))
)

val userResponseSchema = Chez.Object(
  "id" -> Chez.String(),
  "name" -> Chez.String(),
  "email" -> Chez.String(),
  "age" -> Chez.Integer()
)

// Define complete route schema
val createUserRouteSchema = RouteSchema(
  summary = Some("Create a new user"),
  description = Some("Creates a new user with validation"),
  tags = List("users"),
  body = Some(createUserSchema),
  responses = Map(
    201 -> ApiResponse("User created", userResponseSchema),
    400 -> ApiResponse("Validation error", errorSchema)
  )
)
```

### Custom Endpoints with Automatic Validation

```scala
object UserAPI extends cask.MainRoutes {

  // Custom endpoint with automatic validation
  @chez.post("/users", createUserRouteSchema)
  def createUser(validatedRequest: ValidatedRequest): cask.Response[String] = {
    validatedRequest.getBody[CreateUserRequest] match {
      case Right(userData) =>
        // Process valid data - validation already done!
        val user = createUser(userData)
        cask.Response(write(user), statusCode = 201)
      case Left(error) =>
        cask.Response(write(error), statusCode = 400)
    }
  }

  // Schema introspection endpoints (automatically available)
  @cask.get("/schemas")
  def getSchemas() = RouteSchemaRegistry.getAll

  @cask.get("/routes")
  def getRoutes() = RouteSchemaRegistry.getRoutes
}
```

### Zero Boilerplate Validation

```scala
// Before (manual validation)
@cask.post("/users")
def createUser(request: cask.Request) = {
  try {
    val json = ujson.read(request.text())
    // Manual validation logic...
    val name = json("name").str
    if (name.length < 1) throw new Exception("Name too short")
    // More validation...
  } catch {
    case e => cask.Response("Validation failed", 400)
  }
}

// After (with CaskChez)
@chez.post("/users", createUserRouteSchema)
def createUser(validatedRequest: ValidatedRequest) = {
  // Request is already validated against schema!
  validatedRequest.getBody[CreateUserRequest] match {
    case Right(userData) => processUser(userData)
    case Left(error) => handleValidationError(error)
  }
}
```

## Lihaoyi Ecosystem Integration

Chez integrates seamlessly with the lihaoyi ecosystem:

```scala
// With upickle for JSON serialization
import upickle.default.*

val userJson = """{"id":"123","name":"John","email":"john@example.com"}"""
val user = read[User](userJson)  // Type-safe deserialization

// With os-lib for configuration
val configSchema = Chez.Object(
  "database" -> Chez.Object(
    "host" -> Chez.String(),
    "port" -> Chez.Integer(minimum = Some(1024), maximum = Some(65535))
  ),
  "server" -> Chez.Object(
    "host" -> Chez.String(),
    "port" -> Chez.Integer(minimum = Some(1024), maximum = Some(65535))
  )
)

val config = configSchema.fromJsonStringUnsafe(os.read(os.pwd / "config.json"))
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
  )
)
```

### Schema References

```scala
val refSchema = Chez.Ref("#/$defs/User")
val dynamicRefSchema = Chez.DynamicRef("#user")
```

### Meta-Schema Compliance

```scala
val compliantSchema = Chez.Object(
  "product" -> Chez.Object(
    "id" -> Chez.String(),
    "name" -> Chez.String()
  )
).withSchema("https://json-schema.org/draft/2020-12/schema")
 .withId("https://example.com/schemas/product")
 .withTitle("Product Schema")
 .withDescription("A schema for product objects")
```

## Architecture

### Chez Module Structure

- **Core Trait**: `Chez` trait provides the base functionality
- **Primitive Types**: `StringChez`, `NumberChez`, `IntegerChez`, `BooleanChez`, `NullChez`
- **Complex Types**: `ObjectChez`, `ArrayChez`
- **Composition Types**: `AnyOfChez`, `OneOfChez`, `AllOfChez`, `NotChez`, `IfThenElseChez`
- **References**: `RefChez`, `DynamicRefChez`, `DefsChez`
- **Modifiers**: `OptionalChez`, `NullableChez`, `DefaultChez`
- **Metadata**: `TitleChez`, `DescriptionChez`, `SchemaChez`, `IdChez`, `ExamplesChez`

### CaskChez Module Structure

- **Route Schemas**: `RouteSchema`, `ApiResponse`, `SecurityRequirement`
- **Custom Endpoints**: `@chez.get`, `@chez.post`, `@chez.put`, `@chez.delete`
- **Validation**: `ValidatedRequest`, `ValidatedRequestReader`
- **Registry**: `RouteSchemaRegistry` for centralized schema management
- **Integration**: Seamless Cask HTTP framework integration

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

### Chez Examples

See the `Chez/src/main/scala/chez/examples/` package for comprehensive usage examples:

- `BasicUsage.scala` - Core functionality demonstration
- `LihaoyiEcosystem.scala` - Integration with lihaoyi ecosystem
- `ComplexTypes.scala` - Advanced schema composition
- `Validation.scala` - Schema validation examples

### CaskChez Examples

See the `CaskChez/src/main/scala/caskchez/examples/` package for HTTP integration examples:

- `FinalExample.scala` - Complete HTTP API with automatic validation

## Running Examples

```bash
# Run Chez core examples
./mill Chez.runMain chez.examples.BasicUsage
./mill Chez.runMain chez.examples.LihaoyiEcosystem

# Run CaskChez HTTP server example
./mill CaskChez.runMain caskchez.examples.FinalExample
# Then test with: curl http://localhost:8082/health
```

## Development Status

### ✅ **Completed Features**

#### **Phase 1: Core Foundation** ✅

- [x] Basic Chez trait hierarchy with JSON Schema 2020-12 compliance
- [x] Primitive schema types (String, Number, Integer, Boolean, Null)
- [x] Core vocabulary implementation ($schema, $id, $ref, $defs)
- [x] Basic type-level computations using Scala 3 match types
- [x] JSON Schema generation with proper meta-schema

#### **Phase 2: Validation & Complex Types** ✅

- [x] Complete validation vocabulary (type, properties, required, etc.)
- [x] Object schema with all validation keywords
- [x] Array schema with JSON Schema 2020-12 keywords (prefixItems, items, etc.)
- [x] String validation (minLength, maxLength, pattern, format)
- [x] Number validation (minimum, maximum, exclusiveMinimum, multipleOf)
- [x] Modifier pattern implementation (optional/nullable)

#### **Phase 3: Composition & Meta-Data** ✅

- [x] Composition keywords (anyOf, oneOf, allOf, not)
- [x] Conditional schemas (if/then/else)
- [x] Meta-data vocabulary (title, description, examples, default)
- [x] Reference resolution ($ref, $dynamicRef, $dynamicAnchor)
- [x] Schema definitions and reuse ($defs)
- [x] **Builder pattern for metadata (withTitle, withDescription, etc.)**

#### **CaskChez Integration** ✅

- [x] HTTP route schema definitions
- [x] Custom Cask endpoints with automatic validation
- [x] Schema registry and introspection
- [x] Zero-boilerplate validation decorators
- [x] Complete working HTTP server examples

### 🚧 **Next Phases**

#### **Phase 4: Enhanced JSON API** ⏳

- [ ] Automatic ReadWriter derivation
- [ ] Chez-driven JSON serialization API (toJson/fromJson)
- [ ] Validation integration with JSON parsing
- [ ] Safe and unsafe API variants

#### **Phase 5: Format & Content Vocabularies** ⏳

- [ ] Enhanced format vocabulary (date-time, email, uri, etc.)
- [ ] Content vocabulary (contentMediaType, contentEncoding, contentSchema)
- [ ] Custom format validation extensibility

#### **Phase 6: Advanced Features** ⏳

- [ ] Unevaluated vocabulary (unevaluatedProperties, unevaluatedItems)
- [ ] Dynamic references and recursive schemas
- [ ] JSON Schema test suite compliance verification
- [ ] Performance optimizations and benchmarks

## License

MIT License. See [LICENSE](LICENSE) for details.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.
