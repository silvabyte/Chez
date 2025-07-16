package chezwiz.agent.examples

import scala.concurrent.{Await}
import scala.concurrent.duration.*
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
import chezwiz.agent.*
import chezwiz.agent.providers.OpenAIProvider

object AdvancedExample extends App:
  println("=== Advanced Example ===")

  sys.env.get("OPENAI_API_KEY") match
    case Some(key) =>
      val provider = new OpenAIProvider(key)

      val agent = Agent(
        name = "Creative Writer",
        instructions = "You are a creative writer who writes short, engaging stories",
        provider = provider,
        model = "gpt-4o-mini",
        temperature = Some(0.8), // Higher creativity
        maxTokens = Some(200)
      )

      val response = Await.result(
        agent.generateText("Write a short story about a robot discovering emotions"),
        15.seconds
      )

      println(s"Creative Story:\n${response.content}")

      // Show usage statistics
      response.usage.foreach { usage =>
        println(
          s"\nUsage: ${usage.promptTokens} prompt + ${usage.completionTokens} completion = ${usage.totalTokens} total tokens"
        )
      }

    case None =>
      println("OPENAI_API_KEY not found for advanced example")
