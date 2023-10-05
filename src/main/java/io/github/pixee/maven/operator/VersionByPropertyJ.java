package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.kotlin.POMDocument;

import java.util.*;

public class VersionByPropertyJ extends AbstractVersionCommandJ {

    @Override
    public boolean execute(ProjectModelJ pm) {
        Set<VersionDefinitionJ> definedProperties = new TreeSet<>(VERSION_KIND_COMPARATOR);

        for (Map.Entry<String, List<Pair<String, POMDocument>>> entry : pm.propertiesDefinedByFile().entrySet()) {
            String propertyName = entry.getKey();
            if (PROPERTY_TO_KIND.containsKey(propertyName)) {
                KindJ kind = PROPERTY_TO_KIND.get(propertyName);

                if (kind != null) {
                    definedProperties.add(new VersionDefinitionJ(kind, entry.getValue().get(0).getFirst()));
                }
            }
        }

        result.addAll(definedProperties);

        return !definedProperties.isEmpty();
    }

}
