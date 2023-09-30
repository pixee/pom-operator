package io.github.pixee.maven.operator.java;

import io.github.pixee.maven.operator.Kind;
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
}
