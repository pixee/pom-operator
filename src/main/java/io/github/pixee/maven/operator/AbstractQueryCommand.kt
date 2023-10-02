package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.AbstractCommandJ
import io.github.pixee.maven.operator.java.AbstractQueryCommandJ
import io.github.pixee.maven.operator.java.ProjectModelJ
import io.github.pixee.maven.operator.java.UtilJ
import org.apache.commons.lang3.SystemUtils
import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.InvocationRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths
import java.util.*

/**
 * Common Base Class - Meant to be used by Simple Queries using either Invoker and/or Embedder, thus
 * relying on dependency:tree mojo outputting into a text file - which might be cached.
 *
 */
abstract class AbstractQueryCommand : AbstractCommandJ() {
    /**
     * Generates a temporary file path used to store the output of the <pre>dependency:tree</pre> mojo
     *
     * @param pomFilePath POM Original File Path
     */
    private fun getOutputPath(pomFilePath: File): File {
        return AbstractQueryCommandJ.getOutputPath(pomFilePath)
    }

    /**
     * Given a POM URI, returns a File Object
     *
     * @param d POMDocument
     */
    protected fun getPomFilePath(d: POMDocument): File = AbstractQueryCommandJ.getPomFilePath(d)

    /**
     * Abstract Method to extract dependencies
     *
     * @param outputPath Output Path to where to store the content
     * @param pomFilePath Input Pom Path
     * @param c Project Model
     */
    abstract fun extractDependencyTree(outputPath: File, pomFilePath: File, c: ProjectModelJ)

    /**
     * Internal Holder Variable
     *
     * Todo: OF COURSE IT BREAKS THE PROTOCOL
     */
    internal var result: Collection<Dependency>? = null

    /**
     * We declare the main logic here - details are made in the child classes for now
     */

    override fun execute(pm: ProjectModelJ): Boolean {
        val pomFilePath = getPomFilePath(pm.pomFile)

        val outputPath = getOutputPath(pomFilePath)

        if (outputPath.exists()) {
            outputPath.delete()
        }

        try {
            extractDependencyTree(outputPath, pomFilePath, pm)
        } catch (e: InvalidContextException) {
            return false
        }

        this.result = extractDependencies(outputPath).values

        return true
    }

    /**
     * Given a File containing the output of the dependency:tree mojo, read its contents and parse, creating an array of dependencies
     *
     * About the file contents: We receive something such as this, then filter it out:
     *
     * <pre>
     *     br.com.ingenieux:pom-operator:jar:0.0.1-SNAPSHOT
     *     +- xerces:xercesImpl:jar:2.12.1:compile
     *     |  \- xml-apis:xml-apis:jar:1.4.01:compile
     *     \- org.jetbrains.kotlin:kotlin-test:jar:1.5.31:test
     * </pre>
     *
     * @param outputPath file to read
     */
    protected fun extractDependencies(outputPath: File) = AbstractQueryCommandJ.extractDependencies(outputPath)

    protected fun buildInvocationRequest(
        outputPath: File,
        pomFilePath: File,
        c: ProjectModelJ
    ): InvocationRequest {
        return AbstractQueryCommandJ.buildInvocationRequest(outputPath, pomFilePath, c)
    }

    /**
     * Locates where Maven is at - HOME var and main launcher script.
     *
     * @param invocationRequest InvocationRequest to be filled up
     */
    private fun findMaven(invocationRequest: InvocationRequest) {
        AbstractQueryCommandJ.findMaven(invocationRequest)
    }

    companion object {
        /**
         * Mojo Reference
         */
        const val DEPENDENCY_TREE_MOJO_REFERENCE =
            "org.apache.maven.plugins:maven-dependency-plugin:3.3.0:tree"

        val LOGGER: Logger = LoggerFactory.getLogger(AbstractQueryCommand::class.java)
    }

    override fun postProcess(c: ProjectModelJ): Boolean = false
}
