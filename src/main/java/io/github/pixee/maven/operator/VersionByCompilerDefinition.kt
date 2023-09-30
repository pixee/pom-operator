package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.AbstractVersionCommandJ
import io.github.pixee.maven.operator.java.UtilJ.selectXPathNodes
import io.github.pixee.maven.operator.java.VersionDefinitionJ
import org.apache.commons.lang3.text.StrSubstitutor
import org.dom4j.Element
import java.util.*

class VersionByCompilerDefinition : AbstractVersionCommandJ() {
    override fun execute(pm: ProjectModel): Boolean {
        val definedSettings: MutableSet<VersionDefinitionJ> =
            TreeSet<VersionDefinitionJ>(AbstractVersionCommandJ.VERSION_KIND_COMPARATOR)

        val parents = listOf(
            "//m:project/m:build/m:pluginManagement/m:plugins",
            "//m:project/m:build/m:plugins"
        )

        val properties = pm.resolvedProperties

        val sub = StrSubstitutor(properties)

        parents.forEach { parent ->
            pm.allPomFiles.forEach { doc ->
                val pluginExpression =
                    "$parent/m:plugin[./m:artifactId[text()='maven-compiler-plugin']]//m:configuration"
                val compilerNode = selectXPathNodes(doc.resultPom,pluginExpression).firstOrNull()

                if (compilerNode != null) {
                    AbstractVersionCommandJ.TYPE_TO_KIND.entries.mapNotNull {
                        val childElement = (compilerNode as Element).element(it.key)

                        if (childElement != null) {
                            VersionDefinitionJ(it.value, sub.replace(childElement.textTrim))
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