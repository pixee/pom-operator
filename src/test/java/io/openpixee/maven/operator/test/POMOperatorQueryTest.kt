package io.openpixee.maven.operator.test

import io.openpixee.maven.operator.POMOperator
import io.openpixee.maven.operator.ProjectModel
import io.openpixee.maven.operator.ProjectModelFactory
import org.junit.Test
import org.slf4j.LoggerFactory

class POMOperatorQueryTest {
    val LOGGER = LoggerFactory.getLogger(POMOperatorTest::class.java)

    @Test
    fun testBasicQuery() {
        val context =
            ProjectModelFactory
                .load(this.javaClass.getResource("pom-1.xml"))
                .build()

        POMOperator.queryDependency(context)
    }
}