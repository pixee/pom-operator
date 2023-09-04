package io.github.pixee.maven.operator

class UnwrapEffectivePom : AbstractVersionCommand() {
    override fun execute(pm: ProjectModel): Boolean =
        try {
            executeInternal(pm)
        } catch (e: Exception) {
            false
        }

    fun executeInternal(pm: ProjectModel): Boolean {
        val embedderFacadeResponse = EmbedderFacade.invokeEmbedder(
            EmbedderFacadeRequest(offline = pm.offline, pomFile = pm.pomFile.file)
        )

        val res = embedderFacadeResponse.modelBuildingResult

        listOf(
            res.effectiveModel.build.pluginManagement.plugins,
            res.effectiveModel.build.plugins,
        ).flatMap { it }.filter { p -> p.artifactId.equals("maven-compiler-plugin") }
                .map {
                    it.configuration
                }

        return false
    }
}