package io.github.pixee.maven.operator


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
     * It will return a set of type VersionDescription with either zero, one or two elements
     *
     * If zero - no versions were detected. Defaults to original JDK Version (I'd say most likely between 1.4 and 1.8)
     * If one - most likely the -release flag (java 9 onwards). Something such as new VersionDescription(Kind.RELEASE, "9") will be set
     * if two - likely before 9. So there'll be two items: new VersionDescription(Kind.SOURCE, "1.8") and another one with kind set to Kind.TARGET
     *
     * @return set of VersionDescription
     */
    @JvmStatic
    fun queryVersions(
        projectModel: ProjectModel
    ) = queryVersions(projectModel, emptyList())

    /**
     * Internal Use (package-wide) - Query for all the artifacts mentioned on a POM
     *
     * @param projectModel Project Model (Context) class
     * @param commandList do not use (required for tests)
     */
    @JvmStatic
    internal fun queryDependency(
        projectModel: ProjectModel,
        commandList: List<Command>
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
        commandList: List<Command>
    ): Set<VersionDefinition> {
        val chain = Chain.createForVersionQuery(projectModel.queryType)

        executeChain(commandList, chain, projectModel)

        val lastCommand = chain.commandList.filterIsInstance<AbstractVersionCommand>()
            .lastOrNull { it.result != null && it.result.isNotEmpty() }
            ?: return emptySet()

        return lastCommand.result!!
    }

    private fun executeChain(
        commandList: List<Command>,
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
