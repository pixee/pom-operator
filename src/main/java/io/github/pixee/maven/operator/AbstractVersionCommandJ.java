package io.github.pixee.maven.operator;

import org.apache.commons.lang3.builder.CompareToBuilder;
import java.util.*;

public class AbstractVersionCommandJ extends AbstractCommandJ {

    public Set<VersionDefinitionJ> result = new TreeSet<>(VERSION_KIND_COMPARATOR);

    public static final Comparator<VersionDefinitionJ> VERSION_KIND_COMPARATOR = new Comparator<VersionDefinitionJ>() {
        @Override
        public int compare(VersionDefinitionJ o1, VersionDefinitionJ o2) {
            if (o1 == null) return 1;
            if (o2 == null) return -1;

            return new CompareToBuilder().append(o1.getKind(), o2.getKind()).toComparison();
        }
    };

    public static final Map<String, KindJ> TYPE_TO_KIND = new HashMap<>();
    public static final Map<String, KindJ> PROPERTY_TO_KIND = new HashMap<>();

    static {
        TYPE_TO_KIND.put("source", KindJ.SOURCE);
        TYPE_TO_KIND.put("target", KindJ.TARGET);
        TYPE_TO_KIND.put("release", KindJ.RELEASE);

        for (Map.Entry<String, KindJ> entry : TYPE_TO_KIND.entrySet()) {
            PROPERTY_TO_KIND.put("maven.compiler." + entry.getKey(), entry.getValue());
        }
    }
}
