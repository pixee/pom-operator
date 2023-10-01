package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.ProjectModelJ
import io.github.pixee.maven.operator.java.UtilJ.selectXPathNodes
import org.dom4j.Element
import java.io.File

/**
 * ProjectModel represents the input parameters for the chain
 *
 * @todo consider resolution and also Topological Sort of Properties for cross-property reference
 */
class ProjectModel internal constructor(
    val pomFile: POMDocument,

    val parentPomFiles: List<POMDocument> = emptyList(),

    var dependency: Dependency?,
    val skipIfNewer: Boolean,
    val useProperties: Boolean,
    val activeProfiles: Set<String>,
    val overrideIfAlreadyExists: Boolean,
    val queryType: QueryType = QueryType.NONE,

    val repositoryPath: File? = null,

    var finishedByClass: String? = null,

    val offline: Boolean = false,
) {
    internal var modifiedByCommand = false

    /**
     * Involved POM Files
     */
    val allPomFiles: List<POMDocument>
        get() = ProjectModelJ.getAllPomFiles(pomFile, parentPomFiles)

    val resolvedProperties =
        ProjectModelJ.resolvedProperties(pomFile, allPomFiles, activeProfiles)

    val propertiesDefinedByFile =
        ProjectModelJ.propertiesDefinedByFile(pomFile, allPomFiles, activeProfiles)

    private fun getPropertiesFromProfile(
        profileName: String,
        pomFile: POMDocument
    ): Map<String, String> {
        return ProjectModelJ.getPropertiesFromProfile(profileName, pomFile)
    }

    companion object {
        fun propertiesDefinedOnPomDocument(pomFile: POMDocument): Map<String, String> {
            return ProjectModelJ.propertiesDefinedOnPomDocument(pomFile)
        }
    }
}

