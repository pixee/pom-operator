package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.*
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class POMOperatorQueryTest {
    private val LOGGER: Logger = LoggerFactory.getLogger(POMOperatorTest::class.java)

    @Test
    fun testBasicQuery() {
        QueryType.values().filterNot { it == QueryType.NONE } .forEach { queryType ->
            val context =
                ProjectModelFactory
                    .load(this.javaClass.getResource("pom-1.xml")!!)
                    .withQueryType(queryType)
                    .build()

            val dependencies = POMOperator.queryDependency(context)

            LOGGER.debug("Dependencies found: {}", dependencies)

            assertTrue(dependencies.isNotEmpty(), "Dependencies are not empty")
        }
    }

    @Test
    fun testFailedSafeQuery() {
        val context =
            ProjectModelFactory
                .load(this.javaClass.getResource("pom-broken.xml")!!)
                .withQueryType(QueryType.SAFE)
                .build()

        val dependencies = POMOperator.queryDependency(context)

        assertTrue(dependencies.isEmpty(), "Dependencies are empty")
    }

    @Test(expected = IllegalStateException::class)
    fun testFailedUnsafeQuery() {
        val context =
            ProjectModelFactory
                .load(this.javaClass.getResource("pom-broken.xml")!!)
                .withQueryType(QueryType.UNSAFE)
                .build()

        val dependencies = POMOperator.queryDependency(context)

        assertTrue(dependencies.isEmpty(), "Dependencies are empty")
    }

    @Test
    fun testAllQueryTypes() {
        listOf("pom-1.xml", "pom-3.xml").forEach { pomFile ->
            Chain.AVAILABLE_QUERY_COMMANDS.forEach {
                val commandClassName = "io.github.pixee.maven.operator.${it.second}"

                val commandListOverride = listOf(Class.forName(commandClassName).newInstance() as Command)

                val context =
                    ProjectModelFactory
                        .load(this.javaClass.getResource(pomFile)!!)
                        .withQueryType(QueryType.UNSAFE)
                        .build()

                val dependencies = POMOperator.queryDependency(context, commandList = commandListOverride)

                assertTrue(dependencies.isNotEmpty(), "Dependencies are not empty")
            }
        }
    }


    @Test
    fun testTemporaryDirectory() {
        QueryType.values().filterNot { it == QueryType.NONE } .forEach { queryType ->
            val tempDirectory = Files.createTempDirectory("mvn-repo").toFile()

            tempDirectory.mkdirs()

            LOGGER.info("Using queryType: $queryType at $tempDirectory")

            assertEquals(tempDirectory.list()?.filter { File(it).isDirectory }?.size ?: 0, 0, "There must be no files")

            val context =
                ProjectModelFactory
                    .load(this.javaClass.getResource("pom-1.xml")!!)
                    .withQueryType(queryType)
                    .withRepositoryPath(tempDirectory)
                    .build()

            val dependencies = POMOperator.queryDependency(context)

            LOGGER.debug("Dependencies found: {}", dependencies)

            assertTrue(dependencies.isNotEmpty(), "Dependencies are not empty")

            assertEquals(tempDirectory.list().filter { File(it).isDirectory }.size, 0, "There must be files")
        }
    }
}