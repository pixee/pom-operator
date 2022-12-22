package io.openpixee.maven.operator


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
     * Query for all the artifacts mentioned on a POM
     *
     * @param projectModel Project Model (Context) class
     */
    @JvmStatic
    fun queryDependency(projectModel: ProjectModel): Collection<Dependency> {
        val chain = Chain.createForQuery(projectModel.queryType)

        chain.execute(projectModel)

        val lastCommand = chain.commandList.filterIsInstance<AbstractSimpleQueryCommand>()
            .filter { it.result != null }
            .lastOrNull()
            ?: return emptyList()

        return lastCommand.result!!
    }
}
