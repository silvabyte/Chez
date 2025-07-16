package chezwiz.agent.examples

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.*
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Main runner that executes all example applications.
 *
 * Usage: ./mill ChezWiz.runMain chezwiz.examples.AllExamples
 *
 * Or run individual examples:
 *   - ./mill ChezWiz.runMain chezwiz.agent.examples.BasicExample
 *   - ./mill ChezWiz.runMain chezwiz.agent.examples.AdvancedExample
 *   - ./mill ChezWiz.runMain chezwiz.agent.examples.ConversationExample
 *   - ./mill ChezWiz.runMain chezwiz.agent.examples.ErrorHandlingExample
 *   - ./mill ChezWiz.runMain chezwiz.agent.examples.ModelComparisonExample
 *   - ./mill ChezWiz.runMain chezwiz.agent.examples.StructuredResponseExample
 *   - ./mill ChezWiz.runMain chezwiz.agent.examples.EntityExtractionExample
 */
object AllExamples extends App:

  println("Running all ChezWiz examples...")
  println("=" * 60)

  import scala.util.{Try, Success, Failure}

  Try {
    // Run all examples in sequence by calling their main methods
    println("Running BasicExample...")
    chezwiz.agent.examples.BasicExample.main(Array.empty[String])
    Thread.sleep(1000) // Brief pause between examples

    println("Running AdvancedExample...")
    // Comment out examples that may not exist yet
    AdvancedExample.main(Array.empty[String])
    Thread.sleep(1000)

    ConversationExample.main(Array.empty[String])
    Thread.sleep(1000)

    ErrorHandlingExample.main(Array.empty[String])
    Thread.sleep(1000)

    ModelComparisonExample.main(Array.empty[String])
    Thread.sleep(1000)

    StructuredResponseExample.main(Array.empty[String])
    Thread.sleep(1000)

    // EntityExtractionExample.main(Array.empty[String])
  } match {
    case Success(_) =>
      println("\n" + "=" * 60)
      println("All examples completed successfully!")
    case Failure(ex) =>
      println(s"Example execution failed: ${ex.getMessage}")
      ex.printStackTrace()
  }
