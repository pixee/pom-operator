package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.ProjectModelJ
import org.apache.maven.shared.invoker.DefaultInvoker
import org.apache.maven.shared.invoker.InvocationRequest
import java.io.File

class QueryByInvoker : AbstractQueryCommand() {

    override fun extractDependencyTree(
        outputPath: File,
        pomFilePath: File,
        c: ProjectModelJ
    ) {
        val invoker = DefaultInvoker()

        val invocationRequest: InvocationRequest =
            buildInvocationRequest(outputPath, pomFilePath, c)

        val invocationResult = invoker.execute(invocationRequest)

        val exitCode = invocationResult.exitCode

        if (0 != exitCode) {
            throw IllegalStateException(
                "Unexpected Status Code from Invoker: %02d".format(
                    exitCode
                )
            )
        }
    }

}