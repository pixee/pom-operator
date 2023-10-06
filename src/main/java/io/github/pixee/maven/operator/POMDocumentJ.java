package io.github.pixee.maven.operator;

import lombok.EqualsAndHashCode;

import org.dom4j.Document;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
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
        if (this.getPomPath() != null) {
            return new File(this.getPomPath().toURI());
        } else {
            throw new NullPointerException("pomPath is null");
        }
    }

    public Document getResultPom() {
        return (Document) pomDocument.clone();
    }

    public byte[] getOriginalPom() {
        return originalPom;
    }

    public URL getPomPath() {
        return pomPath;
    }

    public Document getPomDocument() {
        return pomDocument;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public String getEndl() {
        return endl;
    }

    public void setEndl(String endl) {
        this.endl = endl;
    }

    public String getIndent() {
        return indent;
    }

    public void setIndent(String indent) {
        this.indent = indent;
    }

    public byte[] getResultPomBytes() {
        return resultPomBytes;
    }

    public void setResultPomBytes(byte[] resultPomBytes) {
        this.resultPomBytes = resultPomBytes;
    }

    public String getPreamble() {
        return preamble;
    }

    public void setPreamble(String preamble) {
        this.preamble = preamble;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public boolean getDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public String toString() {
        return (pomPath == null) ? "missing" : "[POMDocument @ " + pomPath.toString() + "]";
    }
}

