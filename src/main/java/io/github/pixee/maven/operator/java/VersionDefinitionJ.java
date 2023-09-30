package io.github.pixee.maven.operator.java;

import io.github.pixee.maven.operator.Kind;

public class VersionDefinitionJ {
    private Kind kind;
    private String value;

    public VersionDefinitionJ(Kind kind, String value) {
        this.kind = kind;
        this.value = value;
    }
    public Kind getKind() {
        return kind;
    }

    public String getValue() {
        return value;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
