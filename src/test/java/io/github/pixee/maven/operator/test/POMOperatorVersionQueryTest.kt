package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.*
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.assertFalse
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class POMOperatorVersionQueryTest {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(POMOperatorTest::class.java)
    }

    @Test
    fun testBasicQuery() {
        val pomFile = "pom-1.xml"

        QueryType.values().filterNot { it == QueryType.NONE }.forEach { queryType ->
            val optionalVersionQueryResponse = versionDefinitions(pomFile, queryType)

            LOGGER.debug("Versions found: {}", optionalVersionQueryResponse)

            val versionQueryResponse = optionalVersionQueryResponse.get()

            assertTrue(
                "Version defined is 1.8 as source",
                versionQueryResponse.source.satisfies("=1.8.0"),
            )

            assertTrue(
                "Version defined is 1.8 as target",
                versionQueryResponse.target.satisfies("=1.8.0"),
            )
        }
    }

    @Test
    fun testPomVersionZero() {
        QueryType.values().filterNot { it == QueryType.NONE }.forEach { queryType ->
            val optionalVersionResponse = versionDefinitions("pom-version-0.xml", queryType)

            assertFalse("No versions defined (queryType: $queryType)", optionalVersionResponse.isPresent)
        }
    }

    @Test
    fun testPomVersion1and2() {
        (1..2).forEach {index ->
            val pomFile = "pom-version-$index.xml"

            LOGGER.info("Using file: $pomFile")

            QueryType.values().filterNot { it == QueryType.NONE }.forEach { queryType ->
                LOGGER.info("using queryType: $queryType")

                val optionalVersionQueryResponse = versionDefinitions(pomFile, queryType)

                LOGGER.debug("Versions found: {}", optionalVersionQueryResponse)

                val versionQueryResponse = optionalVersionQueryResponse.get()

                assertTrue(
                    "Version defined is 1.8 as source",
                    versionQueryResponse.source.satisfies("=1.8.0"),
                )

                assertTrue(
                    "Version defined is 1.8 as target",
                    versionQueryResponse.target.satisfies("=1.8.0"),
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

                val optionalVersionQueryResponse = versionDefinitions(pomFile, queryType, offline = true)

                LOGGER.debug("Versions found: {}", optionalVersionQueryResponse)

                val versionQueryResponse = optionalVersionQueryResponse.get()

                assertTrue(
                    "Version defined is 1.8 as source",
                    versionQueryResponse.source.satisfies("=1.8.0"),
                )

                assertTrue(
                    "Version defined is 1.8 as target",
                    versionQueryResponse.target.satisfies("=1.8.0"),
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

            val optionalVersionQueryResponse = versionDefinitions(pomFile, queryType)

            LOGGER.debug("Versions found: {}", optionalVersionQueryResponse)

            val versionQueryResponse = optionalVersionQueryResponse.get()

            assertTrue(
                "Version defined is 9",
                versionQueryResponse.source.satisfies("=9.0.0"),
            )

            assertTrue(
                "Version defined is 9",
                versionQueryResponse.target.satisfies("=9.0.0"),
            )
        }
    }

    @Test
    fun testPomVersionsMismatching() {
        val pomFile = "pom-version-7.xml"

        QueryType.values().filterNot { it == QueryType.NONE }.forEach { queryType ->
            val optionalVersionQueryResponse = versionDefinitions(pomFile, queryType)

            LOGGER.debug("Versions found: {}", optionalVersionQueryResponse)

            val versionQueryResponse = optionalVersionQueryResponse.get()

            assertTrue(
                "Version defined is 1.7 as source",
                versionQueryResponse.source.satisfies("=1.7.0"),
            )

            assertTrue(
                "Version defined is 1.8 as target",
                versionQueryResponse.target.satisfies("=1.8.0"),
            )
        }
    }


    private fun versionDefinitions(
        pomFile: String,
        queryType: QueryType,
        offline: Boolean = false
    ): Optional<VersionQueryResponse> {
        val context =
            ProjectModelFactory
                .load(this.javaClass.getResource(pomFile)!!)
                .withQueryType(queryType)
                .withOffline(offline)
                .build()

        return POMOperator.queryVersions(context)
    }
}