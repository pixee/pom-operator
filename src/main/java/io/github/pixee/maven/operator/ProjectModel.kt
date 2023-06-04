package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.selectXPathNodes
import org.dom4j.Element

/**
 * ProjectModel represents the input parameters for the chain
 *
 * @todo consider resolution and also Topological Sort of Properties for cross-property reference
 */
class ProjectModel internal constructor(
    val pomFile: POMDocument,

    val parentPomFiles: List<POMDocument> = emptyList(),

    var dependency: Dependency?,
    val skipIfNewer: Boolean,
    val useProperties: Boolean,
    val activeProfiles: Set<String>,
    val overrideIfAlreadyExists: Boolean,
    val queryType: QueryType = QueryType.NONE,
) {
    internal var modifiedByCommand = false

    /**
     * Involved POM Files
     *
     * @todo Currently we only work at two levels - Parent and Child - could be a problem or not
     * if we employ intermediate levels (e.g., organizational pom, project pom, jar pom)
     */
    val allPomFiles: Collection<POMDocument>
        get() = listOfNotNull(
            pomFile,
            *parentPomFiles.toTypedArray()
        )

    val resolvedProperties: Map<String, String> =
        run {
            val result: MutableMap<String, String> = LinkedHashMap()


            allPomFiles
                .reversed() // parent first, children later - thats why its reversed
                .forEach { pomFile ->
                    val rootProperties =
                        pomFile.pomDocument.rootElement.elements("properties")
                            .flatMap { it.elements() }
                            .associate {
                                it.name to it.text
                            }
                    result.putAll(rootProperties)
                    val activatedProfiles = activeProfiles.filterNot { it.startsWith("!") }
                    activatedProfiles.forEach { profileName ->
                        val expression =
                            "/m:project/m:profiles/m:profile[./m:id[text()='${profileName}']]/m:properties"
                        val propertiesElements =
                            pomFile.pomDocument.selectXPathNodes(expression)

                        val newPropertiesToAppend =
                            propertiesElements.filterIsInstance<Element>()
                                .flatMap { it.elements() }
                                .associate {
                                    it.name to it.text
                                }

                        result.putAll(newPropertiesToAppend)
                    }
                }
            result.toMap()
        }
}

