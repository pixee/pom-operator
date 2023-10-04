package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.kotlin.FormatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

public class ChainJ {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChainJ.class);

    /**
     * Internal ArrayList of the Commands
     */
    private List<CommandJ> commandList;

    // Constructor that takes an array of CommandJ
    public ChainJ(CommandJ... commands) {
        this.commandList = new ArrayList<>(Arrays.asList(commands));
    }

    public List<CommandJ> getCommandList() {
        return commandList;
    }

    public boolean execute(ProjectModelJ c) throws URISyntaxException, IOException {
        boolean done = false;
        ListIterator<CommandJ> listIterator = commandList.listIterator();

        while (!done && listIterator.hasNext()) {
            CommandJ nextCommand = listIterator.next();
            done = nextCommand.execute(c);

            if (done) {
                if (c.getQueryType() == QueryTypeJ.NONE && !(nextCommand instanceof SupportCommandJ)) {
                    c.setModifiedByCommand(true);
                }

                c.setFinishedByClass(nextCommand.getClass().getName());

                break;
            }
        }

        boolean result = done;

        /**
         * Goes Reverse Order applying the filter pattern
         */

        while (listIterator.previousIndex() > 0) {
            CommandJ nextCommand = listIterator.previous();
            done = nextCommand.postProcess(c);

            if (done) {
                break;
            }
        }

        return result;
    }

    public static ChainJ createForModify() {
        return new ChainJ(
                CheckDependencyPresentJ.getInstance(),
                CheckParentPackagingJ.getInstance(),
                new FormatCommand(),
                DiscardFormatCommandJ.getInstance(),
                new CompositeDependencyManagementJ(),
                SimpleUpgradeJ.getInstance(),
                SimpleDependencyManagementJ.getInstance(),
                new SimpleInsertJ()
        );
    }

    public static ChainJ filterByQueryType(
            List<Pair<QueryTypeJ, String>> commandList,
            QueryTypeJ queryType,
            List<AbstractQueryCommandJ> initialCommands,
            QueryTypeFilter queryTypeFilter) {
        List<CommandJ> filteredCommands = new ArrayList<>();
        for (Pair<QueryTypeJ, String> pair : commandList) {
            if (queryTypeFilter.filter(pair.getFirst())) {
                String commandClassName = "io.github.pixee.maven.operator." + pair.getSecond();

                try {
                    Class<?> commandClass = Class.forName(commandClassName);
                    CommandJ command = (CommandJ) commandClass.newInstance();
                    filteredCommands.add(command);
                } catch (Throwable e) {
                    LOGGER.warn("Creating class '{}': ", commandClassName, e);
                }
            }
        }

        List<CommandJ> commands = new ArrayList<>();
        commands.addAll(initialCommands);
        commands.addAll(filteredCommands);

        if (commands.isEmpty()) {
            throw new IllegalStateException("Unable to load any available strategy for " + queryType.name());
        }

        return new ChainJ(commands.toArray(new CommandJ[0]));
    }

    public static ChainJ createForDependencyQuery(QueryTypeJ queryType) {
        return filterByQueryType(
                AVAILABLE_DEPENDENCY_QUERY_COMMANDS,
                queryType,
                List.of(CheckLocalRepositoryDirCommandJ.CheckParentDirCommand.getInstance()),
                it -> it == queryType
        );
    }

    public static ChainJ createForVersionQuery(QueryTypeJ queryType) {
        return filterByQueryType(
                AVAILABLE_QUERY_VERSION_COMMANDS,
                queryType,
                Collections.emptyList(),
                it -> it.ordinal() <= queryType.ordinal()
        );
    }

    public static final List<Pair<QueryTypeJ, String>> AVAILABLE_DEPENDENCY_QUERY_COMMANDS =
            new ArrayList<>(Arrays.asList(
                    new Pair<>(QueryTypeJ.SAFE, "QueryByResolverJ"),
                    new Pair<>(QueryTypeJ.SAFE, "QueryByParsingJ"),
                    new Pair<>(QueryTypeJ.UNSAFE, "QueryByEmbedderJ"),
                    new Pair<>(QueryTypeJ.UNSAFE, "QueryByInvokerJ")
            ));

    public static final List<Pair<QueryTypeJ, String>> AVAILABLE_QUERY_VERSION_COMMANDS =
            new ArrayList<>(Arrays.asList(
                    new Pair<>(QueryTypeJ.NONE, "UnwrapEffectivePomJ"),
                    new Pair<>(QueryTypeJ.SAFE, "VersionByCompilerDefinitionJ"),
                    new Pair<>(QueryTypeJ.SAFE, "VersionByPropertyJ")
            ));

    public interface QueryTypeFilter {
        boolean filter(QueryTypeJ queryType);
    }
}

