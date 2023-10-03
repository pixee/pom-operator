package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.Pair

/**
 * Implements a Chain of Responsibility Pattern
 *
 * @constructor commands: Commands to Use
 */
class Chain(vararg commands: CommandJ) {
    /**
     * Internal ArrayList of the Commands
     */
    internal val commandList: MutableList<CommandJ> = ArrayList(commands.toList())

    /**
     * Executes the Commands in the Chain of Responsibility
     *
     * @param c ProjectModel (context)
     * @return Boolean if successful
     */
    fun execute(c: ProjectModelJ): Boolean {
        var done = false
        val listIterator = commandList.listIterator()

        while ((!done) && listIterator.hasNext()) {
            val nextCommand = listIterator.next()

            done = nextCommand.execute(c)

            if (done) {
                if (c.queryType == QueryTypeJ.NONE && (nextCommand !is SupportCommandJ)) {
                    c.modifiedByCommand = true
                }

                c.finishedByClass = nextCommand.javaClass.name

                break
            }
        }

        val result = done

        /**
         * Goes Reverse Order applying the filter pattern
         */

        while (listIterator.previousIndex() > 0) {
            val nextCommand = listIterator.previous()

            done = nextCommand.postProcess(c)

            if (done)
                break
        }

        return result
    }


    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(Chain::class.java)

        /**
         * Returns a Pre-Configured Chain with the Defaults for Modifying a POM
         */
        fun createForModify() =
            Chain(
                CheckDependencyPresentJ.getInstance(),
                CheckParentPackagingJ.getInstance(),
                FormatCommand(),
                DiscardFormatCommandJ.getInstance(),
                CompositeDependencyManagementJ(),
                SimpleUpgradeJ.getInstance(),
                SimpleDependencyManagementJ.getInstance(),
                SimpleInsertJ()
            )

        private fun filterByQueryType(
            commandList: List<Pair<QueryTypeJ, String>>,
            queryType: QueryTypeJ,
            initialCommands: List<AbstractQueryCommandJ> = emptyList(),
            queryTypeFilter: ((queryType: QueryTypeJ) -> Boolean)
        ): Chain {
            val filteredCommands: List<CommandJ> = commandList
                .filter { queryTypeFilter(it.first) }.mapNotNull {
                    val commandClassName = "io.github.pixee.maven.operator.${it.second}"

                    try {
                        Class.forName(commandClassName).newInstance() as CommandJ
                    } catch (e: Throwable) {
                        LOGGER.warn("Creating class '{}': ", commandClassName, e)

                        null
                    }
                }
                .toList()

            val commands: List<CommandJ> = initialCommands + filteredCommands

            if (commands.isEmpty())
                throw IllegalStateException("Unable to load any available strategy for ${queryType.name}")

            return Chain(*commands.toTypedArray())
        }

        /**
         * Some classes won't have all available dependencies on the classpath during runtime
         * for this reason we'll use <pre>Class.forName</pre> and report issues creating
         */
        val AVAILABLE_DEPENDENCY_QUERY_COMMANDS = listOf<Pair<QueryTypeJ, String>>(
            QueryTypeJ.SAFE to "QueryByResolverJ",
            QueryTypeJ.SAFE to "QueryByParsingJ",
            QueryTypeJ.UNSAFE to "QueryByEmbedder",
            QueryTypeJ.UNSAFE to "QueryByInvokerJ",
        )

        /**
         * returns a pre-configured chain with the defaults for Dependency Querying
         */
        fun createForDependencyQuery(queryType: QueryTypeJ = QueryTypeJ.SAFE): Chain =
            filterByQueryType(
                AVAILABLE_DEPENDENCY_QUERY_COMMANDS,
                queryType,
                listOf(CheckLocalRepositoryDirCommandJ.CheckParentDirCommand.getInstance()),
                { it == queryType }
            )

        /**
         * List of Commands for Version Query
         */
        val AVAILABLE_QUERY_VERSION_COMMANDS = listOf<Pair<QueryTypeJ, String>>(
            QueryTypeJ.NONE to "UnwrapEffectivePomJ",
            QueryTypeJ.SAFE to "VersionByCompilerDefinitionJ",
            QueryTypeJ.SAFE to "VersionByPropertyJ",
        )

        /**
         * returns a pre-configured chain for Version Query
         */
        fun createForVersionQuery(queryType: QueryTypeJ = QueryTypeJ.SAFE): Chain =
            filterByQueryType(
                AVAILABLE_QUERY_VERSION_COMMANDS,
                queryType,
                emptyList(),
                { it.ordinal <= queryType.ordinal }
            )
    }
}