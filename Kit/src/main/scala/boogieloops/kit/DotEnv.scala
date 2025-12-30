package boogieloops.kit

import scala.util.Try

class DotEnv(private val env: Map[String, String]) {

  def get(key: String): Option[String] = env.get(key).orElse(sys.env.get(key))

  def getOrElse(key: String, default: String): String =
    get(key).getOrElse(default)

  def withSet(key: String, value: String): DotEnv =
    new DotEnv(env + (key -> value))

  def all: Map[String, String] = sys.env.toMap ++ env
}

object DotEnv {

  def load(filePath: String = ".env", overrideExisting: Boolean = true): DotEnv = {
    val lines: Seq[String] = Try(os.read.lines(os.Path(filePath, os.pwd))).getOrElse(Seq.empty)
    val env = lines.map(parseLine).foldLeft(Map.empty[String, String]) {
      case (acc, Some((key, value))) =>
        if (overrideExisting || !acc.contains(key)) {
          acc + (key -> value)
        } else {
          acc
        }
      case (acc, None) => acc
    }
    new DotEnv(env)
  }

  private def parseLine(line: String): Option[(String, String)] = {
    val trimmed = line.trim
    if (trimmed.isEmpty || trimmed.startsWith("#")) {
      None
    } else {
      val parts = trimmed.split("=", 2).map(_.trim)
      if (parts.length == 2) {
        Some(parts(0) -> unquote(parts(1)))
      } else {
        None
      }
    }
  }

  private def unquote(value: String): String = {
    if (value.length >= 2) {
      if (
        (value.startsWith("\"") && value.endsWith("\"")) ||
        (value.startsWith("'") && value.endsWith("'"))
      ) {
        value.substring(1, value.length - 1)
      } else {
        value
      }
    } else {
      value
    }
  }
}
