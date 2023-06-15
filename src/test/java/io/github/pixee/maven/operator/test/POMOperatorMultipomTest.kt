package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.Dependency
import io.github.pixee.maven.operator.POMDocumentFactory
import io.github.pixee.maven.operator.ProjectModelFactory
import io.github.pixee.maven.operator.WrongDependencyTypeException
import org.junit.Test
import kotlin.test.assertTrue

class POMOperatorMultipomTest : AbstractTestBase() {
    @Test(expected = WrongDependencyTypeException::class)
    fun testWithParentAndChildMissingPackaging() {
        val parentResource = getResource("parent-and-child-parent-broken.xml")

        val parentPomFiles = listOf(POMDocumentFactory.load(parentResource))

        val parentPom = ProjectModelFactory.load(
            parentResource,
        ).withParentPomFiles(parentPomFiles)

        gwt(
            "parent-and-child",
            parentPom.withDependency(Dependency.fromString("org.dom4j:dom4j:2.0.3"))
        )
    }

    @Test(expected = WrongDependencyTypeException::class)
    fun testWithParentAndChildWrongType() {
        val parentResource = getResource("parent-and-child-child-broken.xml")

        val parentPomFile = POMDocumentFactory.load(getResource("parent-and-child-parent.xml"))

        val parentPomFiles = listOf(parentPomFile)

        val parentPom = ProjectModelFactory.load(
            parentResource,
        ).withParentPomFiles(parentPomFiles)

        gwt(
            "parent-and-child-wrong-type",
            parentPom.withDependency(Dependency.fromString("org.dom4j:dom4j:2.0.3"))
        )
    }

    @Test
    fun testWithMultiplePomsBasic() {
        val parentPomFile = getResource("sample-parent/pom.xml")

        val parentPom = ProjectModelFactory.load(
            getResource("sample-child-with-relativepath.xml")
        ).withParentPomFiles(listOf(POMDocumentFactory.load(parentPomFile)))
            .withUseProperties(false)

        val result = gwt(
            "multiple-pom-basic",
            parentPom.withDependency(Dependency.fromString("org.dom4j:dom4j:2.0.3"))
        )

        assertTrue(result.allPomFiles.size == 2, "There should be two files")
        assertTrue(result.allPomFiles.all { it.dirty }, "All files were modified")
    }
}
