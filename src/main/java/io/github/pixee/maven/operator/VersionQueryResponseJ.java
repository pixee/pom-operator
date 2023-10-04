package io.github.pixee.maven.operator;

import com.github.zafarkhaja.semver.Version;
import lombok.Getter;

@Getter
public class VersionQueryResponseJ {

    private final Version source;
    private final Version target;

    public VersionQueryResponseJ(Version source, Version target) {
        this.source = source;
        this.target = target;
    }

}
