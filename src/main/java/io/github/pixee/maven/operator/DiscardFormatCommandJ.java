package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.kotlin.POMDocument;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;

/**
 * Command Class to Short-Circuit/Discard Processing when no pom changes were made
 */
public class DiscardFormatCommandJ extends AbstractCommandJ {

    private static DiscardFormatCommandJ instance;

    private DiscardFormatCommandJ() {
        // Private constructor to prevent instantiation.
    }

    public static DiscardFormatCommandJ getInstance() {
        if (instance == null) {
            instance = new DiscardFormatCommandJ();
        }
        return instance;
    }

    @Override
    public boolean postProcess(ProjectModelJ pm) {
        boolean mustSkip = false;

        for (POMDocument pomFile : pm.allPomFiles()) {
            Source originalDoc = Input.fromString(new String(pomFile.getOriginalPom())).build();
            Source modifiedDoc = Input.fromString(pomFile.getResultPom().asXML()).build();

            Diff diff = DiffBuilder.compare(originalDoc).withTest(modifiedDoc)
                    .ignoreWhitespace()
                    .ignoreComments()
                    .ignoreElementContentWhitespace()
                    .checkForSimilar()
                    .build();

            boolean hasDifferences = diff.hasDifferences();

            if (!(pm.getModifiedByCommand() || hasDifferences)) {
                pomFile.setResultPomBytes(pomFile.getOriginalPom());
                mustSkip = true;
            }
        }

        /**
         * Triggers early abandonment
         */
        if (mustSkip) {
            return true;
        }

        return super.postProcess(pm);
    }
}

