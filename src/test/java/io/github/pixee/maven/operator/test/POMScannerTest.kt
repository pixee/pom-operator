package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.InvalidPathException
import io.github.pixee.maven.operator.POMScanner
import org.junit.Test
import java.io.File
import kotlin.test.fail

class POMScannerTest: AbstractTestBase() {
    val currentDirectory = File(System.getProperty("user.dir"))

    @Test
    fun testBasic() {
        val pomFile = getResourceAsFile("sample-child-with-relativepath.xml")

        val pmf = POMScanner.scanFrom(pomFile, currentDirectory)
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