package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.UtilJ


val SimpleDependencyManagement = object : AbstractCommand() {
    override fun execute(pm: ProjectModel): Boolean {
        val lookupExpression =
            UtilJ.buildLookupExpressionForDependencyManagement(pm.dependency!!)

        return handleDependency(pm, lookupExpression)
    }
}