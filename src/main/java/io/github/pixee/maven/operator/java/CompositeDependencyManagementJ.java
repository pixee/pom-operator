package io.github.pixee.maven.operator.java;

import io.github.pixee.maven.operator.POMDocument;
import io.github.pixee.maven.operator.java.AbstractCommandJ;
import io.github.pixee.maven.operator.java.ProjectModelJ;
import io.github.pixee.maven.operator.java.UtilJ;

import org.dom4j.Element;
import org.dom4j.Node;

import java.util.List;

public class CompositeDependencyManagementJ extends AbstractCommandJ{

    @Override
    public boolean execute(ProjectModelJ pm) {
        // Abort if not multi-pom
        if (pm.getParentPomFiles().isEmpty()) {
            return false;
        }

        boolean result = false;

        // TODO: Make it configurable / clear WHERE one should change it
        POMDocument parentPomFile = pm.getParentPomFiles().get(pm.getParentPomFiles().size() - 1);

        // add dependencyManagement
        Element dependencyManagementElement;
        if (parentPomFile.getResultPom().getRootElement().elements("dependencyManagement").isEmpty()) {
            dependencyManagementElement = UtilJ.addIndentedElement(parentPomFile.getResultPom().getRootElement(),
                    parentPomFile,
                    "dependencyManagement"
            );
        } else {
            dependencyManagementElement = parentPomFile.getResultPom().getRootElement().element("dependencyManagement");
        }

        Element newDependencyManagementElement = modifyDependency(
                parentPomFile,
                UtilJ.buildLookupExpressionForDependencyManagement(pm.getDependency()),
                pm,
                dependencyManagementElement,
                true
        );

        if (pm.getUseProperties()) {
            if (newDependencyManagementElement != null) {
                Element newVersionNode = UtilJ.addIndentedElement(newDependencyManagementElement, parentPomFile, "version");
                UtilJ.upgradeVersionNode(pm, newVersionNode, parentPomFile);
            } else {
                throw new IllegalStateException("newDependencyManagementElement is missing");
            }
        }

        // add dependency to pom - sans version
        modifyDependency(
                pm.getPomFile(),
                UtilJ.buildLookupExpressionForDependency(pm.getDependency()),
                pm,
                pm.getPomFile().getResultPom().getRootElement(),
                false
        );

        if (!result) {
            result = pm.getPomFile().getDirty();
        }

        return result;
    }

    private Element modifyDependency(POMDocument pomFileToModify, String lookupExpressionForDependency, ProjectModelJ c, Element parentElement, boolean dependencyManagementNode) {
        List<Node> dependencyNodes = UtilJ.selectXPathNodes(pomFileToModify.getResultPom(), lookupExpressionForDependency);

        if (dependencyNodes.size() == 1) {
            List<Node> versionNodes = UtilJ.selectXPathNodes(dependencyNodes.get(0), "./m:version");

            if (versionNodes.size() == 1) {
                Element versionNode = (Element) versionNodes.get(0);
                versionNode.getParent().content().remove(versionNode);
                pomFileToModify.setDirty(true);
            }

            return (Element) dependencyNodes.get(0);
        } else {
            Element dependenciesNode;
            if (parentElement.element("dependencies") != null) {
                dependenciesNode = parentElement.element("dependencies");
            } else {
                dependenciesNode = UtilJ.addIndentedElement(parentElement, pomFileToModify, "dependencies");
            }

            Element dependencyNode = UtilJ.addIndentedElement(dependenciesNode, pomFileToModify, "dependency");
            UtilJ.addIndentedElement(dependencyNode, pomFileToModify, "groupId").setText(c.getDependency().getGroupId());
            UtilJ.addIndentedElement(dependencyNode, pomFileToModify, "artifactId").setText(c.getDependency().getArtifactId());

            if (dependencyManagementNode) {
                if (!c.getUseProperties()) {
                    UtilJ.addIndentedElement(dependencyNode, pomFileToModify, "version").setText(c.getDependency().getVersion());
                }
            }

            pomFileToModify.setDirty(true);

            return dependencyNode;
        }
    }

}
