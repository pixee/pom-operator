package io.github.pixee.maven.operator;

public class VersionDefinitionJ {
    private KindJ kind;
    private String value;

    public VersionDefinitionJ(KindJ kind, String value) {
        this.kind = kind;
        this.value = value;
    }
    public KindJ getKind() {
        return kind;
    }

    public String getValue() {
        return value;
    }

    public void setKind(KindJ kind) {
        this.kind = kind;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
