package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.AbstractCommandJ
import io.github.pixee.maven.operator.java.ProjectModelJ
import io.github.pixee.maven.operator.java.UtilJ


/**
 * Represents bumping an existing dependency/
 */
val SimpleUpgrade = object : AbstractCommandJ() {
    override fun execute(pm: ProjectModelJ): Boolean {
        val lookupExpressionForDependency =
            UtilJ.buildLookupExpressionForDependency(pm.dependency!!)

        return handleDependency(pm, lookupExpressionForDependency)
    }
}