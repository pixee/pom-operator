package io.openpixee.maven.operator

import org.apache.maven.cli.CliRequest
import org.apache.maven.cli.MavenCli
import org.apache.maven.shared.invoker.InvocationRequest
import org.apache.maven.shared.invoker.MavenCommandLineBuilder
import java.io.File

/**
 * Uses Maven Embedder to Implement
 */
class QueryByEmbedder : AbstractSimpleQueryCommand() {
    /**
     * Runs the "dependency:tree" mojo - but using the Embedder instead.
     */
    override fun extractDependencyTree(outputPath: File, pomFilePath: File, c: ProjectModel) {
        val mavenCli = MavenCli()

        val cliBuilder = MavenCommandLineBuilder()

        val invocationRequest: InvocationRequest = buildInvocationRequest(outputPath, pomFilePath, c)

        System.setProperty("maven.multiModuleProjectDirectory", pomFilePath.parent)

        val cliBuilderResult = cliBuilder.build(invocationRequest)

        val cliArgs = cliBuilderResult.commandline.toList().drop(1).toTypedArray()

        val result : Int = mavenCli.doMain(cliArgs, pomFilePath.parent, System.out, System.err)

        if (0 != result)
            throw IllegalStateException("Unexpected status code: %02d".format(result))
    }
}