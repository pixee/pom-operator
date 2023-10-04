package io.github.pixee.maven.operator.java;

import org.dom4j.Element;
import org.dom4j.Node;

import java.util.List;

/**
 * Represents a POM Upgrade Strategy by simply adding a dependency/ section (and optionally a dependencyManagement/ section as well)
 */
public class SimpleInsertJ implements CommandJ {
    @Override
    public boolean execute(ProjectModelJ pm) {
        List<Node> dependencyManagementNodeList = UtilJ.selectXPathNodes(pm.getPomFile().getResultPom(), "/m:project/m:dependencyManagement");

        Element dependenciesNode;
        if (dependencyManagementNodeList.isEmpty()) {
            Element newDependencyManagementNode = UtilJ.addIndentedElement(pm.getPomFile().getResultPom().getRootElement(), pm.getPomFile(), "dependencyManagement");

            dependenciesNode = UtilJ.addIndentedElement(newDependencyManagementNode, pm.getPomFile(), "dependencies");
        } else {
            dependenciesNode = ((Element) dependencyManagementNodeList.get(0)).element("dependencies");
        }

        Element dependencyNode = appendCoordinates(dependenciesNode, pm);

        Element versionNode = UtilJ.addIndentedElement(dependencyNode, pm.getPomFile(), "version");

        UtilJ.upgradeVersionNode(pm, versionNode, pm.getPomFile());

        List<Node> dependenciesNodeList = UtilJ.selectXPathNodes(pm.getPomFile().getResultPom(), "//m:project/m:dependencies");

        Element rootDependencyNode;
        if (dependenciesNodeList.isEmpty()) {
            rootDependencyNode = UtilJ.addIndentedElement(pm.getPomFile().getResultPom().getRootElement(), pm.getPomFile(), "dependencies");
        } else if (dependenciesNodeList.size() == 1) {
            rootDependencyNode = (Element) dependenciesNodeList.get(0);
        } else {
            throw new IllegalStateException("More than one dependencies node");
        }

        appendCoordinates(rootDependencyNode, pm);

        return true;
    }

    /**
     * Creates the XML Elements for a given dependency
     */
    private Element appendCoordinates(Element dependenciesNode, ProjectModelJ c) {
        Element dependencyNode = UtilJ.addIndentedElement(dependenciesNode, c.getPomFile(), "dependency");

        Element groupIdNode = UtilJ.addIndentedElement(dependencyNode, c.getPomFile(), "groupId");

        DependencyJ dep = c.getDependency();
        if (dep != null) {
            groupIdNode.setText(dep.getGroupId());
        }

        Element artifactIdNode = UtilJ.addIndentedElement(dependencyNode, c.getPomFile(), "artifactId");

        if (dep != null) {
            artifactIdNode.setText(dep.getArtifactId());
        }

        return dependencyNode;
    }

    @Override
    public boolean postProcess(ProjectModelJ c) {
        return false;
    }
}

