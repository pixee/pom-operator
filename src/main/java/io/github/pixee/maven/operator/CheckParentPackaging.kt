package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.selectXPathNodes
import org.dom4j.Element
import org.dom4j.Text

/**
 * Guard Command Singleton use to validate required parameters
 */
val CheckParentPackaging = object : AbstractSimpleCommand() {
    fun packagingTypePredicate(d: POMDocument, packagingType: String): Boolean {
        //  "/m:project" +
        //                "/m:dependencyManagement" +
        //                "/m:dependencies" +
        //                "/m:dependency" +
        //                /* */ "[./m:groupId[text()='${dependency.groupId}'] and " +
        //                /*  */ "./m:artifactId[text()='${dependency.artifactId}']" +
        //                "]"
        val elementText =
            d.pomDocument.rootElement.selectXPathNodes("/m:project/m:packaging/text()")
                .firstOrNull()

        if (elementText is Text) {
            return elementText.text.equals(packagingType)
        }

        return false
    }

    override fun execute(c: ProjectModel): Boolean {
        val wrongParentPoms = c.parentPomFiles.filterNot { packagingTypePredicate(it, "pom") }

        if (wrongParentPoms.isNotEmpty()) {
            // todo change type
            throw WrongDependencyTypeException("wrong packaging type for parentPom")
        }

        if (c.parentPomFiles.isNotEmpty()) {
            // check main pom file has a inheritance to one of the members listed
            if (!hasValidParentAndPackaging(c.pomFile)) {
                throw WrongDependencyTypeException("invalid parent/packaging combo for main pomfile")
            }
        }

        // todo: test a->b->c

        return false
    }

    private fun hasValidParentAndPackaging(pomFile: POMDocument): Boolean {
        val parentNode = pomFile.pomDocument.rootElement.selectXPathNodes("/m:project/m:parent")
            .firstOrNull() as Element?

        if (parentNode == null) {
            return false
        }

        val packagingText =
            pomFile.pomDocument.rootElement.selectXPathNodes("/m:project/m:packaging/text()")
                .firstOrNull() as Text?

        if (packagingText == null) {
            throw WrongDependencyTypeException("packaging is missing")
        }

        val validPackagingType = packagingText.text.endsWith("ar")

        return validPackagingType

        return true
    }
}