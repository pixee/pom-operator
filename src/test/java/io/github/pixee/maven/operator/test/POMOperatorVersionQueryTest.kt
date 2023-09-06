package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.*
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class POMOperatorVersionQueryTest {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(POMOperatorTest::class.java)
    }

    @Test
    fun testBasicQuery() {
        val pomFile = "pom-1.xml"

        QueryType.values().filterNot { it == QueryType.NONE }.forEach { queryType ->
            val versions = versionDefinitions(pomFile, queryType)

            LOGGER.debug("Versions  found: {}", versions)

            assertTrue("Versions are not empty", versions.isNotEmpty())

            assertTrue(
                "Version defined is 1.8",
                versions.map { it.value }.toSet().first().equals("1.8")
            )
        }
    }

    @Test
    fun testPomVersionZero() {
        QueryType.values().filterNot { it == QueryType.NONE }.forEach { queryType ->
            val versions = versionDefinitions("pom-version-0.xml", queryType)

            assertTrue("No versions defined (queryType: $queryType)", versions.isEmpty())
        }
    }

    @Test
    fun testPomVersion1and2() {
        (1..2).forEach {index ->
            val pomFile = "pom-version-$index.xml"

            LOGGER.info("Using file: $pomFile")

            QueryType.values().filterNot { it == QueryType.NONE }.forEach { queryType ->
                LOGGER.info("using queryType: $queryType")

                val versions = versionDefinitions(pomFile, queryType)

                LOGGER.debug("Versions  found: {}", versions)

                assertTrue("Versions are not empty", versions.isNotEmpty())

                assertTrue(
                    "Version defined is 1.8",
                    versions.map { it.value }.toSet().first().equals("1.8")
                )
            }
        }
    }

    @Test
    fun testPomVersion4and5and6Offline() {
        (4..6).forEach {index ->
            val pomFile = "pom-version-$index.xml"

            LOGGER.info("Using file: $pomFile")

            QueryType.values().filterNot { it == QueryType.NONE }.forEach { queryType ->
                LOGGER.info("using queryType: $queryType")

                val versions = versionDefinitions(pomFile, queryType, offline = true)

                LOGGER.debug("Versions  found: {}", versions)

                assertTrue("Versions are not empty", versions.isNotEmpty())

                assertTrue(
                    "Version defined is 1.8",
                    versions.map { it.value }.toSet().first().equals("1.8")
                )
            }
        }
    }

    @Test
    fun testPomVersion3() {
        val pomFile = "pom-version-3.xml"

        LOGGER.info("Using file: $pomFile")

        QueryType.values().filterNot { it == QueryType.NONE }.forEach { queryType ->
            LOGGER.info("using queryType: $queryType")

            val versions = versionDefinitions(pomFile, queryType)

            LOGGER.debug("Versions  found: {}", versions)

            assertTrue("Versions are not empty", versions.isNotEmpty())

            assertTrue(
                "Version defined is 9",
                versions.map { it.value }.toSet().first().equals("9")
            )

            assertTrue(
                "Only type defined is RELEASE",
                versions.map { it.kind}.toSet() == setOf(Kind.RELEASE)
            )
        }
    }

    @Test
    fun testPomVersionsMismatching() {
        QueryType.values().filterNot { it == QueryType.NONE }.forEach { queryType ->
            val versions = versionDefinitions("pom-version-7.xml", queryType)

            assertTrue("Versions defined (queryType: $queryType)", versions.isNotEmpty())

            assertTrue("Versions has source set to 1.7", versions.first { it.kind == Kind.SOURCE }.value.equals("1.7"))
            assertTrue("Versions has target set to 1.8", versions.first { it.kind == Kind.TARGET }.value.equals("1.8"))
        }
    }


    private fun versionDefinitions(
        pomFile: String,
        queryType: QueryType,
        offline: Boolean = false
    ): Set<VersionDefinition> {
        val context =
            ProjectModelFactory
                .load(this.javaClass.getResource(pomFile)!!)
                .withQueryType(queryType)
                .withOffline(offline)
                .build()

        return POMOperator.queryVersions(context)
    }
}