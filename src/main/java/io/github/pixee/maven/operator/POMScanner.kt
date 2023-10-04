package io.github.pixee.maven.operator

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
    fun scanFrom(originalFile: File, topLevelDirectory: File): ProjectModelFactory {
        val originalDocument = ProjectModelFactory.load(originalFile)

        val parentPoms: List<File> = try {
            getParentPoms(originalFile)
        } catch (e: Exception) {
            if (e is ModelBuildingException) {
                Ignorable.LOGGER.debug("mbe (you can ignore): ", e)
            } else {
                LOGGER.warn("While trying embedder: ", e)
            }

            return legacyScanFrom(originalFile, topLevelDirectory)
        }

        return originalDocument
            .withParentPomFiles(parentPoms.map { POMDocumentFactory.load(it) })
    }

    @JvmStatic
    fun legacyScanFrom(originalFile: File, topLevelDirectory: File): ProjectModelFactory {
        val pomFile: POMDocument = POMDocumentFactory.load(originalFile)
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

            val newPomFile = POMDocumentFactory.load(newPath.toFile())

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

        return ProjectModelFactory.loadFor(
            pomFile = pomFile,
            parentPomFiles = parentPomFiles
        )
    }

    private fun fixPomRelativePath(text: String?): String {
        if (null == text)
            return ""

        val name = File(text).name

        if (-1 == name.indexOf(".")) {
            return "$text/pom.xml"
        }

        return text
    }

    private fun isRelative(path: String): Boolean {
        if (path.matches(RE_WINDOWS_PATH)) {
            return false
        }

        return !(path.startsWith("/") || path.startsWith("~"))
    }


    private fun getParentPoms(originalFile: File): List<File> {
        val embedderFacadeResponse =
            EmbedderFacade.invokeEmbedder(
                EmbedderFacadeRequest(offline = true, pomFile = originalFile)
            )

        val res = embedderFacadeResponse.modelBuildingResult

        val rawModels = res.modelIds.map { res.getRawModel(it) }.toList()

        val parentPoms: List<File> =
            if (rawModels.size > 1) {
                rawModels.subList(1, rawModels.size).mapNotNull { it.pomFile }.toList()
            } else
                emptyList()
        return parentPoms
    }
}
