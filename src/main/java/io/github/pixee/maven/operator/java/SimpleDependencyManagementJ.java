package io.github.pixee.maven.operator.java;

/**
 * Represents handling dependency management for a project.
 */
public class SimpleDependencyManagementJ extends AbstractCommandJ {

    private static SimpleDependencyManagementJ instance;

    private SimpleDependencyManagementJ() {
        // Private constructor to prevent instantiation.
    }

    public static SimpleDependencyManagementJ getInstance() {
        if (instance == null) {
            instance = new SimpleDependencyManagementJ();
        }
        return instance;
    }

    @Override
    public boolean execute(ProjectModelJ pm) {
        if (pm.getDependency() == null) {
            throw new NullPointerException("Dependency must not be null.");
        }

        String lookupExpression = UtilJ.buildLookupExpressionForDependencyManagement(pm.getDependency());

        return handleDependency(pm, lookupExpression);
    }
}

