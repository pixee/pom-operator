package io.openpixee.maven.operator

val CheckDependencyPresent = object : AbstractSimpleCommand() {
    override fun execute(c: ProjectModel): Boolean {
        if (null == c.dependency)
            throw MissingDependencyException("Dependency must be present for modify")

        return false
    }
}