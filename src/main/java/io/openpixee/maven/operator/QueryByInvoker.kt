package io.openpixee.maven.operator

import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker
import org.apache.maven.shared.invoker.InvocationRequest
import java.io.File
import java.lang.IllegalStateException
import java.nio.file.Paths
import java.util.Properties

abstract class AbstractSimpleQueryCommand : AbstractSimpleCommand() {
}

val QueryByInvoker = object : AbstractSimpleQueryCommand() {
    override fun execute(c: ProjectModel): Boolean {
        val invoker = DefaultInvoker()

        val pomFilePath = Paths.get(c.pomPath!!.toURI()).toFile()
        val basePath = pomFilePath.parentFile

        val outputBasename = "output-%08X.txt".format(pomFilePath.hashCode())

        val outputPath = File(basePath, outputBasename)

        if (!outputPath.exists()) {
            val props = Properties(System.getProperties()).apply {
                //System.getProperties().forEach { t, u -> setProperty(t as String, u as String) }
                setProperty("outputFile", outputPath.absolutePath)
            }

            val request: InvocationRequest = DefaultInvocationRequest().apply {
                pomFile = pomFilePath

                isShellEnvironmentInherited = true

                isNoTransferProgress = true
                isBatchMode = true
                isRecursive = false
                profiles = c.activeProfiles.toList()
                isDebug = true

                goals = listOf("org.apache.maven.plugins:maven-dependency-plugin:3.3.0:tree")
                properties = props

                findMavenHomeOrExecutable(this)

                //mavenExecutable = which
            }

            val invocationResult = invoker.execute(request)

            val exitCode = invocationResult.exitCode


        }

        /**
         * We receive something such as this, then filter it out:
         *
         * <pre>
         *     br.com.ingenieux:pom-operator:jar:0.0.1-SNAPSHOT
         *     +- xerces:xercesImpl:jar:2.12.1:compile
         *     |  \- xml-apis:xml-apis:jar:1.4.01:compile
         *     \- org.jetbrains.kotlin:kotlin-test:jar:1.5.31:test
         * </pre>
         *
         */

        val dependencies =
            outputPath.readLines().drop(1).map {
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

        return false
    }

    private fun findMavenHomeOrExecutable(invocationRequest: InvocationRequest) {
        val m2homeEnvVar = System.getenv("M2_HOME")

        if (null != m2homeEnvVar) {
            val m2HomeDir = File(m2homeEnvVar)

            if (m2HomeDir.isDirectory) {
                invocationRequest.mavenHome = m2HomeDir

                return
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
}