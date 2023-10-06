package io.github.pixee.maven.operator;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

public class POMDocumentFactoryJ {

    public static POMDocumentJ load(InputStream is) throws IOException, DocumentException {

            byte[] originalPom = IOUtils.toByteArray(is);

            SAXReader reader = new SAXReader();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(originalPom);
            Document pomDocument = reader.read(inputStream);

            byte[] byteArrayOf = {};

            return new POMDocumentJ(originalPom, null, pomDocument);

    }

    public static POMDocumentJ load(File f) throws IOException, DocumentException {

            URL fileUrl = f.toURI().toURL();
            return load(fileUrl);

    }

    public static POMDocumentJ load(URL url) throws IOException, DocumentException {
        InputStream inputStream = url.openStream();
            byte[] originalPom = IOUtils.toByteArray(inputStream);

            SAXReader saxReader = new SAXReader();
            Document pomDocument = saxReader.read(new ByteArrayInputStream(originalPom));

            byte[] byteArrayOf = {};

            return new POMDocumentJ(originalPom, url, pomDocument);

    }

}
