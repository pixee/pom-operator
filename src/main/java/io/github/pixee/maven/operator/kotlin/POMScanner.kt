package io.github.pixee.maven.operator.kotlin

import io.github.pixee.maven.operator.EmbedderFacadeJ
import io.github.pixee.maven.operator.IgnorableJ
import io.github.pixee.maven.operator.POMDocumentFactoryJ
import io.github.pixee.maven.operator.POMScannerJ
import io.github.pixee.maven.operator.ProjectModelFactoryJ
import org.apache.maven.model.building.ModelBuildingException
import org.dom4j.Element
import org.dom4j.tree.DefaultElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.notExists

object POMScanner {
    private val LOGGER: Logger = LoggerFactory.getLogger(POMScanner::class.java)

    private val RE_WINDOWS_PATH = Regex("""^\p{Alpha}:""")

    @JvmStatic
    fun scanFrom(originalFile: File, topLevelDirectory: File): ProjectModelFactoryJ {
        return POMScannerJ.scanFrom(originalFile, topLevelDirectory)
    }

    @JvmStatic
    fun legacyScanFrom(originalFile: File, topLevelDirectory: File): ProjectModelFactoryJ {
        val pomFile: POMDocument = POMDocumentFactoryJ.load(originalFile)
        val parentPomFiles: MutableList<POMDocument> = arrayListOf()

        val pomFileQueue: Queue<Element> = LinkedList()

        val relativePathElement =
            pomFile.pomDocument.rootElement.element("parent")?.element("relativePath")

        val parentElement = pomFile.pomDocument.rootElement.element("parent")

        if (relativePathElement != null && relativePathElement.textTrim.isNotEmpty()) {
            pomFileQueue.add(relativePathElement)
        } else if (relativePathElement == null && parentElement != null) {
            // skip trying to find for a parent if we are at the root
            if (!originalFile.parent.equals(topLevelDirectory)) {
                pomFileQueue.add(DefaultElement("relativePath").apply {
                    this.text = "../pom.xml"
                })
            }
        }

        var lastFile: File = originalFile

        fun resolvePath(baseFile: File, relativePath: String): Path {
            var parentDir = baseFile

            if (parentDir.isFile) {
                parentDir = parentDir.parentFile
            }

            val result = File(File(parentDir, relativePath).toURI().normalize().path)

            lastFile = if (result.isDirectory) {
                result
            } else {
                result.parentFile
            }

            return Paths.get(result.absolutePath)
        }

        val prevPaths: MutableSet<String> = linkedSetOf()

        var prevPOMDocument = pomFile

        while (pomFileQueue.isNotEmpty()) {
            val relativePathElement = pomFileQueue.poll()

            if (relativePathElement.textTrim.isEmpty()) {
                break
            }

            val relativePath = fixPomRelativePath(relativePathElement.text)

            if (!isRelative(relativePath)) {
                LOGGER.warn("not relative: $relativePath")

                break
            }

            if (prevPaths.contains(relativePath)) {
                LOGGER.warn("loop: ${pomFile.file}, relativePath: $relativePath")

                break
            } else {
                prevPaths.add(relativePath)
            }

            val newPath = resolvePath(lastFile, relativePath)

            if (newPath.notExists()) {
                LOGGER.warn("new path does not exist: $newPath")

                break
            }

            if (0L == newPath.toFile().length()) {
                LOGGER.warn("File has zero length: $newPath")

                break
            }

            if (!newPath.startsWith(topLevelDirectory.absolutePath)) {
                LOGGER.warn("Not a children: $newPath (absolute: ${topLevelDirectory.absolutePath}")

                break
            }

            val newPomFile = POMDocumentFactoryJ.load(newPath.toFile())

            val hasParent = newPomFile.pomDocument.rootElement.element("parent") != null
            val hasRelativePath = newPomFile.pomDocument.rootElement.element("parent")
                ?.element("relativePath") != null

            if (!hasRelativePath && hasParent) {
                val parentElement = newPomFile.pomDocument.rootElement.element("parent")

                parentElement.add(DefaultElement("relativePath").apply {
                    this.text = "../pom.xml"
                })
            }

            // One Last Test - Does the previous mention at least ArtifactId equals to parent declared at previous?
            // If not break and warn

            val myArtifactId = newPomFile.pomDocument.rootElement.element("artifactId")?.text
            val prevParentArtifactId = prevPOMDocument.pomDocument.rootElement.element("parent")
                .element("artifactId")?.text

            if (null == myArtifactId || null == prevParentArtifactId) {
                LOGGER.warn("Missing previous mine or parent: $myArtifactId / $prevParentArtifactId")

                break
            }

            if (!myArtifactId.equals(prevParentArtifactId)) {
                LOGGER.warn("Previous doesn't match: $myArtifactId / $prevParentArtifactId")

                break
            }

            parentPomFiles.add(newPomFile)

            prevPOMDocument = newPomFile

            val newRelativePathElement =
                newPomFile.pomDocument.rootElement.element("parent")?.element("relativePath")

            if (newRelativePathElement != null) {
                pomFileQueue.add(newRelativePathElement)
            }
        }

        return ProjectModelFactoryJ.loadFor(
            pomFile,
            parentPomFiles
        )
    }

    private fun fixPomRelativePath(text: String?): String {
        return POMScannerJ.fixPomRelativePath(text)
    }

    private fun isRelative(path: String): Boolean {
        return POMScannerJ.isRelative(path)
    }


    private fun getParentPoms(originalFile: File): List<File> {
        return POMScannerJ.getParentPoms(originalFile)
    }
}
