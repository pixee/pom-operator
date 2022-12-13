package io.openpixee.maven.operator.test

import io.openpixee.maven.operator.POMOperator
import io.openpixee.maven.operator.ProjectModelFactory
import io.openpixee.maven.operator.QueryType
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertTrue

class POMOperatorQueryTest {
    val LOGGER : Logger = LoggerFactory.getLogger(POMOperatorTest::class.java)

    @Test
    fun testBasicQuery() {
        QueryType.values().forEach { queryType ->
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
}