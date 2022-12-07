package io.openpixee.maven.operator

import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.InvocationRequest
import java.io.File
import java.nio.file.Paths
import java.util.*

/**
 * Common Base Class - Meant to be used by Simple Queries using either Invoker and/or Embedder, thus
 * relying on dependency:tree mojo outputting into a text file - which might be cached.
 *
 */
abstract class AbstractSimpleQueryCommand : AbstractSimpleCommand() {
    protected fun getOutputPath(pomFilePath: File): File {
        val basePath = pomFilePath.parentFile

        val outputBasename = "output-%08X.txt".format(pomFilePath.hashCode())

        val outputPath = File(basePath, outputBasename)

        return outputPath
    }

    /**
     * Given a POM URI, returns a File Object
     *
     * @param c ProjectModel
     */
    protected fun getPomFilePath(c: ProjectModel): File = Paths.get(c.pomPath!!.toURI()).toFile()

    /**
     * Abstract Method to extract dependencies
     *
     * @param outputPath Output Path to where to store the content
     * @param pomFilePath Input Pom Path
     * @param c Project Model
     */
    abstract fun extractDependencyTree(outputPath: File, pomFilePath: File, c: ProjectModel)

    /**
     * Internal Holder Variable
     *
     * Todo: OF COURSE IT BREAKS THE PROTOCOL
     */
    internal var result: Collection<Dependency>? = null

    /**
     * We declare the main logic here - details are made in the child classes for now
     */
    override fun execute(c: ProjectModel): Boolean {
        val pomFilePath = getPomFilePath(c)

        val outputPath = getOutputPath(pomFilePath)

        /**
         * Can we cache it? If not, lets generate it first
         */
        if (!outputPath.exists()) {
            extractDependencyTree(outputPath, pomFilePath, c)
        } else {
            // Using Cached Version
        }

        this.result = extractDependencies(outputPath).values

        return false
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
    protected fun extractDependencies(outputPath: File) = outputPath.readLines().drop(1).map {
        it.trim(*"+-|\\ ".toCharArray())
    }.map {
        it to it.split(':')
    }.associate { (line, elements) ->
        val (groupId, artifactId, packaging, version, scope) = elements

        line to Dependency(
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            packaging = packaging,
            scope = scope
        )
    }

    protected fun buildInvocationRequest(
        outputPath: File,
        pomFilePath: File,
        c: ProjectModel
    ): InvocationRequest {
        val props = Properties(System.getProperties()).apply {
            //System.getProperties().forEach { t, u -> setProperty(t as String, u as String) }
            setProperty("outputFile", outputPath.absolutePath)
        }

        val request: InvocationRequest = DefaultInvocationRequest().apply {
            findMavenHomeOrExecutable(this)

            pomFile = pomFilePath

            isShellEnvironmentInherited = true

            isNoTransferProgress = true
            isBatchMode = true
            isRecursive = false
            profiles = c.activeProfiles.toList()
            isDebug = true

            properties = props

            goals = listOf(DEPENDENCY_TREE_MOJO_REFERENCE)
        }

        return request
    }

    /**
     * Locates where Maven is at - either via Environment Variable and/or by looking at the path.
     *
     * @param invocationRequest InvocationRequest to be filled up
     */
    private fun findMavenHomeOrExecutable(invocationRequest: InvocationRequest) {
        val m2homeEnvVar = System.getenv("M2_HOME")

        if (null != m2homeEnvVar) {
            val m2HomeDir = File(m2homeEnvVar)

            if (m2HomeDir.isDirectory) {
                invocationRequest.mavenHome = m2HomeDir
//
//                return
            }
        }

        val pathElements = System.getenv("PATH").split(":")

        val foundExecutable = pathElements.map { File(File(it), "mvn") }
            .findLast { it.exists() && it.isFile && it.canExecute() }

        if (null != foundExecutable) {
            invocationRequest.mavenExecutable = foundExecutable

            return
        }

        throw IllegalStateException("Missing Maven Home")
    }

    companion object {
        /**
         * Mojo Reference
         */
        val DEPENDENCY_TREE_MOJO_REFERENCE =
            "org.apache.maven.plugins:maven-dependency-plugin:3.3.0:tree"
    }

}