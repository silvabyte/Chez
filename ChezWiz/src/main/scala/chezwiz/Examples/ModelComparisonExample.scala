package chezwiz.agent.examples

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.*
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
import chezwiz.agent.*
import chezwiz.agent.providers.{AnthropicProvider, OpenAIProvider}

object ModelComparisonExample extends App:
  println("=== Model Comparison Example ===")

  val prompt = "Explain artificial intelligence in exactly 20 words"

  for
    openaiKey <- sys.env.get("OPENAI_API_KEY")
    anthropicKey <- sys.env.get("ANTHROPIC_API_KEY")
  do
    // OpenAI GPT-4o-mini
    val openaiAgent = Agent(
      name = "OpenAI Agent",
      instructions = "You are precise and concise",
      provider = new OpenAIProvider(openaiKey),
      model = "gpt-4o-mini",
      temperature = Some(0.3)
    )

    // Anthropic Claude
    val anthropicAgent = Agent(
      name = "Anthropic Agent",
      instructions = "You are precise and concise",
      provider = new AnthropicProvider(anthropicKey),
      model = "claude-3-5-haiku-20241022",
      temperature = Some(0.3)
    )

    // Get responses
    val openaiResponse = Await.result(openaiAgent.generateText(prompt), 10.seconds)
    val anthropicResponse = Await.result(anthropicAgent.generateText(prompt), 10.seconds)

    println(s"OpenAI:    ${openaiResponse.content}")
    println(s"Anthropic: ${anthropicResponse.content}")

    // Compare word counts
    val openaiWords = openaiResponse.content.split("\\s+").length
    val anthropicWords = anthropicResponse.content.split("\\s+").length
    println(s"Word counts - OpenAI: $openaiWords, Anthropic: $anthropicWords")
