package chez.derivation

import upickle.default.*
import chez.*
import chez.primitives.*

/**
 * Core ReadWriter derivation functionality for Chez schemas
 * 
 * This module provides automatic derivation of upickle ReadWriter instances
 * from Chez schema definitions, working in conjunction with the Mirror-based
 * Schema derivation for complete type safety.
 * 
 * With Mirror-based derivation, this is primarily used for primitive types
 * as case classes get their ReadWriter via `derives ReadWriter`.
 */
object ReadWriterDerivation {
  
  /**
   * Derive ReadWriter for StringChez schemas
   */
  def deriveStringReadWriter(schema: StringChez): ReadWriter[String] = {
    // Basic string ReadWriter with potential for future validation
    readwriter[ujson.Value].bimap[String](
      // Writer: String -> ujson.Value
      str => ujson.Str(str),
      // Reader: ujson.Value -> String  
      json => json.str
    )
  }
  
  /**
   * Derive ReadWriter for NumberChez schemas
   */
  def deriveNumberReadWriter(schema: NumberChez): ReadWriter[Double] = {
    readwriter[ujson.Value].bimap[Double](
      // Writer: Double -> ujson.Value
      num => ujson.Num(num),
      // Reader: ujson.Value -> Double
      json => json.num
    )
  }
  
  /**
   * Derive ReadWriter for IntegerChez schemas
   */
  def deriveIntegerReadWriter(schema: IntegerChez): ReadWriter[Int] = {
    readwriter[ujson.Value].bimap[Int](
      // Writer: Int -> ujson.Value
      int => ujson.Num(int.toDouble),
      // Reader: ujson.Value -> Int
      json => json.num.toInt
    )
  }
  
  /**
   * Derive ReadWriter for BooleanChez schemas
   */
  def deriveBooleanReadWriter(schema: BooleanChez): ReadWriter[Boolean] = {
    readwriter[ujson.Value].bimap[Boolean](
      // Writer: Boolean -> ujson.Value
      bool => if (bool) ujson.True else ujson.False,
      // Reader: ujson.Value -> Boolean
      json => json.bool
    )
  }
  
  /**
   * Derive ReadWriter for NullChez schemas
   */
  def deriveNullReadWriter(schema: NullChez): ReadWriter[Null] = {
    readwriter[ujson.Value].bimap[Null](
      // Writer: Null -> ujson.Value
      _ => ujson.Null,
      // Reader: ujson.Value -> Null
      _ => null
    )
  }
}

/**
 * Extension methods to add deriveReadWriter to primitive Chez types
 */
extension (chez: Chez) {
  /**
   * Derive a ReadWriter instance for primitive schemas
   * 
   * For complex types (objects, arrays), use Mirror-based Schema derivation
   * with `case class T derives Schema, ReadWriter` instead.
   */
  def deriveReadWriter[T]: ReadWriter[T] = chez match {
    case s: StringChez => ReadWriterDerivation.deriveStringReadWriter(s).asInstanceOf[ReadWriter[T]]
    case n: NumberChez => ReadWriterDerivation.deriveNumberReadWriter(n).asInstanceOf[ReadWriter[T]]
    case i: IntegerChez => ReadWriterDerivation.deriveIntegerReadWriter(i).asInstanceOf[ReadWriter[T]]
    case b: BooleanChez => ReadWriterDerivation.deriveBooleanReadWriter(b).asInstanceOf[ReadWriter[T]]
    case n: NullChez => ReadWriterDerivation.deriveNullReadWriter(n).asInstanceOf[ReadWriter[T]]
    case _ => throw new UnsupportedOperationException(
      s"ReadWriter derivation for ${chez.getClass.getSimpleName} not supported. " +
      s"Use Mirror-based derivation: case class T derives Schema, ReadWriter"
    )
  }
}