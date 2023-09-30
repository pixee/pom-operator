package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.AbstractCommandJ

/**
 * Guard Command Singleton use to validate required parameters
 */
val CheckDependencyPresent = object : AbstractCommandJ() {
    override fun execute(pm: ProjectModel): Boolean {
        /**
         * CheckDependencyPresent requires a Dependency to be Present
         */
        if (null == pm.dependency)
            throw MissingDependencyException("Dependency must be present for modify")

        return false
    }
}