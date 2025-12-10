package boogieloops.ai.examples

import upickle.default.ReadWriter
import boogieloops.ai.*
import boogieloops.ai.providers.{OpenAIProvider, AnthropicProvider}
import boogieloops.schema.derivation.Schema

import java.util.concurrent.atomic.AtomicLong

object Examples extends App {
  Config.initialize(sys.env.getOrElse("ENV_DIR", os.pwd.toString))

  val metadata = RequestMetadata(
    tenantId = Some("demo"),
    userId = Some("user-1"),
    conversationId = Some("example")
  )

// Example 1: Basic text generation
  def basic(): Unit = {
    println("=== Basic Text Generation ===")

    val agent = {
      Agent(
        "Assistant",
        "Give concise answers",
        new OpenAIProvider(Config.OPENAI_API_KEY),
        "gpt-4o-mini"
      )
    }
    agent.generateText("What is 2 + 2?", metadata) match {
      case Right(response) => println(s"Answer: ${response.content}")
      case Left(error) => println(s"Error: $error")
    }
  }

// Example 2: Structured data (builds on basic text)
  def structured(): Unit = {
    println("\n=== Structured Data Generation ===")

    case class Task(
        @Schema.description("Task description") task: String,
        @Schema.description("Priority level") priority: String,
        @Schema.description("Time in minutes") @Schema.minimum(1) minutes: Int
    ) derives Schema, ReadWriter

    val agent = {
      Agent(
        "Planner",
        "Create structured task data",
        new OpenAIProvider(Config.OPENAI_API_KEY),
        "gpt-4o"
      )
    }
    agent.generateObject[Task](
      "Create a task for reviewing code with high priority",
      metadata
    ) match {
      case Right(response) =>
        val task = response.data
        println(s"Task: ${task.task} (${task.priority}, ${task.minutes}min)")
      case Left(error) => println(s"Error: $error")
    }
  }

// Example 3: Conversation memory (builds on previous concepts)
  def conversation(): Unit = {
    println("\n=== Conversation with Memory ===")

    val agent = {
      Agent(
        "Bot",
        "Remember context across messages",
        new OpenAIProvider(Config.OPENAI_API_KEY),
        "gpt-4o-mini"
      )
    }

    val chatMetadata = RequestMetadata(
      tenantId = Some("demo"),
      userId = Some("alice"),
      conversationId = Some("chat-1")
    )

    // First message
    agent.generateText("Hi, I'm Alice learning Scala", chatMetadata) match {
      case Right(r1) =>
        println(s"Alice: Hi, I'm Alice learning Scala")
        println(s"Bot: ${r1.content.take(100)}...")

        // Second message - bot should remember Alice's name
        agent.generateText("What should I learn first?", chatMetadata) match {
          case Right(r2) => println(s"Bot: ${r2.content.take(100)}...")
          case Left(error) => println(s"Error: $error")
        }
      case Left(error) => println(s"Error: $error")
    }
  }

// Example 4: Multi-provider support (builds on all previous)
  def multiProvider(): Unit = {
    println("\n=== Multiple AI Providers ===")

    val question = "Explain recursion in one sentence"

    // Try OpenAI
    val openai = {
      Agent(
        "OpenAI",
        "Brief technical answers",
        new OpenAIProvider(Config.OPENAI_API_KEY),
        "gpt-4o-mini"
      )
    }
    openai.generateText(question, metadata) match {
      case Right(response) => println(s"OpenAI: ${response.content}")
      case Left(error) => println(s"OpenAI Error: $error")
    }

    // Try Anthropic
    val claude = Agent(
      "Claude",
      "Brief technical answers",
      new AnthropicProvider(Config.ANTHROPIC_API_KEY),
      "claude-3-5-haiku-20241022"
    )
    claude.generateText(question, metadata) match {
      case Right(response) => println(s"Claude: ${response.content}")
      case Left(error) => println(s"Claude Error: $error")
    }
  }

// Example 5: Monitoring with hooks (builds on everything above)
  def monitoring(): Unit = {
    println("\n=== Agent Monitoring & Hooks ===")

    // Simple inline metrics hook
    class MetricsHook extends PreRequestHook with PostResponseHook with ErrorHook {
      private val requests = new AtomicLong(0)
      private val successes = new AtomicLong(0)
      private val errors = new AtomicLong(0)
      private val totalTime = new AtomicLong(0)

      override def onPreRequest(ctx: PreRequestContext) = requests.incrementAndGet()
      override def onPostResponse(ctx: PostResponseContext) = ctx.response match {
        case Right(_) => successes.incrementAndGet(); totalTime.addAndGet(ctx.duration)
        case Left(_) => errors.incrementAndGet()
      }
      override def onError(ctx: ErrorContext) = errors.incrementAndGet()

      def stats: String = {
        s"Requests: ${requests.get()}, Success: ${successes.get()}, Errors: ${errors.get()}, Avg: ${
            if (successes.get() > 0) totalTime.get() / successes.get() else 0
          }ms"
      }
    }

    // Simple logging hook
    class LogHook extends PreRequestHook with PostResponseHook {
      override def onPreRequest(ctx: PreRequestContext) =
        println(s"[${ctx.agentName}] Starting request...")
      override def onPostResponse(ctx: PostResponseContext) =
        println(s"[${ctx.agentName}] Completed in ${ctx.duration}ms")
    }

    val metrics = new MetricsHook()
    val logging = new LogHook()

    val hooks = HookRegistry.empty
      .addPreRequestHook(metrics)
      .addPostResponseHook(metrics)
      .addErrorHook(metrics)
      .addPreRequestHook(logging)
      .addPostResponseHook(logging)

    val provider = new OpenAIProvider(Config.OPENAI_API_KEY)
    val agent = Agent(
      "Monitored",
      "You are monitored",
      provider,
      "gpt-4o-mini",
      hooks = hooks
    )
    println("✅ Agent with monitoring created")

    // Run a few operations to generate metrics
    List(
      "Hello, how are you?",
      "What's the weather like?",
      "Tell me a joke"
    ).foreach { msg =>
      agent.generateText(msg, metadata) match {
        case Right(response) => println(s"Response: ${response.content.take(50)}...")
        case Left(error) => println(s"Error: $error")
      }
    }

    println(s"\n📊 Final Metrics: ${metrics.stats}")
  }

// Example 6: Built-in metrics system (most advanced)
  def builtInMetrics(): Unit = {
    println("\n=== Built-in Metrics System ===")

    // Manually wire metrics using hooks
    val metrics = new DefaultAgentMetrics()
    val metricsHook = new MetricsHook(metrics)
    val hooks = HookRegistry.empty
      .addPreRequestHook(metricsHook)
      .addPostResponseHook(metricsHook)
      .addPreObjectRequestHook(metricsHook)
      .addPostObjectResponseHook(metricsHook)
      .addErrorHook(metricsHook)
      .addHistoryHook(metricsHook)

    val agent = Agent(
      name = "Production",
      instructions = "Production agent with metrics",
      provider = new OpenAIProvider(Config.OPENAI_API_KEY),
      model = "gpt-4o-mini",
      hooks = hooks
    )
    println("✅ Agent with built-in metrics created")

    // Run operations with different metadata scopes
    val users = List("alice", "bob", "charlie")
    users.foreach { user =>
      val userMetadata = RequestMetadata(
        tenantId = Some("company"),
        userId = Some(user),
        conversationId = Some(s"session-$user")
      )

      agent.generateText(s"Hello from $user", userMetadata)
    }

    // Print comprehensive metrics
    metrics.getSnapshot("Production").foreach { snapshot =>
      println(s"\n📈 Production Metrics:")
      println(snapshot.summary)
    }
  }

  // Run examples in order (each builds on previous)
  basic()
  structured()
  conversation()
  multiProvider()
  monitoring()
  builtInMetrics()

  println("\n=== Examples Complete ===")
   println("💡 Each example builds on the previous, showing progressive boogieloops.ai capabilities")
  println("🔧 Set OPENAI_API_KEY and/or ANTHROPIC_API_KEY to see real results")
}
