package io.openpixee.maven.operator


/**
 * Fa&ccedil;ade for the POM Upgrader
 */
object POMOperator {
    @JvmStatic
    fun modify(projectModel: ProjectModel) = Chain.createForModify().execute(projectModel)


    @JvmStatic
    fun queryDependency(projectModel: ProjectModel) = Chain.createForQuery().execute(projectModel)

}
