package io.github.pixee.maven.operator.java;

import io.github.pixee.maven.operator.ProjectModel;

/**
 * Interface representing a command.
 */
public interface CommandJ {
    /**
     * Given a context, performs an operation.
     *
     * @param pm Context (Project Model) to use.
     * @return true if the execution was successful AND the chain must end.
     */
    boolean execute(ProjectModel pm);

    /**
     * Post Processing, implementing a Filter Pattern.
     *
     * @param c ProjectModel for post-processing.
     * @return true if post-processing was successful.
     */
    boolean postProcess(ProjectModel c);
}
