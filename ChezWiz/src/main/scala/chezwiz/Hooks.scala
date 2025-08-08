package chezwiz.agent

import upickle.default.*
import chez.derivation.Schema

/**
 * Hook system for ChezWiz agents providing lifecycle events for observability,
 * logging, and custom business logic.
 */

// ============================================================================
// Hook Context Data Classes
// ============================================================================

/** Context passed to PreRequestHook */
case class PreRequestContext(
    agentName: String,
    model: String,
    request: ChatRequest,
    metadata: RequestMetadata,
    timestamp: Long = System.currentTimeMillis()
)

/** Context passed to PostResponseHook */
case class PostResponseContext(
    agentName: String,
    model: String,
    request: ChatRequest,
    response: Either[ChezError, ChatResponse],
    metadata: RequestMetadata,
    requestTimestamp: Long,
    responseTimestamp: Long = System.currentTimeMillis()
) {
  def duration: Long = responseTimestamp - requestTimestamp
}

/** Context passed to PreObjectRequestHook */
case class PreObjectRequestContext(
    agentName: String,
    model: String,
    request: ObjectRequest,
    metadata: RequestMetadata,
    targetType: String,
    timestamp: Long = System.currentTimeMillis()
)

/** Context passed to PostObjectResponseHook */
case class PostObjectResponseContext(
    agentName: String,
    model: String,
    request: ObjectRequest,
    response: Either[ChezError, ObjectResponse[ujson.Value]],
    metadata: RequestMetadata,
    targetType: String,
    requestTimestamp: Long,
    responseTimestamp: Long = System.currentTimeMillis()
) {
  def duration: Long = responseTimestamp - requestTimestamp
}

/** Context passed to ErrorHook */
case class ErrorContext(
    agentName: String,
    model: String,
    error: ChezError,
    metadata: RequestMetadata,
    operation: String, // "generateText", "generateObject", etc.
    request: Option[Either[ChatRequest, ObjectRequest]] = None,
    timestamp: Long = System.currentTimeMillis()
)

/** History operation types */
enum HistoryOperation {
  case Add, Clear, ClearAll
}

/** Context passed to HistoryHook */
case class HistoryContext(
    agentName: String,
    operation: HistoryOperation,
    metadata: RequestMetadata,
    message: Option[ChatMessage] = None, // For Add operations
    historySize: Int,
    timestamp: Long = System.currentTimeMillis()
)

/** Context passed to ScopeChangeHook */
case class ScopeChangeContext(
    agentName: String,
    previousMetadata: Option[RequestMetadata],
    newMetadata: RequestMetadata,
    timestamp: Long = System.currentTimeMillis()
)

/** Context passed to PreEmbeddingHook */
case class PreEmbeddingContext(
    agentName: String,
    model: String,
    request: EmbeddingRequest,
    metadata: RequestMetadata,
    timestamp: Long = System.currentTimeMillis()
)

/** Context passed to PostEmbeddingHook */
case class PostEmbeddingContext(
    agentName: String,
    model: String,
    request: EmbeddingRequest,
    response: Either[ChezError, EmbeddingResponse],
    metadata: RequestMetadata,
    requestTimestamp: Long,
    responseTimestamp: Long = System.currentTimeMillis()
) {
  def duration: Long = responseTimestamp - requestTimestamp
}

// ============================================================================
// Hook Trait Interfaces
// ============================================================================

/** Base trait for all agent hooks */
trait AgentHook

/** Hook executed before sending requests to LLM providers */
trait PreRequestHook extends AgentHook {
  def onPreRequest(context: PreRequestContext): Unit
}

/** Hook executed after receiving responses from LLM providers */
trait PostResponseHook extends AgentHook {
  def onPostResponse(context: PostResponseContext): Unit
}

/** Hook executed before sending object generation requests to LLM providers */
trait PreObjectRequestHook extends AgentHook {
  def onPreObjectRequest(context: PreObjectRequestContext): Unit
}

/** Hook executed after receiving object generation responses from LLM providers */
trait PostObjectResponseHook extends AgentHook {
  def onPostObjectResponse(context: PostObjectResponseContext): Unit
}

/** Hook executed when errors occur */
trait ErrorHook extends AgentHook {
  def onError(context: ErrorContext): Unit
}

/** Hook executed when conversation history changes */
trait HistoryHook extends AgentHook {
  def onHistoryChange(context: HistoryContext): Unit
}

/** Hook executed when conversation scope changes */
trait ScopeChangeHook extends AgentHook {
  def onScopeChange(context: ScopeChangeContext): Unit
}

/** Hook executed before sending embedding requests to LLM providers */
trait PreEmbeddingHook extends AgentHook {
  def onPreEmbedding(context: PreEmbeddingContext): Unit
}

/** Hook executed after receiving embedding responses from LLM providers */
trait PostEmbeddingHook extends AgentHook {
  def onPostEmbedding(context: PostEmbeddingContext): Unit
}

// ============================================================================
// Hook Registry
// ============================================================================

/** Registry for managing and executing hooks */
class HookRegistry {
  private var preRequestHooks: List[PreRequestHook] = List.empty
  private var postResponseHooks: List[PostResponseHook] = List.empty
  private var preObjectRequestHooks: List[PreObjectRequestHook] = List.empty
  private var postObjectResponseHooks: List[PostObjectResponseHook] = List.empty
  private var errorHooks: List[ErrorHook] = List.empty
  private var historyHooks: List[HistoryHook] = List.empty
  private var scopeChangeHooks: List[ScopeChangeHook] = List.empty
  private var preEmbeddingHooks: List[PreEmbeddingHook] = List.empty
  private var postEmbeddingHooks: List[PostEmbeddingHook] = List.empty

  // Registration methods
  def addPreRequestHook(hook: PreRequestHook): HookRegistry = {
    preRequestHooks = preRequestHooks :+ hook
    this
  }

  def addPostResponseHook(hook: PostResponseHook): HookRegistry = {
    postResponseHooks = postResponseHooks :+ hook
    this
  }

  def addPreObjectRequestHook(hook: PreObjectRequestHook): HookRegistry = {
    preObjectRequestHooks = preObjectRequestHooks :+ hook
    this
  }

  def addPostObjectResponseHook(hook: PostObjectResponseHook): HookRegistry = {
    postObjectResponseHooks = postObjectResponseHooks :+ hook
    this
  }

  def addErrorHook(hook: ErrorHook): HookRegistry = {
    errorHooks = errorHooks :+ hook
    this
  }

  def addHistoryHook(hook: HistoryHook): HookRegistry = {
    historyHooks = historyHooks :+ hook
    this
  }

  def addScopeChangeHook(hook: ScopeChangeHook): HookRegistry = {
    scopeChangeHooks = scopeChangeHooks :+ hook
    this
  }

  def addPreEmbeddingHook(hook: PreEmbeddingHook): HookRegistry = {
    preEmbeddingHooks = preEmbeddingHooks :+ hook
    this
  }

  def addPostEmbeddingHook(hook: PostEmbeddingHook): HookRegistry = {
    postEmbeddingHooks = postEmbeddingHooks :+ hook
    this
  }

  // Execution methods (safe - catch exceptions to prevent hook failures from breaking agent)
  def executePreRequestHooks(context: PreRequestContext): Unit = {
    preRequestHooks.foreach { hook =>
      try {
        hook.onPreRequest(context)
      } catch {
        case ex: Exception =>
          // Log hook failure but don't propagate (hooks should not break agent functionality)
          System.err.println(s"PreRequestHook failed: ${ex.getMessage}")
      }
    }
  }

  def executePostResponseHooks(context: PostResponseContext): Unit = {
    postResponseHooks.foreach { hook =>
      try {
        hook.onPostResponse(context)
      } catch {
        case ex: Exception =>
          System.err.println(s"PostResponseHook failed: ${ex.getMessage}")
      }
    }
  }

  def executePreObjectRequestHooks(context: PreObjectRequestContext): Unit = {
    preObjectRequestHooks.foreach { hook =>
      try {
        hook.onPreObjectRequest(context)
      } catch {
        case ex: Exception =>
          System.err.println(s"PreObjectRequestHook failed: ${ex.getMessage}")
      }
    }
  }

  def executePostObjectResponseHooks(context: PostObjectResponseContext): Unit = {
    postObjectResponseHooks.foreach { hook =>
      try {
        hook.onPostObjectResponse(context)
      } catch {
        case ex: Exception =>
          System.err.println(s"PostObjectResponseHook failed: ${ex.getMessage}")
      }
    }
  }

  def executeErrorHooks(context: ErrorContext): Unit = {
    errorHooks.foreach { hook =>
      try {
        hook.onError(context)
      } catch {
        case ex: Exception =>
          System.err.println(s"ErrorHook failed: ${ex.getMessage}")
      }
    }
  }

  def executeHistoryHooks(context: HistoryContext): Unit = {
    historyHooks.foreach { hook =>
      try {
        hook.onHistoryChange(context)
      } catch {
        case ex: Exception =>
          System.err.println(s"HistoryHook failed: ${ex.getMessage}")
      }
    }
  }

  def executeScopeChangeHooks(context: ScopeChangeContext): Unit = {
    scopeChangeHooks.foreach { hook =>
      try {
        hook.onScopeChange(context)
      } catch {
        case ex: Exception =>
          System.err.println(s"ScopeChangeHook failed: ${ex.getMessage}")
      }
    }
  }

  def executePreEmbeddingHooks(context: PreEmbeddingContext): Unit = {
    preEmbeddingHooks.foreach { hook =>
      try {
        hook.onPreEmbedding(context)
      } catch {
        case ex: Exception =>
          System.err.println(s"PreEmbeddingHook failed: ${ex.getMessage}")
      }
    }
  }

  def executePostEmbeddingHooks(context: PostEmbeddingContext): Unit = {
    postEmbeddingHooks.foreach { hook =>
      try {
        hook.onPostEmbedding(context)
      } catch {
        case ex: Exception =>
          System.err.println(s"PostEmbeddingHook failed: ${ex.getMessage}")
      }
    }
  }

  // Utility methods
  def hasAnyHooks: Boolean = {
    preRequestHooks.nonEmpty ||
    postResponseHooks.nonEmpty ||
    preObjectRequestHooks.nonEmpty ||
    postObjectResponseHooks.nonEmpty ||
    errorHooks.nonEmpty ||
    historyHooks.nonEmpty ||
    scopeChangeHooks.nonEmpty ||
    preEmbeddingHooks.nonEmpty ||
    postEmbeddingHooks.nonEmpty
  }

  def clear(): Unit = {
    preRequestHooks = List.empty
    postResponseHooks = List.empty
    preObjectRequestHooks = List.empty
    postObjectResponseHooks = List.empty
    errorHooks = List.empty
    historyHooks = List.empty
    scopeChangeHooks = List.empty
    preEmbeddingHooks = List.empty
    postEmbeddingHooks = List.empty
  }
}

object HookRegistry {
  def empty: HookRegistry = new HookRegistry()
}
