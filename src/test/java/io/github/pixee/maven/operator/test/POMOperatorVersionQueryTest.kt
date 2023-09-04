package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.POMOperator
import io.github.pixee.maven.operator.ProjectModelFactory
import io.github.pixee.maven.operator.QueryType
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
        QueryType.values().filterNot { it == QueryType.NONE }.forEach { queryType ->
            val context =
                ProjectModelFactory
                    .load(this.javaClass.getResource("pom-1.xml")!!)
                    .withQueryType(queryType)
                    .build()

            val versions = POMOperator.queryVersions(context)

            LOGGER.debug("Versions  found: {}", versions)

            assertTrue("Versions are not empty", versions.isNotEmpty())

            assertTrue(
                "Version defined is 1.8",
                versions.map { it.value }.toSet().first().equals("1.8")
            )
        }
    }
}