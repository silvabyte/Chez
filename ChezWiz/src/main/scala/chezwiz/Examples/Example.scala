package chezwiz.agent

/**
 * Legacy example runner maintained for backwards compatibility.
 *
 * For new usage, prefer running individual examples or AllExamples:
 *   - ./mill Agents.runMain agents.examples.AllExamples (runs all examples)
 *   - ./mill Agents.runMain agents.examples.BasicExample (runs specific example)
 */
object Example extends App:
  println("Running all examples via AllExamples...")
  chezwiz.agent.examples.AllExamples.main(args)
