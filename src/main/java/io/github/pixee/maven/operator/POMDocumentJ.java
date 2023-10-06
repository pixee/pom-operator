package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.kotlin.POMDocument;
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
public class POMDocumentJ {

    private byte[] originalPom;
    private URL pomPath;
    private Charset charset;
    private String endl;
    private String indent;
    private byte[] resultPomBytes;
    private String preamble;
    private String suffix;
    private boolean dirty;
    private POMDocument pomDocument;

    public POMDocumentJ(byte[] originalPom, URL pomPath, Document pomDocument) {
        this.pomDocument = new POMDocument(originalPom, pomPath, pomDocument, Charset.defaultCharset(), "\n", "  ", new byte[0], "", "");

        this.originalPom = originalPom;
        this.pomPath = pomPath;
        this.charset = Charset.defaultCharset();
        this.endl = "\n";
        this.indent = "  ";
        this.resultPomBytes = new byte[0];
        this.preamble = "";
        this.suffix = "";
        this.dirty = false;
    }


    public POMDocumentJ(byte[] originalPom, Document pomDocument) {

        this(originalPom, null, pomDocument);
    }

    public File getFile() throws URISyntaxException {
        return pomDocument.getFile$pom_operator();
    }

    public Document getResultPom() {
        return pomDocument.getResultPom();
    }

    @Override
    public String toString() {
        return (pomPath == null) ? "missing" : "[POMDocument @ " + pomPath.toString() + "]";
    }

    public byte[] getOriginalPom() {
        return this.originalPom;
    }

    public URL getPomPath() {
        return this.getPomPath();
    }

    public Document getPomDocument() {
        return pomDocument.getPomDocument();
    }

    public Charset getCharset() {
        return this.charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public String getEndl() {
        return this.endl;
    }

    public void setEndl(String endl) {
        this.endl = endl;
    }

    public String getIndent() {
        return this.indent;
    }

    public void setIndent(String indent) {
        this.indent = indent;
    }

    public byte[] getResultPomBytes() {
        return this.resultPomBytes;
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
        return this.suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }


    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }


    public boolean getDirty(){
        return this.dirty;
    }
}

