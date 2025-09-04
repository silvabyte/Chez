# Chez (Core): Type‑Safe JSON Schema for Scala 3

Define schemas next to your types with annotations, derive JSON Schema automatically, and validate JSON at runtime.

## Install

Mill:

```scala
mvn"com.silvabyte::chez:0.2.0"
```

SBT:

```scala
libraryDependencies += "com.silvabyte" %% "chez" % "0.2.0"
```

## Quickstart

```scala
import chez.derivation.Schema
import ujson.*

case class User(
  @Schema.minLength(1) name: String,
  @Schema.format("email") email: String,
  @Schema.minimum(0) age: Int
) derives Schema

val schema = Schema[User]
val json = Obj("name"->"Ada","email"->"ada@lovelace.org","age"->28)
schema.validate(json) // Right(()) or Left(List(...))
```

## Annotations (common)

- Strings: `minLength`, `maxLength`, `pattern`, `format`
- Numbers: `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`, `multipleOf`
- Arrays: `minItems`, `maxItems`, `uniqueItems`
- Metadata: `title`, `description`, `examples`, `deprecated`, `readOnly`, `writeOnly`
- Defaults: `default(value)` (type‑safe)

## Unions, Option, Nulls

- `A | B` → JSON Schema `oneOf`
- `Option[T]` → optional field (may be absent)
- Nullable: `@Schema.nullable(true)` to allow null for present fields

## Tips

- Validate at boundaries and use errors to drive responses.
- Pair with upickle for read/write when you need typed data.

