package io.openpixee.maven.operator.test

import io.openpixee.maven.operator.*
import io.openpixee.maven.operator.Util.buildLookupExpressionForDependency
import io.openpixee.maven.operator.Util.selectXPathNodes
import io.openpixee.maven.operator.Util.which
import org.apache.commons.lang3.SystemUtils
import org.dom4j.DocumentException
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.xmlunit.diff.ComparisonType
import java.io.File
import kotlin.test.assertTrue

/**
 * Unit test for simple App.
 */
class POMOperatorTest : AbstractTestBase() {
    @Test(expected = DocumentException::class)
    fun testWithBrokenPom() {
        val context = gwt(
            "broken-pom",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("broken-pom.xml")!!,
            ).withDependency(Dependency.fromString("org.dom4j:dom4j:2.0.3"))
        )
    }

    @Test
    fun testWithMultipleDependencies() {
        val deps = listOf(
            "org.slf4j:slf4j-api:1.7.25",
            "io.github.pixee:java-code-security-toolkit:0.0.2",
            "org.owasp.encoder:encoder:1.2.3",
        ).map { Dependency.fromString(it) }.toList()

        val testPom = File.createTempFile("pom", ".xml")

        POMOperatorTest::class.java.getResourceAsStream("sample-bad-pom.xml")!!
            .copyTo(testPom.outputStream())

        deps.forEach { d ->
            val projectModel = ProjectModelFactory.load(testPom)
                .withDependency(d)
                .withUseProperties(true)
                .withOverrideIfAlreadyExists(true)
                .build()

            if (POMOperator.modify(projectModel)) {
                val resultPomAsXml = String(projectModel.resultPomBytes)

                LOGGER.debug("resultPomAsXml: {}", resultPomAsXml)

                testPom.writeBytes(projectModel.resultPomBytes)
            } else {
                throw IllegalStateException("Code that shouldn't be reached out at all")
            }
        }

        val resolvedDeps = POMOperator.queryDependency(
            ProjectModelFactory.load(testPom).withQueryType(QueryType.SAFE).build()
        )

        val testPomContents = testPom.readText()

        assertTrue(3 == resolvedDeps.size, "Must have three dependencies")
        assertTrue(testPomContents.contains("<!--"), "Must have a comment inside")
        assertTrue(
            testPomContents.contains("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"),
            "Must have a formatted attribute spanning a whole line inside"
        )
    }

    @Test(expected = MissingDependencyException::class)
    fun testWithDependencyMissing() {
        gwt(
            "case-dependency-missing",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-case-1.xml")!!,
            )
        )
    }

    @Test
    fun testCaseOne() {
        val context = gwt(
            "case-1",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-case-1.xml")!!,
            ).withDependency(Dependency.fromString("org.dom4j:dom4j:2.0.3"))
        )

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)

        assertThat("Document has differences", diff.hasDifferences())

        val textDiff = getTextDifferences(context.pomDocument, context.resultPom)

        LOGGER.debug("textDiff: {}", textDiff)

        assertThat(
            "diff contains a <dependencyManagement> tag",
            textDiff.toString().contains("<dependencyManagement>")
        )
        assertThat("diff contains a <dependency> tag", textDiff.toString().contains("<dependency>"))

        val effectivePom = context.getEffectivePom()

        LOGGER.debug("effectivePom: {}", effectivePom.asXML())
    }

    @Test
    fun testCaseThree() {
        val dependencyToUpgradeOnCaseThree = Dependency("org.dom4j", "dom4j", version = "2.0.2")

        val context = gwt(
            "case-3",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-case-3.xml")!!,
            ).withDependency(dependencyToUpgradeOnCaseThree).withSkipIfNewer(false)
        )

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)

        assertThat("Document has differences", diff.hasDifferences())
        assertThat("Document has a single difference", diff.differences.toList().size == 1)
        assertThat(
            "Document has different versions",
            diff.differences.toList()[0].comparison.type == ComparisonType.TEXT_VALUE
        )
        assertThat(
            "Document has changed version set to ${dependencyToUpgradeOnCaseThree.version}",
            diff.differences.toList()[0].comparison.testDetails.value == dependencyToUpgradeOnCaseThree.version
        )
    }

    @Test
    fun testCaseThreeButWithLowerVersion() {
        val dependencyToUpgrade = Dependency("org.dom4j", "dom4j", version = "2.0.2")

        val context = gwt(
            "pom-case-three-with-lower-version",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-case-3.xml")!!,
            ).withDependency(dependencyToUpgrade).withSkipIfNewer(true)
        )

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)

        assertThat("Document has no differences", !diff.hasDifferences())
    }

    @Test
    fun testCase4() {
        val pomPath = File(POMOperatorTest::class.java.getResource("webgoat-parent.xml")!!.toURI())

        val args =
            if (SystemUtils.IS_OS_WINDOWS) {
                listOf("cmd.exe", "/c")
            } else {
                listOf()
            } +
                    listOf(
                        which("mvn")!!.absolutePath,
                        "-N",
                        "install:install-file",
                        "-DgroupId=org.owasp.webgoat",
                        "-DartifactId=webgoat-parent",
                        "-Dversion=8.2.3-SNAPSHOT",
                        "-Dpackaging=pom",
                        "-Dfile=${pomPath.absolutePath}"
                    )

        val exitCode = ProcessBuilder(
            *args.toTypedArray()
        ).start().waitFor()

        assertThat("POM install was successful", 0 == exitCode)

        val dependencyToUpgrade =
            Dependency("org.apache.activemq", "activemq-amqp", version = "5.16.2")

        val context = gwt(
            "case-4",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-case-4.xml")!!,
            ).withDependency(dependencyToUpgrade)
        )

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)

        assertThat("Document has differences", diff.hasDifferences())

        val effectivePom = context.getEffectivePom()

        assertThat(
            "Dependencies Section did change",
            effectivePom.selectXPathNodes(buildLookupExpressionForDependency(dependencyToUpgrade))
                .isNotEmpty()
        )
    }

    @Test
    fun testCaseWithEmptyElement() {
        val dependencyToUpgrade =
            Dependency("io.openpixee", "java-security-toolkit", version = "1.0.0")

        val context = gwt(
            "case-5",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-case-5.xml")!!,
            ).withDependency(dependencyToUpgrade).withUseProperties(true)
        )

        val resultPomAsString = String(context.resultPomBytes)

        assertTrue(
            resultPomAsString.contains("<project\n"),
            "There must be an unformatted preamble first line"
        )
        assertTrue(
            resultPomAsString.contains("        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">"),
            "There must be an unformatted preamble last line"
        )

        assertTrue(
            resultPomAsString.contains("<email></email>"),
            "There must be a dumb empty element"
        )
        assertTrue(
            resultPomAsString.contains("<email/>"),
            "There must be an empty element with zero spaces"
        )
        assertTrue(
            resultPomAsString.contains("<email />"),
            "There must be an empty element with one spaces"
        )
    }

    @Test
    fun testCaseWithEmptyElementHiddenInComment() {
        val dependencyToUpgrade =
            Dependency("io.openpixee", "java-security-toolkit", version = "1.0.0")

        val context = gwt(
            "case-6",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-case-6.xml")!!,
            ).withDependency(dependencyToUpgrade).withUseProperties(true)
        )

        val resultPomAsString = String(context.resultPomBytes)

        assertTrue(
            resultPomAsString.contains("<email></email>"),
            "There must be a dumb empty element"
        )
        assertTrue(
            resultPomAsString.contains("<email/>"),
            "There must be an empty element with zero spaces"
        )
        assertTrue(
            resultPomAsString.contains("<email />"),
            "There must be an empty element with one spaces"
        )
        assertTrue(
            resultPomAsString.contains("<email   /> -->"),
            "There must be an empty element with three spaces inside a comment"
        )
    }

    @Test
    fun testCaseWithProperty() {
        val dependencyToUpgrade =
            Dependency("org.dom4j", "dom4j", version = "1.0.0")

        val context = gwt(
            "case-with-property",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-with-property-simple.xml")!!,
            ).withDependency(dependencyToUpgrade).withUseProperties(true).withSkipIfNewer(true)
        )

        LOGGER.debug("original pom: {}", context.pomDocument.asXML())
        LOGGER.debug("resulting pom: {}", context.resultPom.asXML())

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)

        assertThat("Document has differences", diff.hasDifferences())

        val differenceList = diff.differences.toList()

        assertThat("Document has one difference", 1 == differenceList.size)

        assertThat(
            "Document changes a single version",
            differenceList.first().toString()
                .startsWith("Expected text value '0.0.1-SNAPSHOT' but was '1.0.0'")
        )

        assertEquals(
            "Document changes a property called 'sample.version'",
            differenceList.first().comparison.testDetails.xPath,
            "/project[1]/properties[1]/sample.version[1]/text()[1]"
        )
    }

    @Test(expected = IllegalStateException::class)
    fun testCaseWithPropertyDefinedTwice() {
        val dependencyToUpgrade =
            Dependency("org.dom4j", "dom4j", version = "1.0.0")

        val originalPom = """
<?xml version="1.0" encoding="UTF-8"?>
<project
        xmlns="http://maven.apache.org/POM/4.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>br.com.ingenieux</groupId>
    <artifactId>pom-operator</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    
    <properties>
      <dom4j.version>0.0.1</dom4j.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.dom4j</groupId>
            <artifactId>dom4j</artifactId>
            <version>${'$'}{dom4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.dom4j</groupId>
            <artifactId>dom4j-other</artifactId>
            <version>${'$'}{dom4j.version}</version>
        </dependency>
    </dependencies>
</project>
                """.trimIndent()
        val context =
            ProjectModelFactory.load(
                originalPom.byteInputStream(),
            ).withDependency(dependencyToUpgrade).withUseProperties(true)
                .withOverrideIfAlreadyExists(false)
                .build()

        POMOperator.modify(context)
    }

    @Test
    fun testCaseWithoutPropertyButDefiningOne() {
        val dependencyToUpgrade =
            Dependency("org.dom4j", "dom4j", version = "1.0.0")

        val originalPom = """
<?xml version="1.0" encoding="UTF-8"?>
<project
        xmlns="http://maven.apache.org/POM/4.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>br.com.ingenieux</groupId>
    <artifactId>pom-operator</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>org.dom4j</groupId>
            <artifactId>dom4j</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
                """.trim()
        val context =
            ProjectModelFactory.load(
                originalPom.byteInputStream(),
            ).withDependency(dependencyToUpgrade).withUseProperties(true).withSkipIfNewer(true)
                .build()

        POMOperator.modify(context)

        LOGGER.debug("original pom: {}", context.pomDocument.asXML())
        LOGGER.debug("resulting pom: {}", context.resultPom.asXML())

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)

        assertThat("Document has differences", diff.hasDifferences())

        val differencesAsList = diff.differences.toList()

        assertThat("Document has several differences", differencesAsList.size > 1)
    }

}
