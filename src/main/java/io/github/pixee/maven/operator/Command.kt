package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.ProjectModelJ

/**
 * Represents a Command in a Chain of Responsibility Pattern
 */
interface Command {
    /**
     * Given a context, performs an operation
     *
     * @param pm Context (Project Model) to use
     * @return true if the execution was successful *AND* the chain must end
     */
    fun execute(pm: ProjectModelJ): Boolean

    /**
     * Post Processing, implementing a Filter Pattern
     */
    fun postProcess(c: ProjectModelJ): Boolean
}