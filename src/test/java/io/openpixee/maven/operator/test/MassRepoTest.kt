package io.openpixee.maven.operator.test

import io.openpixee.maven.operator.Dependency
import io.openpixee.maven.operator.POMOperator
import io.openpixee.maven.operator.ProjectModelFactory
import org.apache.commons.lang3.SystemUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

data class TestRepo(
    val slug: String,
    val branch: String = "master",
    val pomPath: String = "pom.xml"
) {
    fun cacheDir() = BASE_CACHE_DIR.resolve("repo-%08X".format(slug.hashCode()))

    companion object {
        val BASE_CACHE_DIR: File = File(System.getProperty("user.dir") + "/.cache").absoluteFile
    }
}

/**
 * This test actually picks up several github repos and apply the POM Operator and checks for a runtime dependency change
 */
class MassRepoIT {
    /*
    https://github.com/trending/java?since=daily

    apache/pulsar
    metersphere/metersphere
    apache/rocketmq
    OpenAPITools/openapi-generator
    casbin/jcasbin
    trinodb/trino
    bytedeco/javacv
    flowable/flowable-engine

    Azure/azure-sdk-for-java
    questdb/questdb
     */

    val repos = listOf(
        TestRepo(
            "apache/pulsar",
            pomPath = "pulsar-broker/pom.xml"
        ) to "commons-codec:commons-codec:1.14",
        TestRepo(
            "metersphere/metersphere",
            branch = "main",
            pomPath = "framework/eureka/pom.xml"
        ) to "commons-lang3:commons-lang3:3.6",
        TestRepo(
            "apache/rocketmq",
            pomPath = "common/pom.xml"
        ) to "commons-codec:commons-codec:1.15",
        TestRepo(
            "OpenAPITools/openapi-generator",
            pomPath = "modules/openapi-generator-core/pom.xml"
        ) to "com.google.guava:guava:31.0-jre",
        TestRepo(
            "casbin/jcasbin",
        ) to "com.google.code.gson:gson:2.8.0",
        /*
        TestRepo(
            "trinodb/trino",
            pomPath = "core/trino-main/pom.xml"
        ) to "org.apache.commons:commons-math3:3.6.0",
         */
        TestRepo(
            "bytedeco/javacv"
        ) to "org.jogamp.jocl:jocl-main:2.3.1",
        TestRepo(
            "flowable/flowable-engine",
            branch = "main",
            pomPath = "modules/flowable-engine-common-api/pom.xml"
        ) to "org.apache.commons:commons-lang3:3.11",
    )

    /**
     * Checks out - or resets - a stored github repo
     */
    private fun checkoutOrResetCachedRepo(repo: TestRepo) {
        if (!repo.cacheDir().exists()) {
            // git clone -b branch github.com/slug/ dir
            val command = arrayOf(
                "git",
                "clone",
                "-b",
                repo.branch,
                "https://github.com/${repo.slug}.git",
                repo.cacheDir().canonicalPath
            )

            LOGGER.debug("Running command: ${command.joinToString(" ")}")

            val process = ProcessBuilder(*command)
                .directory(TestRepo.BASE_CACHE_DIR.canonicalFile)
                .inheritIO()
                .start()

            process.waitFor()
        } else {
            val command = arrayOf("git", "reset", "--hard", "HEAD")

            LOGGER.debug("Running command: ${command.joinToString(" ")}")

            val process = ProcessBuilder(*command)
                .directory(repo.cacheDir())
                .inheritIO()
                .start()

            process.waitFor()
        }
    }

    /**
     * Sanity Test on a single repo
     */
    @Test
    fun testBasic() {
        val firstCase = repos.first()

        testOnRepo(firstCase.first, firstCase.second)
    }

    /**
     * THATS THE FULL TEST
     */
    @Test
    fun testAllOthers() {
        repos.forEach { testOnRepo(it.first, it.second) }
    }

    private fun testOnRepo(
        sampleRepo: TestRepo,
        dependencyToUpgradeString: String
    ) {
        LOGGER.info(
            "Testing on repo {}, branch {} with dependency {}",
            sampleRepo.slug,
            sampleRepo.branch,
            dependencyToUpgradeString
        )

        checkoutOrResetCachedRepo(sampleRepo)

        val originalDependencies = getDependenciesFrom(sampleRepo)

        LOGGER.info("dependencies: {}", originalDependencies)

        val dependencyToUpgrade = Dependency.fromString(dependencyToUpgradeString)
        val context = ProjectModelFactory.load(File(sampleRepo.cacheDir(), sampleRepo.pomPath))
            .withDependency(dependencyToUpgrade)
            .withSkipIfNewer(false)
            .build()

        POMOperator.modify(context)

        val alternatePomFile =
            File(File(sampleRepo.cacheDir(), sampleRepo.pomPath).parent, "pom-modified.xml")

        alternatePomFile.writeText(context.resultPom.asXML())

        val finalDependencies =
            getDependenciesFrom(alternatePomFile.canonicalPath, sampleRepo.cacheDir())

        LOGGER.info("dependencies: {}", finalDependencies)

        val dependencyAsStringWithPackaging = dependencyToUpgrade.toString()

        assertFalse(
            "Dependency should be originally missing", originalDependencies.contains(
                dependencyAsStringWithPackaging
            )
        )
        assertTrue(
            "New Dependency should be appearing", finalDependencies.contains(
                dependencyAsStringWithPackaging
            )
        )
    }

    private fun getDependenciesFrom(repo: TestRepo): String =
        getDependenciesFrom(repo.pomPath, repo.cacheDir())

    private fun getDependenciesFrom(pomPath: String, dir: File): String {
        val outputFile = File.createTempFile("tmp-pom", ".txt")

        if (outputFile.exists()) {
            outputFile.delete()
        }

        val command = if (SystemUtils.IS_OS_WINDOWS) {
            listOf("cmd.exe", "/c")
        } else {
            emptyList()
        } + listOf(
            "mvn",
            "-B",
            "-f",
            pomPath,
            "dependency:tree",
            "-Dscope=runtime",
            "-DoutputFile=${outputFile.canonicalPath}"
        )

        val process = ProcessBuilder(*command.toTypedArray())
            .directory(dir)
            .inheritIO()
            .start()

        process.waitFor()

        val result = outputFile.readText()

        outputFile.delete()

        return result
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(MassRepoIT::class.java)
    }

    init {
        /**
         * Creates the Cache Directory
         */
        if (!TestRepo.BASE_CACHE_DIR.exists())
            TestRepo.BASE_CACHE_DIR.mkdirs()
    }
}