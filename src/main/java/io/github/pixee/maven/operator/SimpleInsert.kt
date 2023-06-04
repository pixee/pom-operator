package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.addIndentedElement
import io.github.pixee.maven.operator.Util.selectXPathNodes
import io.github.pixee.maven.operator.Util.upgradeVersionNode
import org.dom4j.Element

/**
 * Represents a POM Upgrade Strategy by simply adding a dependency/ section (and optionally a dependencyManagement/ section as well)
 */
val SimpleInsert = object : Command {
    override fun execute(c: ProjectModel): Boolean {
        val dependencyManagementNodeList =
            c.pomFile.resultPom.selectXPathNodes("/m:project/m:dependencyManagement")

        val dependenciesNode = if (dependencyManagementNodeList.isEmpty()) {
            val newDependencyManagementNode =
                c.pomFile.resultPom.rootElement.addIndentedElement(
                    c.pomFile,
                    "dependencyManagement"
                )

            val dependencyManagementNode =
                newDependencyManagementNode.addIndentedElement(c.pomFile, "dependencies")

            dependencyManagementNode
        } else {
            (dependencyManagementNodeList.first() as Element).element("dependencies")
        }

        val dependencyNode = appendCoordinates(dependenciesNode, c)

        val versionNode = dependencyNode.addIndentedElement(c.pomFile, "version")

        upgradeVersionNode(c, versionNode)

        val dependenciesNodeList =
            c.pomFile.resultPom.selectXPathNodes("//m:project/m:dependencies")

        val rootDependencyNode: Element = if (dependenciesNodeList.isEmpty()) {
            c.pomFile.resultPom.rootElement.addIndentedElement(c.pomFile, "dependencies")
        } else if (dependenciesNodeList.size == 1) {
            dependenciesNodeList[0] as Element
        } else {
            throw IllegalStateException("More than one dependencies node")
        }

        appendCoordinates(rootDependencyNode, c)

        return true
    }

    /**
     * Creates the XML Elements for a given dependency
     */
    private fun appendCoordinates(
        dependenciesNode: Element,
        c: ProjectModel
    ): Element {
        val dependencyNode = dependenciesNode.addIndentedElement(c.pomFile, "dependency")

        val groupIdNode = dependencyNode.addIndentedElement(c.pomFile, "groupId")

        val dep = c.dependency!!

        groupIdNode.text = dep.groupId

        val artifactIdNode = dependencyNode.addIndentedElement(c.pomFile, "artifactId")

        artifactIdNode.text = dep.artifactId

        return dependencyNode
    }

    override fun postProcess(c: ProjectModel): Boolean = false
}

