package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.java.AbstractVersionCommandJ;
import io.github.pixee.maven.operator.java.VersionDefinitionJ;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.dom4j.Element;
import org.dom4j.Node;
import java.util.*;

import static io.github.pixee.maven.operator.java.UtilJ.selectXPathNodes;

public class VersionByCompilerDefinitionJ extends AbstractVersionCommandJ {
    @Override
    public boolean execute(ProjectModel pm) {
        Set<VersionDefinitionJ> definedSettings = new TreeSet<>(AbstractVersionCommandJ.VERSION_KIND_COMPARATOR);

        List<String> parents = Arrays.asList(
                "//m:project/m:build/m:pluginManagement/m:plugins",
                "//m:project/m:build/m:plugins"
        );

        Map<String, String> properties = pm.getResolvedProperties();

        StrSubstitutor sub = new StrSubstitutor(properties);

        for (String parent : parents) {
            for (POMDocument doc : pm.getAllPomFiles()) {
                String pluginExpression = parent + "/m:plugin[./m:artifactId[text()='maven-compiler-plugin']]" +
                        "//m:configuration";
                List<Node> compilerNodes = selectXPathNodes(doc.getResultPom(), pluginExpression);

                if (!compilerNodes.isEmpty()) {
                    for (Map.Entry<String, Kind> entry : AbstractVersionCommandJ.TYPE_TO_KIND.entrySet()) {
                        String key = entry.getKey();
                        Kind value = entry.getValue();

                        for (Node compilerNode : compilerNodes) {
                            Element childElement = ((Element) compilerNode).element(key);

                            if (childElement != null) {
                                String textTrim = childElement.getTextTrim();
                                String substitutedText = sub.replace(textTrim);
                                definedSettings.add(new VersionDefinitionJ(value, substitutedText));
                            }
                        }
                    }
                }
            }
        }

        result.addAll(definedSettings);

        return !definedSettings.isEmpty();
    }
}

