# Concepts

## Optional vs Nullable

- Optional: `Option[T]` → field may be absent
- Nullable: annotate a present field as nullable (e.g., `@Schema.nullable(true)`)

## Unions and OneOf

- Scala union types (e.g., `A | B`) map to JSON Schema `oneOf`

## Validation Philosophy

- Encode constraints alongside types using `@Schema.*`
- Validate at boundaries (HTTP body/query/headers/params, or raw JSON)
- Return validation results; avoid exceptions in normal flow

## Annotations Cheatsheet

- Strings: `minLength`, `maxLength`, `pattern`, `format`
- Numbers: `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`, `multipleOf`
- Arrays: `minItems`, `maxItems`, `uniqueItems`
- Metadata: `title`, `description`, `examples`, `deprecated`, `readOnly`, `writeOnly`
- Defaults: `default(value)` (type‑safe)
