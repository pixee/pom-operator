package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.AbstractCommandJ
import io.github.pixee.maven.operator.java.ProjectModelJ
import io.github.pixee.maven.operator.java.UtilJ.selectXPathNodes
import io.github.pixee.maven.operator.java.UtilJ
import org.dom4j.Element
import java.lang.IllegalStateException

class CompositeDependencyManagement : AbstractCommandJ() {
    override fun execute(pm: ProjectModelJ): Boolean {
        /**
         * Abort if not multi-pom
         */
        if (pm.parentPomFiles.isEmpty()) {
            return false
        }

        var result = false

        /**
         * TODO: Make it configurable / clear WHERE one should change it
         */
        val parentPomFile = pm.parentPomFiles.last()

        // add dependencyManagement

        val dependencyManagementElement =
            if (parentPomFile.resultPom.rootElement.elements("dependencyManagement").isEmpty()) {
                UtilJ.addIndentedElement(parentPomFile.resultPom.rootElement,
                    parentPomFile,
                    "dependencyManagement"
                )
            } else {
                parentPomFile.resultPom.rootElement.element("dependencyManagement")
            }

        val newDependencyManagementElement = modifyDependency(
            parentPomFile,
            UtilJ.buildLookupExpressionForDependencyManagement(pm.dependency!!),
            pm,
            dependencyManagementElement,
            dependencyManagementNode = true,
        )

        if (pm.useProperties) {
            if (newDependencyManagementElement != null) {
                val newVersionNode = UtilJ.addIndentedElement(newDependencyManagementElement, parentPomFile, "version")

                UtilJ.upgradeVersionNode(pm, newVersionNode, parentPomFile)
            } else {
                throw IllegalStateException("newDependencyManagementElement is missing")
            }
        }

        // add dependency to pom - sans version
        modifyDependency(
            pm.pomFile,
            UtilJ.buildLookupExpressionForDependency(pm.dependency!!),
            pm,
            pm.pomFile.resultPom.rootElement,
            dependencyManagementNode = false,
        )

        if (!result) {
            result = pm.pomFile.dirty
        }

        return result
    }

    private fun modifyDependency(
        pomFileToModify: POMDocument,
        lookupExpressionForDependency: String,
        c: ProjectModelJ,
        parentElement: Element,
        dependencyManagementNode: Boolean,
    ): Element? {
        val dependencyNodes =
            selectXPathNodes(pomFileToModify.resultPom,lookupExpressionForDependency)

        if (1 == dependencyNodes.size) {
            val versionNodes = selectXPathNodes(dependencyNodes[0],"./m:version")

            if (1 == versionNodes.size) {
                val versionNode = versionNodes.first()

                versionNode.parent.content().remove(versionNode)

                pomFileToModify.dirty = true
            }

            return dependencyNodes[0] as Element
        } else {
            val dependenciesNode: Element =
                if (null != parentElement.element("dependencies")) {
                    parentElement.element("dependencies")
                } else {
                    UtilJ.addIndentedElement(parentElement,
                        pomFileToModify,
                        "dependencies"
                    )
                }

            val dependencyNode: Element =
                UtilJ.addIndentedElement(dependenciesNode,pomFileToModify, "dependency")

            UtilJ.addIndentedElement(dependencyNode,pomFileToModify, "groupId").text =
                c.dependency!!.groupId
            UtilJ.addIndentedElement(dependencyNode,pomFileToModify, "artifactId").text =
                c.dependency!!.artifactId

            if (dependencyManagementNode) {
                if (!c.useProperties) {
                    UtilJ.addIndentedElement(dependencyNode,pomFileToModify, "version").text =
                        c.dependency!!.version!!
                }
            }

            pomFileToModify.dirty = true

            return dependencyNode
        }

        return null
    }
}