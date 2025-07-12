# Missing Types Implementation Plan for Chez

**Status**: Planning Phase  
**Priority**: High (Production Readiness)  
**Estimated Effort**: 2-3 weeks  

## Executive Summary

Chez has excellent core schema derivation capabilities but is missing support for critical Scala collection types and enums that are essential for production usage. This document outlines a systematic plan to implement the missing types and fix existing issues.

## Current State Analysis

### ✅ **What Works Well**
- **Primitive Types**: String, Integer, Number, Boolean, Null (comprehensive)
- **Complex Types**: Array, Object with full JSON Schema 2020-12 features  
- **Composition**: OneOf, AllOf, AnyOf, If-Then-Else, Not, References
- **Annotations**: Rich metadata and validation annotation system
- **Basic Derivation**: Case classes, nested objects, optional fields, generics

### ❌ **Critical Gaps Identified**
1. **Collection Types**: Map, Set, Vector - No schema derivation support
2. **Enum Support**: Scala 3 enums fail compilation during derivation
3. **Sealed Traits**: ADT support incomplete/placeholder implementation  
4. **Union Types**: Either[A,B] not supported
5. **Default Value Handling**: BROKEN - fields with defaults still marked required
6. **Scala Default Detection**: Cannot detect case class parameter defaults (e.g., `field: String = "default"`)

### 🐛 **Current Issues** (After systematic test fixing)
- **PROGRESS**: Test success rate improved from ~145/157 to **150/157** (95.5% pass rate)
- **Default Value Bug**: Fields with `@Schema.default("value")` annotations still marked required (4 tests)
- **Scala Default Detection**: Cannot detect `field: Type = defaultValue` syntax in case classes  
- **Enum Annotation**: ❌ enum annotation parsing broken - needs different List[String] approach (2 tests)
- **Validation Logic**: Missing MaxItemsViolation implementation (1 test)
- **Nested Patterns**: Pattern annotation not applied in deeply nested objects (1 test)

### ✅ **FIXED Issues**
- **Examples Serialization**: Fixed `withExamples()` calls across test files  
- **Annotation Processing**: Added missing `multipleOf`, `exclusiveMinimum/Maximum`, `const` annotations
- **Array Metadata**: Fixed examples serialization in ArrayChezTests

## Implementation Plan

### **Phase 1: Fix Foundation Issues** (Week 1)
*Priority: Critical - Fix existing broken functionality*

#### 1.1 Fix Default Value Handling
**Issue**: Fields with default values incorrectly marked as required in generated schemas

**Root Cause**: `deriveProductWithAnnotations` doesn't properly handle Scala default values

**Solution**:
```scala
// In SchemaDerivation.scala
private def getDefaultValue[T](fieldName: String, mirror: Mirror.ProductOf[T]): Option[ujson.Value] = {
  // Use reflection to check if field has default value
  // Return None if no default, Some(json) if default exists
}

private def isFieldRequired[T](fieldName: String, fieldType: Type, hasDefault: Boolean): Boolean = {
  // Field is required if:
  // 1. Not Option[_] type AND
  // 2. No default value defined
  !isOptionType(fieldType) && !hasDefault
}
```

**Tests Required**:
- Case class with defaults should not have required fields
- Mixed required/optional/default fields
- Complex default values (collections, objects)

#### 1.2 Fix Examples Serialization
**Issue**: Array examples not properly serialized in JSON Schema output

**Solution**: Update `toJsonSchema` methods to properly handle ujson.Value arrays

#### 1.3 Annotation Processing Fixes  
**Issue**: Some annotations not properly applied during derivation

**Solution**: Review and fix annotation processing pipeline in `deriveProductWithAnnotations`

### **Phase 2: Collection Types Implementation** (Week 1-2)
*Priority: High - Essential for production usage*

#### 2.1 Map[K, V] Support
**Target**: Support Map types in schema derivation

**JSON Schema Mapping**:
```scala
Map[String, V] => {
  "type": "object",
  "additionalProperties": <schema for V>
}

Map[K, V] where K != String => {
  "type": "object", 
  "patternProperties": {
    ".*": <schema for V>
  },
  "description": "Map with keys of type K"
}
```

**Implementation**:
```scala
// In SchemaDerivation.scala
given [K, V](using kSchema: Schema[K], vSchema: Schema[V]): Schema[Map[K, V]] = {
  if (kSchema.isStringType) {
    Schema(ObjectChez(additionalProperties = Some(vSchema.chez)))
  } else {
    Schema(ObjectChez(
      patternProperties = Map(".*" -> vSchema.chez),
      description = Some(s"Map with ${kSchema.description} keys")
    ))
  }
}
```

#### 2.2 Set[T] Support
**Target**: Set types as arrays with unique items

**JSON Schema Mapping**:
```scala
Set[T] => {
  "type": "array",
  "items": <schema for T>,
  "uniqueItems": true
}
```

**Implementation**:
```scala
given [T](using tSchema: Schema[T]): Schema[Set[T]] = {
  Schema(ArrayChez(tSchema.chez, uniqueItems = Some(true)))
}
```

#### 2.3 Vector[T] Support  
**Target**: Vector types as arrays (same as List)

**Implementation**:
```scala
given [T](using tSchema: Schema[T]): Schema[Vector[T]] = {
  Schema(ArrayChez(tSchema.chez))
}
```

### **Phase 3: Enum and ADT Support** (Week 2)
*Priority: High - Type safety for APIs*

#### 3.1 Scala 3 Enum Derivation
**Issue**: Current enum derivation causes compilation failures

**Target Schema**:
```scala
enum Status { case Active, Inactive, Pending }
// Should generate:
{
  "type": "string",
  "enum": ["Active", "Inactive", "Pending"]
}
```

**Implementation Strategy**:
```scala
// Fix derivation for Mirror.SumOf[T] where T is enum
private def deriveEnum[T](sum: Mirror.SumOf[T]): Chez = {
  val enumValues = getEnumCaseNames[T](sum)
  StringChez(enumValues = Some(enumValues))
}

private def getEnumCaseNames[T](sum: Mirror.SumOf[T]): List[String] = {
  // Use compile-time reflection to extract case names
  // Return list of string representations
}
```

#### 3.2 Sealed Trait Support (Discriminated Unions)
**Target**: ADTs as OneOf with discriminator

**Example**:
```scala
sealed trait Event
case class UserEvent(userId: String) extends Event  
case class SystemEvent(level: String) extends Event

// Should generate OneOf with discriminator:
{
  "oneOf": [
    {
      "type": "object",
      "properties": {
        "type": {"const": "UserEvent"},
        "userId": {"type": "string"}
      }
    },
    {
      "type": "object", 
      "properties": {
        "type": {"const": "SystemEvent"},
        "level": {"type": "string"}
      }
    }
  ],
  "discriminator": {"propertyName": "type"}
}
```

### **Phase 4: Union Types** (Week 3)
*Priority: Medium - Common patterns*

#### 4.1 Either[A, B] Support
**Target**: Either as AnyOf composition

**JSON Schema Mapping**:
```scala
Either[A, B] => {
  "anyOf": [
    <schema for A>,
    <schema for B>
  ]
}
```

#### 4.2 Try[T] Support (Optional)
**Target**: Success/failure pattern

**JSON Schema Mapping**:
```scala
Try[T] => {
  "anyOf": [
    <schema for T>,
    {
      "type": "object",
      "properties": {
        "error": {"type": "string"}
      }
    }
  ]
}
```

## Implementation Details

### **File Structure**
```
chez/derivation/
├── SchemaDerivation.scala       # Core derivation logic (modify)
├── CollectionSchemas.scala      # New: Map, Set, Vector given instances  
├── EnumDerivation.scala         # New: Enum and sealed trait derivation
├── UnionTypeSchemas.scala       # New: Either, Try support
└── DefaultValueHandling.scala   # New: Proper default value detection
```

### **Test Strategy**
```
chez/test/derivation/
├── CollectionDerivationTests.scala     # Map, Set, Vector tests
├── EnumDerivationTests.scala           # Enum derivation tests  
├── SealedTraitDerivationTests.scala    # ADT tests
├── DefaultValueTests.scala             # Fix default handling tests
└── UnionTypeTests.scala               # Either, Try tests
```

## Success Criteria

### **Acceptance Tests**
1. ✅ **Map Support**: `Map[String, Int]` generates correct object schema
2. ✅ **Set Support**: `Set[String]` generates array with uniqueItems
3. ✅ **Enum Support**: Scala 3 enums generate string enum schemas
4. ✅ **Sealed Trait Support**: ADTs generate discriminated unions
5. ✅ **Default Values**: Fields with defaults not marked required
6. ✅ **All Tests Pass**: No compilation errors or test failures

### **Performance Requirements**
- Schema derivation compile time < 5 seconds for complex types
- Generated JSON Schema size reasonable (< 10KB for typical cases)
- No runtime reflection (compile-time only)

### **API Compatibility**
- Backward compatible with existing Schema[T] derivations
- No breaking changes to annotation API
- Consistent with existing error handling

## Risk Mitigation

### **Technical Risks**
1. **Scala 3 Mirror Limitations**: Some advanced enum patterns may not be derivable
   - *Mitigation*: Document limitations, provide manual derivation escape hatch

2. **Compilation Performance**: Complex derivation could slow builds
   - *Mitigation*: Optimize derivation code, add compilation benchmarks

3. **JSON Schema Size**: Complex ADTs could generate very large schemas
   - *Mitigation*: Add schema size warnings, provide reference-based alternatives

### **Backward Compatibility**
- All existing tests must continue to pass
- Existing annotation APIs unchanged
- Migration guide for any behavior changes

## Timeline

| Week | Phase | Deliverables |
|------|--------|--------------|
| 1 | Foundation Fixes | Default value handling, examples serialization, passing tests |
| 1-2 | Collections | Map, Set, Vector derivation with comprehensive tests |
| 2 | Enums/ADTs | Scala 3 enum support, sealed trait discriminated unions |
| 3 | Union Types | Either, Try support, documentation updates |

## Next Steps

1. **Create feature branch**: `feature/missing-types-implementation`
2. **Start with Phase 1**: Fix default value handling (highest priority)
3. **Add comprehensive tests**: For each new type as implemented
4. **Update documentation**: Include new supported types in README
5. **Performance testing**: Ensure derivation performance acceptable

## Appendix: Current Test Failures

From test execution, these specific issues need addressing:

1. **SchemaDerivationTests**: Default value tests failing
2. **AnnotationExample**: Array annotation constraints not applied
3. **ComplexTypes**: Nested derivation edge cases
4. **MirrorDerivedExamples**: Recursive structure infinite loops

Each will be addressed in the corresponding implementation phase.