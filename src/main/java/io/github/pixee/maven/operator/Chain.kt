package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.CommandJ
import io.github.pixee.maven.operator.java.SupportCommandJ
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
    fun execute(c: ProjectModel): Boolean {
        var done = false
        val listIterator = commandList.listIterator()

        while ((!done) && listIterator.hasNext()) {
            val nextCommand = listIterator.next()

            done = nextCommand.execute(c)

            if (done) {
                if (c.queryType == QueryType.NONE && (nextCommand !is SupportCommandJ)) {
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
                CheckDependencyPresent,
                CheckParentPackaging,
                FormatCommand(),
                DiscardFormatCommand(),
                CompositeDependencyManagement(),
                SimpleUpgrade,
                SimpleDependencyManagement,
                SimpleInsert
            )

        private fun filterByQueryType(
            commandList: List<Pair<QueryType, String>>,
            queryType: QueryType,
            initialCommands: List<AbstractQueryCommand> = emptyList(),
            queryTypeFilter: ((queryType: QueryType) -> Boolean)
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
        val AVAILABLE_DEPENDENCY_QUERY_COMMANDS = listOf<Pair<QueryType, String>>(
            QueryType.SAFE to "QueryByResolver",
            QueryType.SAFE to "QueryByParsing",
            QueryType.UNSAFE to "QueryByEmbedder",
            QueryType.UNSAFE to "QueryByInvoker",
        )

        /**
         * returns a pre-configured chain with the defaults for Dependency Querying
         */
        fun createForDependencyQuery(queryType: QueryType = QueryType.SAFE): Chain =
            filterByQueryType(
                AVAILABLE_DEPENDENCY_QUERY_COMMANDS,
                queryType,
                listOf(CHECK_PARENT_DIR_COMMAND),
                { it == queryType }
            )

        /**
         * List of Commands for Version Query
         */
        val AVAILABLE_QUERY_VERSION_COMMANDS = listOf<Pair<QueryType, String>>(
            QueryType.NONE to "UnwrapEffectivePom",
            QueryType.SAFE to "VersionByCompilerDefinitionJ",
            QueryType.SAFE to "VersionByPropertyJ",
        )

        /**
         * returns a pre-configured chain for Version Query
         */
        fun createForVersionQuery(queryType: QueryType = QueryType.SAFE): Chain =
            filterByQueryType(
                AVAILABLE_QUERY_VERSION_COMMANDS,
                queryType,
                emptyList(),
                { it.ordinal <= queryType.ordinal }
            )
    }
}