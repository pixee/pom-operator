package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.selectXPathNodes
import io.github.pixee.maven.operator.java.UtilJ
import org.dom4j.Element
import java.lang.IllegalStateException

class CompositeDependencyManagement : AbstractCommand() {
    override fun execute(pm: ProjectModel): Boolean {
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
            Util.buildLookupExpressionForDependencyManagement(pm.dependency!!),
            pm,
            dependencyManagementElement,
            dependencyManagementNode = true,
        )

        if (pm.useProperties) {
            if (newDependencyManagementElement != null) {
                val newVersionNode = UtilJ.addIndentedElement(newDependencyManagementElement, parentPomFile, "version")

                Util.upgradeVersionNode(pm, newVersionNode, parentPomFile)
            } else {
                throw IllegalStateException("newDependencyManagementElement is missing")
            }
        }

        // add dependency to pom - sans version
        modifyDependency(
            pm.pomFile,
            Util.buildLookupExpressionForDependency(pm.dependency!!),
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
        c: ProjectModel,
        parentElement: Element,
        dependencyManagementNode: Boolean,
    ): Element? {
        val dependencyNodes =
            pomFileToModify.resultPom.selectXPathNodes(lookupExpressionForDependency)

        if (1 == dependencyNodes.size) {
            val versionNodes = dependencyNodes[0].selectXPathNodes("./m:version")

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