package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.EmbedderFacadeJ;
import io.github.pixee.maven.operator.IgnorableJ;
import io.github.pixee.maven.operator.POMDocumentFactoryJ;
import io.github.pixee.maven.operator.ProjectModelFactoryJ;
import org.apache.maven.model.building.ModelBuildingException;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class POMScannerJ {

    private static List<File> getParentPoms(File originalFile) {
        EmbedderFacadeJ.EmbedderFacadeResponse embedderFacadeResponse = EmbedderFacadeJ.invokeEmbedder(
                new EmbedderFacadeJ.EmbedderFacadeRequest(true, null, originalFile, null, null)
        );

        EmbedderFacadeJ.ModelBuildingResult res = embedderFacadeResponse.getModelBuildingResult();

        List<EmbedderFacadeJ.RawModel> rawModels = new ArrayList<>();
        for (String modelId : res.getModelIds()) {
            rawModels.add(res.getRawModel(modelId));
        }

        List<File> parentPoms;
        if (rawModels.size() > 1) {
            parentPoms = new ArrayList<>();
            for (int i = 1; i < rawModels.size(); i++) {
                EmbedderFacadeJ.RawModel rawModel = rawModels.get(i);
                if (rawModel != null) {
                    File pomFile = rawModel.getPomFile();
                    if (pomFile != null) {
                        parentPoms.add(pomFile);
                    }
                }
            }
        } else {
            parentPoms = new ArrayList<>();
        }
        return parentPoms;
    }

}
