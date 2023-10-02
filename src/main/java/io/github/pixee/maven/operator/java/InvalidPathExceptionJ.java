package io.github.pixee.maven.operator.java;

import java.io.File;
import java.io.IOException;

public class InvalidPathExceptionJ extends IOException {
    private final File parentPath;
    private final String relativePath;
    private final boolean loop;

    public InvalidPathExceptionJ(File parentPath, String relativePath, boolean loop) {
        super("Invalid Relative Path " + relativePath + " (from " + parentPath.getAbsolutePath() + ") (loops? " + loop + ")");
        this.parentPath = parentPath;
        this.relativePath = relativePath;
        this.loop = loop;
    }

    public InvalidPathExceptionJ(File parentPath, String relativePath) {
        this(parentPath, relativePath, false);
    }

    public File getParentPath() {
        return parentPath;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public boolean isLoop() {
        return loop;
    }
}

