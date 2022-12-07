package io.openpixee.maven.operator.test

import io.openpixee.maven.operator.POMOperator
import io.openpixee.maven.operator.ProjectModelFactory
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertTrue

class POMOperatorQueryTest {
    val LOGGER : Logger = LoggerFactory.getLogger(POMOperatorTest::class.java)

    @Test
    fun testBasicQuery() {
        val context =
            ProjectModelFactory
                .load(this.javaClass.getResource("pom-1.xml")!!)
                .build()

        val dependencies = POMOperator.queryDependency(context)

        LOGGER.debug("Dependencies found: {}", dependencies)

        assertTrue(dependencies.isNotEmpty(), "Dependencies are not empty")
    }
}