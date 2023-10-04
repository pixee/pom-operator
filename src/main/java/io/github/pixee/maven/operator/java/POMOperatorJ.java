package io.github.pixee.maven.operator.java;

import com.github.zafarkhaja.semver.Version;
import io.github.pixee.maven.operator.Chain;
import io.github.pixee.maven.operator.Dependency;
import io.github.pixee.maven.operator.java.*;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

/**
 * Facade for the POM Operator
 */
public class POMOperatorJ {

    /**
     * Bump a Dependency Version on a POM.
     *
     * @param projectModel Project Model (Context) class
     */
    public static boolean modify(ProjectModelJ projectModel) {
        return Chain.Companion.createForModify().execute(projectModel);
    }

    /**
     * Public API - Query for all the artifacts referenced inside a POM File.
     *
     * @param projectModel Project Model (Context) Class
     */
    public static Collection<Dependency> queryDependency(ProjectModelJ projectModel) {
        return queryDependency(projectModel, Collections.emptyList());
    }

    /**
     * Public API - Query for all the versions mentioned inside a POM File.
     *
     * @param projectModel Project Model (Context) Class
     */
    public static Optional<VersionQueryResponseJ> queryVersions(ProjectModelJ projectModel) {
        Set<VersionDefinitionJ> queryVersionResult = queryVersions(projectModel, Collections.emptyList());

        if (queryVersionResult.size() == 2) {
            if (queryVersionResult.stream().anyMatch(it -> it.getKind() == KindJ.RELEASE)) {
                throw new IllegalStateException("Unexpected queryVersionResult Combination: " + queryVersionResult);
            }

            VersionDefinitionJ queryVersionSource = queryVersionResult.stream()
                    .filter(it -> it.getKind() == KindJ.SOURCE)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing source version"));

            VersionDefinitionJ queryVersionTarget = queryVersionResult.stream()
                    .filter(it -> it.getKind() == KindJ.TARGET)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing target version"));

            Version mappedSourceVersion = mapVersion(queryVersionSource.getValue());
            Version mappedTargetVersion = mapVersion(queryVersionTarget.getValue());

            return Optional.of(new VersionQueryResponseJ(mappedSourceVersion, mappedTargetVersion));
        }

        if (queryVersionResult.size() == 1) {
            List<VersionDefinitionJ> queryVersionResultList = CollectionUtils.isNotEmpty(queryVersionResult) ? queryVersionResult.stream().toList() : Collections.emptyList();
            Version mappedVersion = mapVersion(queryVersionResultList.get(0).getValue());

            VersionQueryResponseJ returnValue = new VersionQueryResponseJ(mappedVersion, mappedVersion);

            return Optional.of(returnValue);
        }

        return Optional.empty();
    }


    /**
     * Given a version string, formats and returns it as a semantic version object.
     *
     * Versions starting with "1." are appended with ".0".
     *
     * Other versions are appended with ".0.0".
     *
     * @return mapped version
     */
    public static Version mapVersion(String version) {
        String fixedVersion = version + (version.startsWith("1.") ? ".0" : ".0.0");
        return Version.valueOf(fixedVersion);
    }

    /**
     * Internal Use (package-wide) - Query for dependencies mentioned on a POM.
     *
     * @param projectModel Project Model (Context) class
     * @param commandList do not use (required for tests)
     */
    public static Collection<Dependency> queryDependency(ProjectModelJ projectModel, List<CommandJ> commandList) {
        Chain chain = Chain.Companion.createForDependencyQuery(projectModel.getQueryType());

        executeChain(commandList, chain, projectModel);

        AbstractQueryCommandJ lastCommand = null;
        for (int i = chain.getCommandList().size() - 1; i >= 0; i--) {
            if (chain.getCommandList().get(i) instanceof AbstractQueryCommandJ) {
                lastCommand = (AbstractQueryCommandJ) chain.getCommandList().get(i);
                if (lastCommand.getResult() != null) {
                    break;
                }
            }
        }

        if (lastCommand == null) {
            return Collections.emptyList();
        }

        return lastCommand.getResult();
    }

    /**
     * Internal Use (package-wide) - Query for versions mentioned on a POM.
     *
     * @param projectModel Project Model (Context) class
     * @param commandList do not use (required for tests)
     */
    public static Set<VersionDefinitionJ> queryVersions(ProjectModelJ projectModel, List<CommandJ> commandList) {
        Chain chain = Chain.Companion.createForVersionQuery(projectModel.getQueryType());

        executeChain(commandList, chain, projectModel);

        AbstractVersionCommandJ lastCommand = null;
        for (int i = chain.getCommandList().size() - 1; i >= 0; i--) {
            if (chain.getCommandList().get(i) instanceof AbstractVersionCommandJ) {
                lastCommand = (AbstractVersionCommandJ) chain.getCommandList().get(i);
                if (lastCommand.result != null && !lastCommand.result.isEmpty()) {
                    break;
                }
            }
        }

        if (lastCommand == null) {
            return Collections.emptySet();
        }

        return lastCommand.result;
    }


    private static void executeChain(List<CommandJ> commandList, Chain chain, ProjectModelJ projectModel) {
        if (!commandList.isEmpty()) {
            chain.getCommandList().clear();
            chain.getCommandList().addAll(commandList);
        }

        chain.execute(projectModel);
    }


}
