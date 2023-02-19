package io.openpixee.maven.operator

import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXWriter
import org.dom4j.io.XMLWriter
import java.lang.IllegalStateException
import java.nio.charset.Charset
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.StartDocument
import javax.xml.stream.events.StartElement

class FormatCommand : AbstractSimpleCommand() {
    private var charset: Charset = Charset.defaultCharset()

    private var preamble: String = ""

    override fun execute(c: ProjectModel): Boolean {
        val inputFactory = XMLInputFactory.newInstance()
        val eventReader = inputFactory.createXMLEventReader(c.originalPom.inputStream())

        while (true) {
            val event = eventReader.nextEvent()

            if (event.isStartDocument && (event as StartDocument).encodingSet()) {
                this.charset = Charset.forName((event as StartDocument).characterEncodingScheme)
            } else if (event.isStartElement) {
                val startElementEvent = (event as StartElement)

                var offset = startElementEvent.location.characterOffset

                preamble = c.originalPom.toString(this.charset).substring(0, offset)

                break
            }

            if (! eventReader.hasNext())
                throw IllegalStateException("Couldn't find document start")
        }

        return super.execute(c)
    }

    override fun postProcess(c: ProjectModel): Boolean {
        val writer = SAXWriter()

        var xmlRepresentation = c.resultPom.asXML()

        /**
         * We might need to replace the beginning of the POM with the same content
         * from the very beginning
         */
        val inputFactory = XMLInputFactory.newInstance()
        val eventReader = inputFactory.createXMLEventReader(xmlRepresentation.toByteArray(this.charset).inputStream())

        while (true) {
            val event = eventReader.nextEvent()

            if (event.isStartElement) {
                val startElementEvent = (event as StartElement)

                var offset = startElementEvent.location.characterOffset

                xmlRepresentation = this.preamble + xmlRepresentation.substring(offset)

                break
            }

            if (! eventReader.hasNext())
                throw IllegalStateException("Couldn't find document start")
        }

        c.resultPomBytes = xmlRepresentation.toByteArray(this.charset)

        return super.postProcess(c)
    }
}