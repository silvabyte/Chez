package chez.examples

import chez.*
import chez.derivation.*
import upickle.default.*

/**
 * Example demonstrating automatic ReadWriter derivation from Chez schemas
 * 
 * This example shows how to eliminate manual ReadWriter creation by using
 * the automatic derivation feature.
 */
object ReadWriterDerivationExample {
  
  def main(args: Array[String]): Unit = {
    println("🚀 Chez ReadWriter Derivation Example")
    println("=" * 50)
    
    // Example 1: String schema with automatic ReadWriter
    println("\n1. String Schema with Automatic ReadWriter:")
    testStringSchema()
    
    // Example 2: Number schema with automatic ReadWriter
    println("\n2. Number Schema with Automatic ReadWriter:")
    testNumberSchema()
    
    // Example 3: Integer schema with automatic ReadWriter
    println("\n3. Integer Schema with Automatic ReadWriter:")
    testIntegerSchema()
    
    // Example 4: Boolean schema with automatic ReadWriter
    println("\n4. Boolean Schema with Automatic ReadWriter:")
    testBooleanSchema()
    
    println("\n✅ All ReadWriter derivation examples completed successfully!")
    println("📋 Note: Validation integration (honoring constraints during deserialization) will be added in future batches.")
  }
  
  def testStringSchema(): Unit = {
    val stringSchema = Chez.String(minLength = Some(1), maxLength = Some(100))
    given ReadWriter[String] = stringSchema.deriveReadWriter[String]
    
    val testString = "Hello, World!"
    val jsonString = writeJs(testString)
    val roundTripString = read[String](jsonString)
    
    println(s"Original: $testString")
    println(s"JSON: $jsonString")
    println(s"Roundtrip: $roundTripString")
    println(s"Success: ${testString == roundTripString}")
  }
  
  def testNumberSchema(): Unit = {
    val numberSchema = Chez.Number(minimum = Some(0.0), maximum = Some(100.0))
    given ReadWriter[Double] = numberSchema.deriveReadWriter[Double]
    
    val testNumber = 42.5
    val jsonNumber = writeJs(testNumber)
    val roundTripNumber = read[Double](jsonNumber)
    
    println(s"Original: $testNumber")
    println(s"JSON: $jsonNumber")
    println(s"Roundtrip: $roundTripNumber")
    println(s"Success: ${testNumber == roundTripNumber}")
  }
  
  def testIntegerSchema(): Unit = {
    val integerSchema = Chez.Integer(minimum = Some(1), maximum = Some(1000))
    given ReadWriter[Int] = integerSchema.deriveReadWriter[Int]
    
    val testInteger = 123
    val jsonInteger = writeJs(testInteger)
    val roundTripInteger = read[Int](jsonInteger)
    
    println(s"Original: $testInteger")
    println(s"JSON: $jsonInteger")
    println(s"Roundtrip: $roundTripInteger")
    println(s"Success: ${testInteger == roundTripInteger}")
  }
  
  def testBooleanSchema(): Unit = {
    val booleanSchema = Chez.Boolean()
    given ReadWriter[Boolean] = booleanSchema.deriveReadWriter[Boolean]
    
    val testBoolean = true
    val jsonBoolean = writeJs(testBoolean)
    val roundTripBoolean = read[Boolean](jsonBoolean)
    
    println(s"Original: $testBoolean")
    println(s"JSON: $jsonBoolean")
    println(s"Roundtrip: $roundTripBoolean")
    println(s"Success: ${testBoolean == roundTripBoolean}")
  }
}