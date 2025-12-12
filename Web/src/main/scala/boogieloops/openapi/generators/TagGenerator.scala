package boogieloops.web.openapi.generators

import boogieloops.web.*
import boogieloops.web.openapi.models.*

/**
 * Generates OpenAPI Tag objects from route schemas
 */
object TagGenerator {

  /**
   * Extract unique tags from all routes and create TagObjects
   */
  def extractUniqueTags(allRoutes: Map[String, RouteSchema]): List[TagObject] = {
    val allTags = allRoutes.values.flatMap(_.tags).toSet

    allTags
      .map { tagName =>
        TagObject(
          name = tagName,
          description = generateTagDescription(tagName, allRoutes),
          externalDocs = None
        )
      }
      .toList
      .sortBy(_.name)
  }

  /**
   * Generate a description for a tag based on routes that use it
   */
  private def generateTagDescription(
      tagName: String,
      allRoutes: Map[String, RouteSchema]
  ): Option[String] = {
    val routesWithTag = allRoutes.filter(_._2.tags.contains(tagName))

    if (routesWithTag.nonEmpty) {
      val routeCount = routesWithTag.size
      val operations = routesWithTag.keys.map(extractMethod).toSet

      Some(s"Operations related to $tagName ($routeCount endpoints: ${operations.mkString(", ")})")
    } else {
      Some(s"Operations related to $tagName")
    }
  }

  private def extractMethod(methodPath: String): String = {
    methodPath.split(":").headOption.getOrElse("GET").toUpperCase
  }
}
