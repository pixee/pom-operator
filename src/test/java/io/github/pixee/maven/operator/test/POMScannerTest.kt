package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.InvalidPathException
import io.github.pixee.maven.operator.POMScanner
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.fail

class POMScannerTest: AbstractTestBase() {
    val currentDirectory = File(System.getProperty("user.dir"))

    @Test
    fun testBasic() {
        val pomFile = getResourceAsFile("sample-child-with-relativepath.xml")

        val pmf = POMScanner.scanFrom(pomFile, currentDirectory)
    }

    @Test(expected = InvalidPathException::class)
    fun testTwoLevelsWithLoop() {
        val pomFile = getResourceAsFile("sample-child-with-relativepath-and-two-levels.xml")

        val pmf = POMScanner.scanFrom(pomFile, currentDirectory)
    }

    @Test
    fun testTwoLevelsWithoutLoop() {
        val pomFile = getResourceAsFile("sample-child-with-relativepath-and-two-levels-nonloop.xml")

        val pmf = POMScanner.scanFrom(pomFile, currentDirectory)

        assertTrue(pmf.build().parentPomFiles.size == 2, "There must be two parent pom files")
    }

    @Test
    fun testInvalidRelativePaths() {
        for (index in 1..4) {
            val pomFile = getResourceAsFile("sample-child-with-broken-path-${index}.xml")

            try {
                POMScanner.scanFrom(pomFile, currentDirectory)

                fail("Unreachable code")
            } catch (e: Exception) {
                LOGGER.info("Exception thrown: ", e)

                if (e is InvalidPathException) {
                    continue
                }

                throw e
            }
        }
    }
}