package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.AbstractQueryCommandJ
import io.github.pixee.maven.operator.java.InvalidContextExceptionJ
import io.github.pixee.maven.operator.java.ProjectModelJ
import java.io.File

val CHECK_PARENT_DIR_COMMAND = object : AbstractQueryCommandJ() {
    override fun extractDependencyTree(outputPath: File, pomFilePath: File, c: ProjectModelJ) =
        throw InvalidContextExceptionJ()

    override fun execute(c: ProjectModelJ): Boolean {
        val localRepositoryPath = getLocalRepositoryPath(c)

        if (!localRepositoryPath.exists()) {
            localRepositoryPath.mkdirs()
        }

        return false
    }
}