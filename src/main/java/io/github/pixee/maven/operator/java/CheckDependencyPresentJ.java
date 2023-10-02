package io.github.pixee.maven.operator.java;

import io.github.pixee.maven.operator.MissingDependencyException;

public class CheckDependencyPresentJ extends AbstractCommandJ {
    private static final CheckDependencyPresentJ INSTANCE = new CheckDependencyPresentJ();

    private CheckDependencyPresentJ() {
        // Private constructor to prevent instantiation
    }

    public static CheckDependencyPresentJ getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean execute(ProjectModelJ pm) {
        /**
         * CheckDependencyPresentJ requires a Dependency to be Present
         */
        if (pm.getDependency() == null)
            throw new MissingDependencyException("Dependency must be present for modify");

        return false;
    }
}

