package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.selectXPathNodes
import org.dom4j.Element
import java.util.*

class VersionByCompilerDefinition : AbstractVersionCommand() {
    override fun execute(pm: ProjectModel): Boolean {
        val definedSettings: MutableSet<VersionDefinition> =
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
                    TYPE_TO_KIND.entries.mapNotNull {
                        val childElement = (compilerNode as Element).element(it.key)

                        if (childElement != null) {
                            VersionDefinition(it.value, childElement.textTrim)
                        } else {
                            null
                        }
                    }.forEach {
                        definedSettings.add(it)
                    }
                }
            }
        }

        this.result.addAll(definedSettings)

        return definedSettings.isNotEmpty()
    }

}