package chez.derivation

import upickle.default.*
import ujson.*
import chez.*
import chez.complex.ObjectChez
import chez.complex.ArrayChez
import chez.primitives.*

/**
 * ReadWriter generation for ObjectChez schemas
 * 
 * This module generates ReadWriter[Map[String, Any]] instances for ObjectChez,
 * providing automatic serialization/deserialization of object data.
 */
object ObjectReadWriter {
  
  /**
   * Generate a ReadWriter[Map[String, Any]] for an ObjectChez schema
   */
  def deriveObjectReadWriter(objectChez: ObjectChez): ReadWriter[Map[String, Any]] = {
    val objectInfo = ObjectAnalysis.analyzeObjectSchema(objectChez)
    
    readwriter[ujson.Value].bimap[Map[String, Any]](
      // Writer: Map[String, Any] -> ujson.Value
      objectMap => writeObjectToJson(objectMap, objectInfo),
      // Reader: ujson.Value -> Map[String, Any]  
      json => readObjectFromJson(json, objectInfo)
    )
  }
  
  /**
   * Write a Map[String, Any] to JSON using the object schema
   */
  private def writeObjectToJson(objectMap: Map[String, Any], objectInfo: ObjectAnalysis.ObjectInfo): ujson.Value = {
    val jsonObj = ujson.Obj()
    
    // Write each field according to its schema
    objectInfo.fields.foreach { fieldInfo =>
      objectMap.get(fieldInfo.name) match {
        case Some(value) =>
          // Field is present - serialize using field's schema
          try {
            val serializedValue = writeValueWithSchema(value, fieldInfo.schema)
            jsonObj(fieldInfo.name) = serializedValue
          } catch {
            case e: Exception =>
              throw new RuntimeException(s"Failed to serialize field '${fieldInfo.name}': ${e.getMessage}", e)
          }
        case None =>
          // Field is missing
          if (fieldInfo.required && !fieldInfo.hasDefault) {
            throw new RuntimeException(s"Required field '${fieldInfo.name}' is missing")
          }
          // Optional fields are simply omitted from JSON
      }
    }
    
    jsonObj
  }
  
  /**
   * Read a Map[String, Any] from JSON using the object schema
   */
  private def readObjectFromJson(json: ujson.Value, objectInfo: ObjectAnalysis.ObjectInfo): Map[String, Any] = {
    json match {
      case obj: ujson.Obj =>
        val resultMap = scala.collection.mutable.Map[String, Any]()
        
        // Read each field according to its schema
        objectInfo.fields.foreach { fieldInfo =>
          obj.value.get(fieldInfo.name) match {
            case Some(jsonValue) =>
              // Field is present - deserialize using field's schema
              try {
                val deserializedValue = readValueWithSchema(jsonValue, fieldInfo.schema)
                resultMap(fieldInfo.name) = deserializedValue
              } catch {
                case e: Exception =>
                  throw new RuntimeException(s"Failed to deserialize field '${fieldInfo.name}': ${e.getMessage}", e)
              }
            case None =>
              // Field is missing
              if (fieldInfo.required && !fieldInfo.hasDefault) {
                throw new RuntimeException(s"Required field '${fieldInfo.name}' is missing from JSON")
              }
              // Optional fields are omitted from the result map
          }
        }
        
        scala.collection.immutable.Map.from(resultMap)
        
      case _ =>
        throw new RuntimeException(s"Expected JSON object, got ${json.getClass.getSimpleName}")
    }
  }
  
  /**
   * Write a value using a specific Chez schema (recursive)
   */
  def writeValueWithSchema(value: Any, schema: Chez): ujson.Value = {
    schema match {
      case _: StringChez => 
        value match {
          case s: String => ujson.Str(s)
          case _ => throw new RuntimeException(s"Expected String, got ${value.getClass.getSimpleName}")
        }
        
      case _: NumberChez =>
        value match {
          case d: Double => ujson.Num(d)
          case f: Float => ujson.Num(f.toDouble)
          case _ => throw new RuntimeException(s"Expected Double, got ${value.getClass.getSimpleName}")
        }
        
      case _: IntegerChez =>
        value match {
          case i: Int => ujson.Num(i.toDouble)
          case l: Long => ujson.Num(l.toDouble)
          case _ => throw new RuntimeException(s"Expected Int, got ${value.getClass.getSimpleName}")
        }
        
      case _: BooleanChez =>
        value match {
          case b: Boolean => if (b) ujson.True else ujson.False
          case _ => throw new RuntimeException(s"Expected Boolean, got ${value.getClass.getSimpleName}")
        }
        
      case _: NullChez =>
        ujson.Null
        
      case objSchema: ObjectChez =>
        value match {
          case map: Map[String, Any] @unchecked =>
            val objInfo = ObjectAnalysis.analyzeObjectSchema(objSchema)
            writeObjectToJson(map, objInfo)
          case _ => throw new RuntimeException(s"Expected Map[String, Any] for object, got ${value.getClass.getSimpleName}")
        }
        
      case arraySchema: ArrayChez[?] =>
        value match {
          case list: List[Any] @unchecked =>
            ujson.Arr(list.map(item => writeValueWithSchema(item, arraySchema.items))*)
          case _ => throw new RuntimeException(s"Expected List[Any] for array, got ${value.getClass.getSimpleName}")
        }
        
      case _ =>
        // For unsupported schema types, convert value to ujson.Value based on its runtime type
        value match {
          case s: String => ujson.Str(s)
          case i: Int => ujson.Num(i.toDouble)
          case l: Long => ujson.Num(l.toDouble)
          case f: Float => ujson.Num(f.toDouble)
          case d: Double => ujson.Num(d)
          case b: Boolean => if (b) ujson.True else ujson.False
          case null => ujson.Null
          case map: Map[String, Any] @unchecked =>
            // Convert Map to JSON object
            val obj = ujson.Obj()
            map.foreach { case (k, v) =>
              obj(k) = writeValueWithSchema(v, Chez.String()) // Use string schema for unknown values
            }
            obj
          case list: List[Any] @unchecked =>
            ujson.Arr(list.map(v => writeValueWithSchema(v, Chez.String()))*)
          case _ =>
            // Last resort: convert to string
            ujson.Str(value.toString)
        }
    }
  }
  
  /**
   * Read a value using a specific Chez schema (recursive)
   */
  def readValueWithSchema(json: ujson.Value, schema: Chez): Any = {
    schema match {
      case _: StringChez =>
        json.str
        
      case _: NumberChez =>
        json.num
        
      case _: IntegerChez =>
        json.num.toInt
        
      case _: BooleanChez =>
        json.bool
        
      case _: NullChez =>
        null
        
      case objSchema: ObjectChez =>
        val objInfo = ObjectAnalysis.analyzeObjectSchema(objSchema)
        readObjectFromJson(json, objInfo)
        
      case arraySchema: ArrayChez[?] =>
        json match {
          case arr: ujson.Arr =>
            arr.arr.map(itemJson => readValueWithSchema(itemJson, arraySchema.items)).toList
          case _ =>
            throw new RuntimeException(s"Expected JSON array, got ${json.getClass.getSimpleName}")
        }
      
      // Handle optional wrappers by unwrapping to underlying schema
      case OptionalChez(underlying) =>
        readValueWithSchema(json, underlying)
        
      case NullableChez(underlying) =>
        readValueWithSchema(json, underlying)
        
      case OptionalNullableChez(underlying) =>
        readValueWithSchema(json, underlying)
        
      case DefaultChez(underlying, _) =>
        readValueWithSchema(json, underlying)
        
      case _ =>
        // For completely unsupported schema types, try to convert JSON to basic Scala types
        json match {
          case ujson.Str(s) => s
          case ujson.Num(n) => n
          case ujson.Bool(b) => b
          case ujson.Null => null
          case arr: ujson.Arr => arr.arr.toList
          case obj: ujson.Obj => obj.value.toMap
        }
    }
  }
}