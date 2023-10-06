package io.github.pixee.maven.operator;

import lombok.EqualsAndHashCode;

import lombok.Getter;
import lombok.Setter;
import org.dom4j.Document;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Data Class to Keep track of an entire POM File, including:
 *
 * Path (pomPath)
 *
 * DOM Contents (pomDocument) - original
 * DOM Contents (resultPom) - modified
 *
 * Charset (ditto)
 * Indent (ditto)
 * Preamble (ditto)
 * Suffix (ditto)
 * Line Endings (endl)
 *
 * Original Content (originalPom)
 * Modified Content (resultPomBytes)
 */
@EqualsAndHashCode
@Getter
@Setter
public class POMDocumentJ {
    private final byte[] originalPom;
    private final URL pomPath;
    private final Document pomDocument;
    private Charset charset = Charset.defaultCharset();
    private String endl = "\n";
    private String indent = "  ";
    private byte[] resultPomBytes = new byte[0];
    private String preamble = "";
    private String suffix = "";
    private boolean dirty = false;

    public POMDocumentJ(byte[] originalPom, URL pomPath, Document pomDocument) {
        this.originalPom = originalPom;
        this.pomPath = pomPath;
        this.pomDocument = pomDocument;
    }


    public POMDocumentJ(byte[] originalPom, Document pomDocument) {
        this(originalPom, null, pomDocument);
    }

    public File getFile() throws URISyntaxException {
        return new File(this.getPomPath().toURI());
    }

    public Document getResultPom() {
        return (Document) pomDocument.clone();
    }

    public boolean getDirty(){
        return dirty;
    }

    @Override
    public String toString() {
        return (pomPath == null) ? "missing" : "[POMDocument @ " + pomPath.toString() + "]";
    }
}

