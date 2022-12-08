package io.openpixee.maven.operator

import org.apache.maven.cli.MavenCli
import org.apache.maven.shared.invoker.InvocationRequest
import org.apache.maven.shared.invoker.MavenCommandLineBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * Uses Maven Embedder to Implement
 */
class QueryByEmbedder : AbstractSimpleQueryCommand() {
    val LOGGER: Logger = LoggerFactory.getLogger(QueryByEmbedder::class.java)

    /**
     * Runs the "dependency:tree" mojo - but using Embedder instead.
     */
    override fun extractDependencyTree(outputPath: File, pomFilePath: File, c: ProjectModel) {
        val mavenCli = MavenCli()

        val cliBuilder = MavenCommandLineBuilder()

        val invocationRequest: InvocationRequest =
            buildInvocationRequest(outputPath, pomFilePath, c)

        System.setProperty("maven.multiModuleProjectDirectory", pomFilePath.parent)

        val cliBuilderResult = cliBuilder.build(invocationRequest)

        val cliArgs = cliBuilderResult.commandline.toList().drop(1).toTypedArray()

        val baosOut = ByteArrayOutputStream()
        val baosErr = ByteArrayOutputStream()

        val result: Int = mavenCli.doMain(
            cliArgs,
            pomFilePath.parent,
            PrintStream(baosOut, true),
            PrintStream(baosErr, true)
        )

        LOGGER.debug("baosOut: {}", baosOut.toString())
        LOGGER.debug("baosErr: {}", baosErr.toString())

        /**
         * Sometimes the Embedder will fail - it will return this specific exit code (1) as well as
         * not generate this file
         *
         * If that happens, we'll move to the next strategy (Invoker-based likely) by throwing a
         * custom exception which is caught inside the Chain#execute method
         *
         * @see Chain#execute
         */
        if (1 == result && (!outputPath.exists()))
            throw InvalidContextException()

        if (0 != result)
            throw IllegalStateException("Unexpected status code: %02d".format(result))
    }

}