package io.openpixee.maven.operator

import io.openpixee.maven.operator.Util.selectXPathNodes
import org.dom4j.Document
import org.dom4j.Element
import java.net.URL

/**
 * ProjectModel represents the input parameters for the chain
 *
 * @todo consider resolution and also Topological Sort of Properties for cross-property reference
 */
class ProjectModel internal constructor(
    val pomPath: URL?,
    val pomDocument: Document,
    var dependency: Dependency?,
    val skipIfNewer: Boolean,
    val useProperties: Boolean,
    val activeProfiles: Set<String>,
    val overrideIfAlreadyExists: Boolean,
) {
    val resultPom: Document = pomDocument.clone() as Document

    val resolvedProperties: Map<String, String> =
        run {
            val rootProperties =
                pomDocument.rootElement.elements("properties").flatMap { it.elements() }
                    .associate {
                        it.name to it.text
                    }
            val result: MutableMap<String, String> = LinkedHashMap()
            result.putAll(rootProperties)
            val activatedProfiles = activeProfiles.filterNot { it.startsWith("!") }
            activatedProfiles.forEach { profileName ->
                val expression =
                    "/m:project/m:profiles/m:profile[./m:id[text()='${profileName}']]/m:properties"
                val propertiesElements =
                    pomDocument.selectXPathNodes(expression)

                val newPropertiesToAppend =
                    propertiesElements.filterIsInstance<Element>()
                        .flatMap { it.elements() }
                        .associate {
                            it.name to it.text
                        }

                result.putAll(newPropertiesToAppend)
            }
            result.toMap()
        }
}

