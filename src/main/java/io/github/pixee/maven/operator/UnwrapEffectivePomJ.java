package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.java.*;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingResult;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
public class UnwrapEffectivePomJ extends AbstractVersionCommandJ{

    private static final Logger LOGGER = LoggerFactory.getLogger(UnwrapEffectivePomJ.class);

    public boolean execute(ProjectModelJ pm) {
        try {
            return executeInternal(pm);
        } catch (Exception e) {
            if (e instanceof ModelBuildingException) {
                IgnorableJ.LOGGER.debug("mbe (you can ignore): ", e);
            } else {
                LOGGER.warn("While trying embedder: ", e);
            }
            return false;
        }
    }

    private boolean executeInternal(ProjectModelJ pm) throws ModelBuildingException {
        EmbedderFacadeJ.EmbedderFacadeRequest request = new EmbedderFacadeJ.EmbedderFacadeRequest(
                pm.getOffline(),
                null,
                pm.getPomFile().getFile$pom_operator(),
                null,
                null
        );

        EmbedderFacadeJ.EmbedderFacadeResponse embedderFacadeResponse = EmbedderFacadeJ.invokeEmbedder(request);

        Set<VersionDefinitionJ> definedVersions = new TreeSet<>(AbstractVersionCommandJ.VERSION_KIND_COMPARATOR);

        ModelBuildingResult res = embedderFacadeResponse.getModelBuildingResult();

        List<Xpp3Dom> pluginConfigurations = new ArrayList<>();

        for (Plugin plugin : res.getEffectiveModel().getBuild().getPluginManagement().getPlugins()) {
            if ("maven-compiler-plugin".equals(plugin.getArtifactId())) {
                Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
                if (configuration != null) {
                    pluginConfigurations.add(configuration);
                }
            }
        }

        for (Plugin plugin : res.getEffectiveModel().getBuild().getPlugins()) {
            if ("maven-compiler-plugin".equals(plugin.getArtifactId())) {
                Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
                if (configuration != null) {
                    pluginConfigurations.add(configuration);
                }
            }
        }

        for (Xpp3Dom config : pluginConfigurations) {
            for (Map.Entry<String, KindJ> entry : AbstractVersionCommandJ.TYPE_TO_KIND.entrySet()) {
                Xpp3Dom child = config.getChild(entry.getKey());

                if (child != null) {
                    definedVersions.add(new VersionDefinitionJ(entry.getValue(), child.getValue()));
                }
            }
        }

        List<VersionDefinitionJ> definedProperties = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : res.getEffectiveModel().getProperties().entrySet()) {
            if (AbstractVersionCommandJ.PROPERTY_TO_KIND.containsKey(entry.getKey())) {
                KindJ kind = AbstractVersionCommandJ.PROPERTY_TO_KIND.get(entry.getKey());

                definedProperties.add(new VersionDefinitionJ(kind, (String) entry.getValue()));
            }
        }

        definedVersions.addAll(definedProperties);

        result.addAll(definedVersions);

        return !definedVersions.isEmpty();
    }

}
