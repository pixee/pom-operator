package io.github.pixee.maven.operator.java;

import io.github.pixee.maven.operator.POMDocument;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public class POMDocumentFactoryJ {

    public static POMDocument load(InputStream is) throws IOException, DocumentException {

            byte[] originalPom = IOUtils.toByteArray(is);

            SAXReader reader = new SAXReader();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(originalPom);
            Document pomDocument = reader.read(inputStream);

            byte[] byteArrayOf = {};

            return new POMDocument(originalPom, null, pomDocument, Charset.defaultCharset(), "\n", "  ", byteArrayOf, "", "");

    }

    public static POMDocument load(File f) throws IOException, DocumentException {

            URL fileUrl = f.toURI().toURL();
            return load(fileUrl);

    }

    public static POMDocument load(URL url) throws IOException, DocumentException {
        InputStream inputStream = url.openStream();
            byte[] originalPom = IOUtils.toByteArray(inputStream);

            SAXReader saxReader = new SAXReader();
            Document pomDocument = saxReader.read(new ByteArrayInputStream(originalPom));

            byte[] byteArrayOf = {};

            return new POMDocument(originalPom, url, pomDocument, Charset.defaultCharset(), "\n", "  ", byteArrayOf, "", "");

    }

}
