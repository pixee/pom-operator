package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.UtilJ


/**
 * Represents bumping an existing dependency/
 */
val SimpleUpgrade = object : AbstractCommand() {
    override fun execute(pm: ProjectModel): Boolean {
        val lookupExpressionForDependency =
            UtilJ.buildLookupExpressionForDependency(pm.dependency!!)

        return handleDependency(pm, lookupExpressionForDependency)
    }
}