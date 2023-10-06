package io.github.pixee.maven.operator.kotlin

import org.dom4j.Document
import java.io.File
import java.net.URL
import java.nio.charset.Charset

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
@Suppress("ArrayInDataClass")
data class POMDocument(
    val pomPath: URL?,
    val pomDocument: Document,
) {
    internal val file: File get() = File(this.pomPath!!.toURI())

    val resultPom: Document = pomDocument.clone() as Document

}

