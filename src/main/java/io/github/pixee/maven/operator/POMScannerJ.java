package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.EmbedderFacadeJ;
import io.github.pixee.maven.operator.POMDocumentFactoryJ;
import io.github.pixee.maven.operator.ProjectModelFactoryJ;
import io.github.pixee.maven.operator.kotlin.POMDocument;
import io.github.pixee.maven.operator.kotlin.POMScanner;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingResult;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class POMScannerJ {

    private static final Pattern RE_WINDOWS_PATH = Pattern.compile("^[A-Za-z]:");

    private static final Logger LOGGER = LoggerFactory.getLogger(POMScannerJ.class);

    public static ProjectModelFactoryJ scanFrom(File originalFile, File topLevelDirectory) throws Exception {
        ProjectModelFactoryJ originalDocument = ProjectModelFactoryJ.load(originalFile);

        List<File> parentPoms;
        try {
            parentPoms = getParentPoms(originalFile);
        } catch (Exception e) {
            if (e instanceof ModelBuildingException) {
                IgnorableJ.LOGGER.debug("mbe (you can ignore): ", e);
            } else {
                LOGGER.warn("While trying embedder: ", e);
            }
            return POMScanner.legacyScanFrom(originalFile, topLevelDirectory);
        }

        try {
            List<POMDocument> parentPomDocuments = parentPoms.stream()
                    .map(file -> {
                        try {
                            return POMDocumentFactoryJ.load(file);
                        } catch (IOException | DocumentException e) {

                            return null; // Handle appropriately in your code
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return originalDocument.withParentPomFiles(parentPomDocuments);
        } catch (Exception e) {

            return originalDocument; // Return the original document or handle appropriately
        }

    }

    public static String fixPomRelativePath(String text) {
        if (text == null) {
            return "";
        }

        String name = new File(text).getName();

        if (name.indexOf('.') == -1) {
            return text + "/pom.xml";
        }

        return text;
    }

    public static boolean isRelative(String path) {
        if (RE_WINDOWS_PATH.matcher(path).matches()) {
            return false;
        }

        return !(path.startsWith("/") || path.startsWith("~"));
    }

    public static List<File> getParentPoms(File originalFile) throws ModelBuildingException {
        EmbedderFacadeJ.EmbedderFacadeResponse embedderFacadeResponse =
                EmbedderFacadeJ.invokeEmbedder(
                        new EmbedderFacadeJ.EmbedderFacadeRequest(true, null, originalFile, null, null)
                );

        ModelBuildingResult res = embedderFacadeResponse.getModelBuildingResult();

        List<Model> rawModels = new ArrayList<>();
        for (String modelId : res.getModelIds()) {
            Model rawModel = res.getRawModel(modelId);
            if (rawModel != null) {
                rawModels.add(rawModel);
            }
        }

        List<File> parentPoms = new ArrayList<>();
        if (rawModels.size() > 1) {
            for (int i = 1; i < rawModels.size(); i++) {
                Model rawModel = rawModels.get(i);
                if (rawModel != null) {
                    File pomFile = rawModel.getPomFile();
                    if (pomFile != null) {
                        parentPoms.add(pomFile);
                    }
                }
            }
        }

        return parentPoms;
    }

}
