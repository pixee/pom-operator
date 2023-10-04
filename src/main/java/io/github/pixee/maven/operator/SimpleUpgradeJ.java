package io.github.pixee.maven.operator;

/**
 * Represents bumping an existing dependency.
 */
public class SimpleUpgradeJ extends AbstractCommandJ {

    private static SimpleUpgradeJ instance;

    private SimpleUpgradeJ() {
        // Private constructor to prevent instantiation.
    }

    public static SimpleUpgradeJ getInstance() {
        if (instance == null) {
            instance = new SimpleUpgradeJ();
        }
        return instance;
    }

    @Override
    public boolean execute(ProjectModelJ pm) {
        if (pm.getDependency() == null) {
            throw new NullPointerException("Dependency must not be null.");
        }

        String lookupExpressionForDependency = UtilJ.buildLookupExpressionForDependency(pm.getDependency());

        return handleDependency(pm, lookupExpressionForDependency);
    }
}

