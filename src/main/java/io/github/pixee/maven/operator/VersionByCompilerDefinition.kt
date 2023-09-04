package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.selectXPathNodes
import org.dom4j.Element
import java.util.*

class VersionByCompilerDefinition : AbstractVersionCommand() {
    override fun execute(pm: ProjectModel): Boolean {
        val definedProperties: MutableSet<VersionDefinition> =
            TreeSet<VersionDefinition>(VERSION_KIND_COMPARATOR)

        val parents = listOf(
            "//m:project/m:build/m:pluginManagement/m:plugins",
            "//m:project/m:build/m:plugins"
        )

        parents.forEach { parent ->
            pm.allPomFiles.forEach { doc ->
                val pluginExpression =
                    "$parent/m:plugin[./m:artifactId[text()='maven-compiler-plugin']]"
                val compilerNode = doc.resultPom.selectXPathNodes(pluginExpression).firstOrNull()

                if (compilerNode != null) {
                    CHILD_TO_VERSION.entries.mapNotNull {
                        val childElement = (compilerNode as Element).element(it.key)

                        if (childElement != null) {
                            VersionDefinition(it.value, childElement.textTrim)
                        } else {
                            null
                        }
                    }.forEach {
                        definedProperties.add(it)
                    }
                }
            }
        }

        this.result.addAll(definedProperties)

        return definedProperties.isNotEmpty()
    }

    companion object {
        private val CHILD_TO_VERSION = mapOf(
            "source" to Kind.SOURCE,
            "target" to Kind.TARGET,
            "release" to Kind.RELEASE,
        )
    }
}