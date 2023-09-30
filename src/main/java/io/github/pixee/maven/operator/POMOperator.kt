package io.github.pixee.maven.operator

import com.github.zafarkhaja.semver.Version
import io.github.pixee.maven.operator.java.CommandJ
import io.github.pixee.maven.operator.java.VersionDefinitionJ
import io.github.pixee.maven.operator.java.VersionQueryResponseJ
import java.util.*


/**
 * Fa&ccedil;ade for the POM Operator
 */
object POMOperator {
    /**
     * Bump a Dependency Version on a POM
     *
     * @param projectModel Project Model (Context) class
     */
    @JvmStatic
    fun modify(projectModel: ProjectModel) = Chain.createForModify().execute(projectModel)

    /**
     * Public API - Query for all the artifacts referenced inside a POM File
     *
     * @param projectModel Project Model (Context) Class
     */
    @JvmStatic
    fun queryDependency(
        projectModel: ProjectModel
    ) = queryDependency(projectModel, emptyList())

    /**
     * Public API - Query for Java Versions
     *
     * It will return optionally a VersionQueryResponse
     *
     */
    @JvmStatic
    fun queryVersions(
        projectModel: ProjectModel
    ): Optional<VersionQueryResponseJ> {
        val queryVersionResult = queryVersions(projectModel, emptyList())

        /*
         * Likely Source / Target
         */
        if (queryVersionResult.size == 2) {
            /*
             * but if there's `release` we`ll throw an exception
             */
            if (queryVersionResult.any { it.kind == Kind.RELEASE })
                throw IllegalStateException("Unexpected queryVersionResult Combination: ${queryVersionResult}")

            val queryVersionSource = queryVersionResult.first { it.kind == Kind.SOURCE }!!
            val queryVersionTarget = queryVersionResult.first { it.kind == Kind.TARGET }!!

            val mappedSourceVersion = mapVersion(queryVersionSource.value)
            val mappedTargetVersion = mapVersion(queryVersionTarget.value)

            return Optional.of(VersionQueryResponseJ(mappedSourceVersion, mappedTargetVersion))
        }

        /**
         * Could be either source, target or release - we pick the value anyway
         */
        if (queryVersionResult.size == 1) {
            val mappedVersion = mapVersion(queryVersionResult.first().value)

            val returnValue = VersionQueryResponseJ(mappedVersion, mappedVersion)

            return Optional.of(returnValue)
        }

        return Optional.empty()
    }

    /**
     * Given a version string, formats and returns as a semantic version object
     *
     * Versions starting with `1.` are appended an `.0`
     *
     * Other versions are `.0.0`
     *
     * and returned as Version Objects
     *
     * @return mapped version
     */
    fun mapVersion(version: String): Version {
        val fixedVersion = version + if (version.startsWith("1.")) {
            ".0"
        } else {
            ".0.0"
        }

        return Version.valueOf(fixedVersion)

    }

    /**
     * Internal Use (package-wide) - Query for all the artifacts mentioned on a POM
     *
     * @param projectModel Project Model (Context) class
     * @param commandList do not use (required for tests)
     */
    @JvmStatic
    internal fun queryDependency(
        projectModel: ProjectModel,
        commandList: List<CommandJ>
    ): Collection<Dependency> {
        val chain = Chain.createForDependencyQuery(projectModel.queryType)

        executeChain(commandList, chain, projectModel)

        val lastCommand = chain.commandList.filterIsInstance<AbstractQueryCommand>()
            .lastOrNull { it.result != null }
            ?: return emptyList()

        return lastCommand.result!!
    }

    /**
     * Internal Use (package-wide) - Query for versions mentioned on a POM
     *
     * @param projectModel Project Model (Context) class
     * @param commandList do not use (required for tests)
     */
    @JvmStatic
    internal fun queryVersions(
        projectModel: ProjectModel,
        commandList: List<CommandJ>
    ): Set<VersionDefinitionJ> {
        val chain = Chain.createForVersionQuery(projectModel.queryType)

        executeChain(commandList, chain, projectModel)

        val lastCommand = chain.commandList.filterIsInstance<AbstractVersionCommand>()
            .lastOrNull { it.result != null && it.result.isNotEmpty() }
            ?: return emptySet()

        return lastCommand.result!!
    }

    private fun executeChain(
        commandList: List<CommandJ>,
        chain: Chain,
        projectModel: ProjectModel
    ) {
        if (commandList.isNotEmpty()) {
            chain.commandList.clear()
            chain.commandList.addAll(commandList)
        }

        chain.execute(projectModel)
    }
}
