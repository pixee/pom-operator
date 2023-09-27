package io.github.pixee.maven.operator;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class Dependency {

    final String groupId;
    final String artifactId;
    final String version;
    final String classifier;
    final String packaging;
    final String scope;


    public Dependency(final String groupId, final String artifactId, final String version) {
        this(groupId, artifactId, version, null, null, null);
    }

    public Dependency(final String groupId, final String artifactId, final String version, final String classifier, final String packaging, final String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = StringUtils.isNotBlank(version) ? version : null;
        this.classifier = StringUtils.isNotBlank(classifier) ? classifier :null;
        this.packaging = StringUtils.isNotBlank(packaging) ?  packaging : "jar";
        this.scope = StringUtils.isNotBlank(scope) ? scope : "compile";
    }

    @Override
    public String toString(){
       return String.join(":", groupId, artifactId, packaging, version);
    }

    public static Dependency fromString(final String str){
        final String[] elements = str.split(":");

        if (elements.length < 3){
            throw new IllegalStateException("Give me at least 3 elements");
        }

        return new Dependency(elements[0], elements[1], elements[2]);
    }
}
