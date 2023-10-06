package io.github.pixee.maven.operator;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryByParsingJ extends AbstractQueryCommandJ {

    private final Set<DependencyJ> dependencies = new LinkedHashSet<>();
    private final Set<DependencyJ> dependencyManagement = new TreeSet<>(new Comparator<DependencyJ>() {
        @Override
        public int compare(DependencyJ o1, DependencyJ o2) {
            if (o1 == o2)
                return 0;

            if (o1 == null)
                return 1;

            if (o2 == null)
                return -1;

            return new CompareToBuilder()
                    .append(o1.getGroupId(), o2.getGroupId())
                    .append(o1.getArtifactId(), o2.getArtifactId())
                    .toComparison();
        }
    });

    private final Map<String, String> properties = new LinkedHashMap<>();
    private final StrSubstitutor strSubstitutor = new StrSubstitutor(properties);

    @Override
    public void extractDependencyTree(File outputPath, File pomFilePath, ProjectModelJ c) {
        // Not implemented
    }

    @Override
    public boolean execute(ProjectModelJ pm) {
        List<POMDocumentJ> pomFilesByHierarchy = pm.allPomFiles();
        Collections.reverse(pomFilesByHierarchy);

        for (POMDocumentJ pomDocument : pomFilesByHierarchy) {
            updateProperties(pomDocument);
            updateDependencyManagement(pomDocument);
            updateDependencies(pomDocument);
        }

        this.result = dependencies;
        return true;
    }

    private void updateDependencyManagement(POMDocumentJ pomDocument) {
        Collection<DependencyJ> dependencyManagementDependenciesToAdd = new ArrayList<>();

        Element dependencyManagementElement = pomDocument.getPomDocument()
                .getRootElement()
                .element("dependencyManagement");

        if (dependencyManagementElement != null) {
            List<Element> dependencyElements = dependencyManagementElement
                    .element("dependencies")
                    .elements("dependency");

            for (Element dependencyElement : dependencyElements) {
                String groupId = getElementTextOrNull(dependencyElement, "groupId");
                String artifactId = getElementTextOrNull(dependencyElement, "artifactId");
                String version = Optional.ofNullable(dependencyElement.elementText("version")).orElse("UNKNOWN");
                String classifier = getElementTextOrNull(dependencyElement, "classifier");
                String packaging = getElementTextOrNull(dependencyElement, "packaging");

                try {
                    version = strSubstitutor.replace(version);
                } catch (IllegalStateException e) {
                    logger.warn("while interpolating version", e);
                    version = "UNKNOWN";
                }

                DependencyJ dependency = new DependencyJ(groupId, artifactId, version, classifier, packaging, null);
                dependencyManagementDependenciesToAdd.add(dependency);
            }
        }

        this.dependencyManagement.addAll(dependencyManagementDependenciesToAdd);
    }

    private DependencyJ lookForDependencyManagement(String groupId, String artifactId) {
        for (DependencyJ dependency : dependencyManagement) {
            if (Objects.equals(dependency.getGroupId(), groupId) && Objects.equals(dependency.getArtifactId(), artifactId)) {
                return dependency;
            }
        }
        return null;
    }

    private void updateDependencies(POMDocumentJ pomDocument) {
        Collection<DependencyJ> dependenciesToAdd = new ArrayList<>();

        Element dependenciesElement = pomDocument.getPomDocument()
                .getRootElement()
                .element("dependencies");

        if (dependenciesElement != null) {
            List<Element> dependencyElements = dependenciesElement.elements("dependency");

            for (Element dependencyElement : dependencyElements) {
                String groupId = getElementTextOrNull(dependencyElement, "groupId");
                String artifactId = getElementTextOrNull(dependencyElement, "artifactId");
                String version = Optional.ofNullable(dependencyElement.elementText("version")).orElse("UNKNOWN");

                DependencyJ proposedDependency = lookForDependencyManagement(groupId, artifactId);

                if (proposedDependency != null) {
                    dependenciesToAdd.add(proposedDependency);
                } else {
                    String classifier = getElementTextOrNull(dependencyElement, "classifier");
                    String packaging = getElementTextOrNull(dependencyElement, "packaging");

                    try {
                        version = strSubstitutor.replace(version);
                    } catch (IllegalStateException e) {
                        logger.warn("while interpolating version", e);
                        version = "UNKNOWN";
                    }

                    DependencyJ dependency = new DependencyJ(groupId, artifactId, version, classifier, packaging, null);
                    dependenciesToAdd.add(dependency);
                }
            }
        }

        this.dependencies.addAll(dependenciesToAdd);
    }


    private String getElementTextOrNull(Element parent, String elementName) {
        Element child = parent.element(elementName);
        return child != null ? child.getText() : null;
    }
    private void updateProperties(POMDocumentJ pomDocument) {
        Map<String, String> propsDefined = ProjectModelJ.propertiesDefinedOnPomDocument(pomDocument);

        for (Map.Entry<String, String> entry : propsDefined.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (!value.matches(RE_INTERPOLATION.pattern())) {
                properties.put(key, value);
            }

            if (!value.matches(RE_INTERPOLATION.pattern())) {
                String newValue;
                try {
                    Matcher matcher = RE_INTERPOLATION.matcher(value);
                    StringBuffer resultBuffer = new StringBuffer();
                    while (matcher.find()) {
                        String variable = matcher.group();
                        String replacement = strSubstitutor.replace(variable);
                        matcher.appendReplacement(resultBuffer, Matcher.quoteReplacement(replacement));
                    }
                    matcher.appendTail(resultBuffer);
                    newValue = resultBuffer.toString();
                } catch (IllegalStateException e) {
                    LOGGER.warn("while replacing variables: ", e);
                    newValue = value;
                }

                properties.put(key, newValue);
            }
        }
    }

    private static final Pattern RE_INTERPOLATION = Pattern.compile(".*\\$\\{[\\p{Alnum}.\\-_]+\\}.*");
    private static final Logger logger = LoggerFactory.getLogger(QueryByParsingJ.class);
}
