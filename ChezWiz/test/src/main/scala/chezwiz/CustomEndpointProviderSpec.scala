package chezwiz.agent

import chezwiz.agent.providers.{CustomEndpointProvider, HttpVersion}
import upickle.default.*
import utest._

object CustomEndpointProviderSpec extends TestSuite:
  val tests = Tests {
    test("CustomEndpointProvider - create provider for LM Studio") {
      val provider = CustomEndpointProvider.forLMStudio(
        baseUrl = "http://localhost:1234/v1",
        modelId = "test-model"
      )

      assert(provider.name.contains("CustomEndpoint"))
      assert(provider.supportedModels.contains("test-model"))
      assert(provider.validateModel("test-model") == Right(()))
      assert(provider.httpVersion == HttpVersion.Http11) // Default for LM Studio
    }

    test("CustomEndpointProvider - create provider with custom base URL") {
      val customUrl = "http://localhost:8080/v1"
      val provider = CustomEndpointProvider.forLMStudio(
        baseUrl = customUrl,
        modelId = "custom-model",
        httpVersion = HttpVersion.Http2
      )

      assert(provider.name.contains(customUrl))
      assert(provider.supportedModels.contains("custom-model"))
      assert(provider.httpVersion == HttpVersion.Http2)
    }

    test("CustomEndpointProvider - create OpenAI-compatible provider with API key") {
      val provider = CustomEndpointProvider.forOpenAICompatible(
        baseUrl = "https://api.example.com/v1",
        apiKey = "test-key",
        supportedModels = List("model1", "model2")
      )

      assert(provider.supportedModels.contains("model1"))
      assert(provider.supportedModels.contains("model2"))
      assert(provider.validateModel("model1") == Right(()))
      assert(provider.validateModel("model3").isLeft)
      assert(provider.httpVersion == HttpVersion.Http2) // Default for OpenAI-compatible
    }

    test("CustomEndpointProvider - create provider with custom headers") {
      val provider = new CustomEndpointProvider(
        apiKey = "test-api-key",
        baseUrl = "https://api.test.com/v1",
        requiresAuthentication = true,
        customHeaders = Map("X-Custom" -> "value"),
        useJsonSchemaFormat = false
      )

      assert(provider.requiresAuthentication == true)
      assert(provider.customHeaders.contains("X-Custom"))
      assert(provider.customHeaders("X-Custom") == "value")
    }

    test("CustomEndpointProvider - create non-authenticated endpoint") {
      val provider = new CustomEndpointProvider(
        apiKey = "",
        baseUrl = "http://localhost:1234/v1",
        requiresAuthentication = false
      )

      assert(provider.requiresAuthentication == false)
    }

    test("CustomEndpointProvider - use OpenAI format by default") {
      val provider = CustomEndpointProvider.forLMStudio(
        baseUrl = "http://localhost:1234/v1"
      )

      assert(provider.useOpenAIFormat == true)
    }

    test("CustomEndpointProvider - validate models correctly when supportedModels is empty") {
      val provider = new CustomEndpointProvider(
        baseUrl = "http://localhost:1234/v1",
        supportedModels = List.empty
      )

      // When supportedModels is empty, any model should be valid
      assert(provider.validateModel("any-model") == Right(()))
      assert(provider.validateModel("another-model") == Right(()))
    }

    test("CustomEndpointProvider - validate models correctly when supportedModels is specified") {
      val provider = new CustomEndpointProvider(
        baseUrl = "http://localhost:1234/v1",
        supportedModels = List("model1", "model2")
      )

      assert(provider.validateModel("model1") == Right(()))
      assert(provider.validateModel("model2") == Right(()))
      provider.validateModel("model3") match {
        case Left(ChezError.ModelNotSupported("model3", _, _)) => // Expected
        case _ => assert(false)
      }
    }

    test("CustomEndpointProvider companion object - provide convenient factory methods") {
      val lmStudioProvider = CustomEndpointProvider.forLMStudio(
        baseUrl = "http://localhost:1234/v1"
      )
      assert(lmStudioProvider.requiresAuthentication == false)
      assert(lmStudioProvider.useOpenAIFormat == true)
      assert(lmStudioProvider.httpVersion == HttpVersion.Http11)

      val openAICompatProvider = CustomEndpointProvider.forOpenAICompatible(
        baseUrl = "https://api.test.com/v1",
        apiKey = "key",
        supportedModels = List("gpt-4")
      )
      assert(openAICompatProvider.requiresAuthentication == true)
      assert(openAICompatProvider.useOpenAIFormat == true)
      assert(openAICompatProvider.httpVersion == HttpVersion.Http2)
    }

    test("CustomEndpointProvider - allow configuring HTTP version") {
      // LM Studio with HTTP/2
      val lmStudioHttp2 = CustomEndpointProvider.forLMStudio(
        baseUrl = "http://localhost:1234/v1",
        httpVersion = HttpVersion.Http2
      )
      assert(lmStudioHttp2.httpVersion == HttpVersion.Http2)

      // OpenAI-compatible with HTTP/1.1
      val openAIWithHttp11 = CustomEndpointProvider.forOpenAICompatible(
        baseUrl = "https://api.example.com/v1",
        apiKey = "key",
        httpVersion = HttpVersion.Http11
      )
      assert(openAIWithHttp11.httpVersion == HttpVersion.Http11)
    }

    test("CustomEndpointProvider - LM Studio uses json_schema format") {
      val lmStudioProvider = CustomEndpointProvider.forLMStudio(
        baseUrl = "http://localhost:1234/v1"
      )
      assert(lmStudioProvider.useJsonSchemaFormat == true)
    }

    test("CustomEndpointProvider - OpenAI compatible can use json_schema format") {
      val defaultProvider = CustomEndpointProvider.forOpenAICompatible(
        baseUrl = "https://api.example.com/v1",
        apiKey = "key"
      )
      assert(defaultProvider.useJsonSchemaFormat == false)

      val jsonSchemaProvider = CustomEndpointProvider.forOpenAICompatible(
        baseUrl = "https://api.example.com/v1",
        apiKey = "key",
        useJsonSchemaFormat = true
      )
      assert(jsonSchemaProvider.useJsonSchemaFormat == true)
    }
  }
