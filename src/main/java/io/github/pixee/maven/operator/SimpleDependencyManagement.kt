package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.buildLookupExpressionForDependencyManagement


val SimpleDependencyManagement = object : AbstractSimpleCommand() {
    override fun execute(c: ProjectModel): Boolean {
        val lookupExpression =
            buildLookupExpressionForDependencyManagement(c.dependency!!)

        return handleDependency(c, lookupExpression)
    }
}