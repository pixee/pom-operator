package io.github.pixee.it;

import fun.mike.dmp.DiffMatchPatch;
import fun.mike.dmp.Patch;
import io.github.pixee.maven.operator.POMOperator;
import io.github.pixee.maven.operator.ProjectModel;
import io.github.pixee.maven.operator.ProjectModelFactory;
import kotlin.Unit;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import static org.junit.Assert.assertFalse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedList;

public class AbstractTestBase {
    protected final Logger LOGGER = LoggerFactory.getLogger(POMOperatorTest.class);

    private URL getResource(final String name){
        return this.getClass().getResource(name);
    }

    private File getResourceAsFile(final String name) throws URISyntaxException {
        return new File(getResource(name).toURI());
    }

    protected void gwt(final GivenProjectModel givenProjectModel, final ThenUnit thenUnit) {
        final ProjectModel context = givenProjectModel.g();

        LOGGER.debug("context: {}", context);

        POMOperator.modify(context);

        LOGGER.debug("context after: {}", context);

        thenUnit.t(context);
    }

    protected ProjectModel gwt(final String name, final ProjectModelFactory pmf) throws DocumentException, IOException {
        return gwt(name, pmf.build());
    }

    protected ProjectModel gwt(final String testName, final ProjectModel context) throws DocumentException, IOException {
        System.out.println("HOLAAAAA");
        final String resultFile = "pom-"+testName+"-result.xml";
        final URL resource = this.getClass().getResource(resultFile);

        if (resource != null) {
            final Document outcome = new SAXReader().read(resource);

            LOGGER.debug("context: {}", context);

            POMOperator.modify(context);

            LOGGER.debug("context after: {}", context);

            assertFalse(
                    "Expected and outcome have differences",
                    getXmlDifferences(context.getPomFile().getResultPom(), outcome).hasDifferences()
            );
        } else {
            final String resultFilePath = "src/test/resources/" + this.getClass().getPackage().getName().replace(
                    ".",
                    "/"
            ) + "/" + resultFile;

            LOGGER.debug("context: {}", context);

            POMOperator.modify(context);

            LOGGER.debug("context after: {}", context);

            LOGGER.warn("File $resultFilePath not found - writing results instead and ignorning assertions at all");

            final File file = new File(resultFilePath);

            final FileOutputStream fileOutputStream = new FileOutputStream(file);

            fileOutputStream.write(context.getPomFile().getResultPomBytes());

            fileOutputStream.close();
        }

        return context;
    }

    protected Diff getXmlDifferences(
            final Document original,
            final Document modified
    ) {
        final Source originalDoc = Input.fromString(original.asXML()).build();
        final Source modifiedDoc = Input.fromString(modified.asXML()).build();

        final Diff diff = DiffBuilder.compare(originalDoc).withTest(modifiedDoc).ignoreWhitespace()
                .checkForSimilar().build();

        LOGGER.debug("diff: {}", diff);

        return diff;
    }

    protected String getTextDifferences(final Document pomDocument, final Document resultPom) throws UnsupportedEncodingException {
        final String pomDocumentAsString = pomDocument.asXML();
        final String resultPomAsString = resultPom.asXML();

        final DiffMatchPatch dmp = new DiffMatchPatch();

        final LinkedList<Patch> diffs = dmp.patch_make(pomDocumentAsString, resultPomAsString);

        final String patch = dmp.patch_toText(diffs);

        return URLDecoder.decode(patch, "utf-8");
    }

    @FunctionalInterface
    public interface GivenProjectModel {
        ProjectModel g();
    }

    @FunctionalInterface
    public interface ThenUnit {
        Unit t(ProjectModel p);
    }
}
