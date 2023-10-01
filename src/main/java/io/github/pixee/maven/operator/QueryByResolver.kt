package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.DependencyJ
import io.github.pixee.maven.operator.java.EmbedderFacadeJ
import io.github.pixee.maven.operator.java.IgnorableJ
import org.apache.maven.model.building.ModelBuildingException
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.collection.DependencyCollectionException
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.graph.DependencyVisitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * This is a resolver that actually embeds much of Maven Logic into that.
 *
 * Futurely TODO Support Third Party / User-Supplied Repositories (right now it only supports central)
 */
class QueryByResolver : AbstractQueryCommand() {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(QueryByResolver::class.java)
    }

    override fun extractDependencyTree(outputPath: File, pomFilePath: File, c: ProjectModel) {
        TODO("Not yet implemented")
    }

    override fun execute(pm: ProjectModel): Boolean {
        val req = EmbedderFacadeJ.EmbedderFacadeRequest(
            pm.offline,
            pm.repositoryPath,
            pm.pomFile.file,
            pm.activeProfiles.filterNot { it.startsWith("!") }.toList(),
            pm.activeProfiles.filter { it.startsWith("!") }
                .map { it.substring(1) }.toList()
        )

        this.result = emptyList()

        val embedderFacadeResponse: EmbedderFacadeJ.EmbedderFacadeResponse

        try {
            embedderFacadeResponse = EmbedderFacadeJ.invokeEmbedder(req)
        } catch (mbe: ModelBuildingException) {
            IgnorableJ.LOGGER.debug("mbe (you can ignore): ", mbe)

            return false
        }

        val res = embedderFacadeResponse.modelBuildingResult

        val dependencyToArtifact: (org.apache.maven.model.Dependency) -> org.eclipse.aether.graph.Dependency =
            {
                org.eclipse.aether.graph.Dependency(
                    DefaultArtifact(
                        it.groupId,
                        it.artifactId,
                        it.classifier,
                        null,
                        it.version
                    ),
                    it.scope,
                )
            }

        val deps: List<org.eclipse.aether.graph.Dependency> =
            res.effectiveModel.dependencies?.map(dependencyToArtifact)?.toList() ?: emptyList()

        val managedDeps: List<org.eclipse.aether.graph.Dependency> =
            res.effectiveModel.dependencyManagement?.dependencies?.map(dependencyToArtifact)
                ?.toList() ?: emptyList()

        val collectRequest =
            CollectRequest(deps, managedDeps, embedderFacadeResponse.remoteRepositories)

        return try {
            val collectResult = embedderFacadeResponse.repositorySystem!!.collectDependencies(
                embedderFacadeResponse.session,
                collectRequest
            )

            val returnList: MutableList<DependencyJ> = mutableListOf()

            collectResult.root.accept(object : DependencyVisitor {
                override fun visitEnter(node: DependencyNode?): Boolean {
                    node?.dependency?.apply {
                        returnList.add(
                            DependencyJ(
                                this.artifact.groupId,
                                this.artifact.artifactId,
                                this.artifact.version,
                                this.artifact.classifier,
                                this.artifact.extension,
                                this.scope,
                            )
                        )
                    }

                    return true
                }

                override fun visitLeave(node: DependencyNode?): Boolean {
                    return true
                }
            })

            this.result = returnList.toList()

            return true
        } catch (e: DependencyCollectionException) {
            LOGGER.warn("while resolving: ", e)

            return false
        }
    }
}