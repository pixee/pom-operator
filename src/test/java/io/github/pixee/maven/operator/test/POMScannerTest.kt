package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.kotlin.POMScanner
import io.github.pixee.maven.operator.InvalidPathExceptionJ
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class POMScannerTest : AbstractTestBase() {
    val currentDirectory = File(System.getProperty("user.dir"))

    @Test
    fun testBasic() {
        val pomFile = getResourceAsFile("sample-child-with-relativepath.xml")

        val pmf = POMScanner.scanFrom(pomFile, currentDirectory)
    }

    @Test
    fun testTwoLevelsWithLoop() {
        val pomFile = getResourceAsFile("sample-child-with-relativepath-and-two-levels.xml")

        val pmf = POMScanner.scanFrom(pomFile, currentDirectory)
    }

    @Test
    fun testTwoLevelsWithoutLoop() {
        val pomFile = getResourceAsFile("sample-child-with-relativepath-and-two-levels-nonloop.xml")

        val pmf = POMScanner.scanFrom(pomFile, currentDirectory).build()

        assertTrue(pmf.parentPomFiles.size == 2, "There must be two parent pom files")

        val uniquePaths =
            pmf.allPomFiles().map { it.pomPath!!.toURI().normalize().toString() }.toSet()

        val uniquePathsAsString = uniquePaths.joinToString(" ")

        LOGGER.info("uniquePathsAsString: $uniquePathsAsString")

        assertTrue(uniquePaths.size == 3, "There must be three unique pom files referenced")
    }

    @Test
    fun testMultipleChildren() {
        for (index in 1..3) {
            val pomFile = getResourceAsFile("nested/child/pom/pom-$index-child.xml")

            val pm = POMScanner.legacyScanFrom(pomFile, currentDirectory).build()

            assertTrue(pm.parentPomFiles.size > 0, "There must be at least one parent pom file")

            val uniquePaths = pm.allPomFiles().map { it.pomPath!!.toURI().normalize().toString() }

            val uniquePathsAsString = uniquePaths.joinToString(" ")

            LOGGER.info("uniquePathsAsString: $uniquePathsAsString")

            assertTrue(
                "There must be aty least two unique pom files referenced",
                uniquePaths.size >= 2
            )
        }
    }

    @Test
    fun testMissingRelativeParentElement() {
        val pomFile = getResourceAsFile("nested/child/pom/pom-demo.xml")

        val pm = POMScanner.legacyScanFrom(pomFile, currentDirectory).build()

        assertTrue(pm.parentPomFiles.size == 1, "There must be a single one parent pom file")
    }

    @Test
    fun testLegacyWithInvalidRelativePaths() {
        for (index in 1..3) {
            val name = "sample-child-with-broken-path-${index}.xml"
            val pomFile = getResourceAsFile(name)

            val pmf = POMScanner.legacyScanFrom(pomFile, currentDirectory)

            assert(pmf.build().parentPomFiles.isEmpty())
        }
    }

    @Test
    fun testWithRelativePathEmpty() {
        for (index in 3..4) {
            val pomFile = getResourceAsFile("pom-multiple-pom-parent-level-${index}.xml")

            try {
                val pmf = POMScanner.scanFrom(pomFile, currentDirectory)

                assertTrue(pmf.build().parentPomFiles.isNotEmpty())
            } catch (e: InvalidPathExceptionJ) {
                LOGGER.info("Exception thrown: ", e)

                if (e is InvalidPathExceptionJ) {
                    continue
                }

                throw e
            }
        }
    }

    @Test
    fun testWithMissingRelativePath() {
        val pomFile =
            getResourceAsFile("sample-parent/sample-child/pom-multiple-pom-parent-level-6.xml")

        val pmf = POMScanner.legacyScanFrom(pomFile, currentDirectory)

        assertTrue(pmf.build().parentPomFiles.isNotEmpty())
    }
}