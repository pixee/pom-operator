package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.AbstractCommandJ
import io.github.pixee.maven.operator.java.ProjectModelJ
import io.github.pixee.maven.operator.java.UtilJ.selectXPathNodes
import io.github.pixee.maven.operator.java.WrongDependencyTypeExceptionJ
import org.dom4j.Element
import org.dom4j.Text

/**
 * Guard Command Singleton use to validate required parameters
 */
val CheckParentPackaging = object : AbstractCommandJ() {
    fun packagingTypePredicate(d: POMDocument, packagingType: String): Boolean {
        val elementText =
            selectXPathNodes(d.pomDocument.rootElement,"/m:project/m:packaging/text()")
                .firstOrNull()

        if (elementText is Text) {
            return elementText.text.equals(packagingType)
        }

        return false
    }

    override fun execute(pm: ProjectModelJ): Boolean {
        val wrongParentPoms = pm.parentPomFiles.filterNot { packagingTypePredicate(it, "pom") }

        if (wrongParentPoms.isNotEmpty()) {
            throw WrongDependencyTypeExceptionJ("wrong packaging type for parentPom")
        }

        if (pm.parentPomFiles.isNotEmpty()) {
            // check main pom file has a inheritance to one of the members listed
            if (!hasValidParentAndPackaging(pm.pomFile)) {
                throw WrongDependencyTypeExceptionJ("invalid parent/packaging combo for main pomfile")
            }
        }

        // todo: test a->b->c

        return false
    }

    private fun hasValidParentAndPackaging(pomFile: POMDocument): Boolean {
        val parentNode = selectXPathNodes(pomFile.pomDocument.rootElement,"/m:project/m:parent")
            .firstOrNull() as Element? ?: return false

        val packagingText =
            (selectXPathNodes(pomFile.pomDocument.rootElement,"/m:project/m:packaging/text()")
                .firstOrNull() as Text?)?.text ?: "jar"

        @Suppress("UnnecessaryVariable") val validPackagingType = packagingText.endsWith("ar")

        return validPackagingType
    }
}