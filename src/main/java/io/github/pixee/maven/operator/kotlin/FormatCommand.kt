package io.github.pixee.maven.operator.kotlin

import io.github.pixee.maven.operator.AbstractCommandJ
import io.github.pixee.maven.operator.FormatCommandJ
import io.github.pixee.maven.operator.MatchDataJ
import io.github.pixee.maven.operator.ProjectModelJ
import org.apache.commons.lang3.StringUtils
import org.mozilla.universalchardet.UniversalDetector
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.events.*

/**
 * Data Class used to keep track of matches (ranges, content, referring tag name)
 */
data class MatchData(
    val range: IntRange,
    val content: String,
    val elementName: String,
    val hasAttributes: Boolean,
    val modifiedContent: Regex? = null
)

/**
 * This Command handles Formatting - particularly storing the original document preamble (the Processing Instruction and the first XML Element contents),
 * which are the only ones which are tricky to format (due to element and its attributes being freeform - thus formatting lost when serializing the DOM
 * and the PI being completely optional for the POM Document)
 */
class FormatCommand : AbstractCommandJ() {
    /**
     * StAX InputFactory
     */
    private val inputFactory = XMLInputFactory.newInstance()

    /**
     * StAX OutputFactory
     */
    private val outputFactory = XMLOutputFactory.newInstance()

    private val singleElementsWithAttributes: MutableList<MatchDataJ> = arrayListOf()

    override fun execute(pm: ProjectModelJ): Boolean {
        for (pomFile in pm.allPomFiles()) {
            FormatCommandJ.parseXmlAndCharset(this.inputFactory,this.singleElementsWithAttributes, pomFile)

            pomFile.endl = FormatCommandJ.parseLineEndings(pomFile)
            pomFile.indent = FormatCommandJ.guessIndent(this.inputFactory, pomFile)
        }

        return super.execute(pm)
    }

    /**
     * When doing the opposite, render the XML using the optionally supplied encoding (defaults to UTF8 obviously)
     * but apply the original formatting as well
     */
    override fun postProcess(pm: ProjectModelJ): Boolean {
        for (pomFile in pm.allPomFiles()) {
            /**
             * Serializes it back
             */
            val content = serializePomFile(pomFile)

            pomFile.resultPomBytes = content
        }

        return super.postProcess(pm)
    }

    /**
     * Serialize a POM Document
     *
     * @param pom pom document
     * @return bytes for the pom document
     */
    private fun serializePomFile(pom: POMDocument): ByteArray {
        // Generate a String representation. We'll need to patch it up and apply back
        // differences we recored previously on the pom (see the pom member variables)
        var xmlRepresentation = pom.resultPom.asXML().toString()

        val originalElementMap = FormatCommandJ.elementBitSet(this.inputFactory, this.outputFactory, pom.originalPom)
        val targetElementMap : BitSet= FormatCommandJ.elementBitSet(this.inputFactory, this.outputFactory, xmlRepresentation.toByteArray())

        // Let's find out the original empty elements from the original pom and store into a stack
        val elementsToReplace: MutableList<MatchDataJ> = FormatCommandJ.getElementsToReplace(originalElementMap, pom)

        // Lets to the replacements backwards on the existing, current pom
        val link : LinkedHashMap<Int, MatchDataJ> = FormatCommandJ.findSingleElementMatchesFrom(xmlRepresentation)
        val emptyElements : Map<Int, MatchDataJ> = FormatCommandJ.getEmptyElements(targetElementMap, xmlRepresentation)

        emptyElements.forEach { (_, match) ->
            val nextMatch = elementsToReplace.removeFirst()

            xmlRepresentation = xmlRepresentation.replaceRange(match.range, nextMatch.content)
        }

        var lastIndex = 0

        singleElementsWithAttributes.sortedBy { it.range.first }.forEach { match ->
            //val index = xmlRepresentation.indexOf(match.modifiedContent!!, lastIndex)
            val representationMatch = match.modifiedContent!!.find(xmlRepresentation, lastIndex)

            if (null == representationMatch) {
                LOGGER.warn("Failure on quoting: {}", match)
            } else {
                xmlRepresentation =
                    xmlRepresentation.replaceRange(representationMatch.range, match.content)

                lastIndex = representationMatch.range.first + match.content.length
            }
        }

        /**
         * We might need to replace the beginning of the POM with the same content
         * from the very beginning
         *
         * Grab the same initial offset from the formatted element like we did
         */
        val inputFactory = XMLInputFactory.newInstance()
        val eventReader = inputFactory.createXMLEventReader(
            xmlRepresentation.toByteArray(pom.charset).inputStream()
        )

        while (true) {
            val event = eventReader.nextEvent()

            if (event.isEndElement) {
                /**
                 * Apply the formatting and tweak its XML Representation
                 */
                val endElementEvent = (event as EndElement)

                val offset = endElementEvent.location.characterOffset

                xmlRepresentation =
                    pom.preamble + xmlRepresentation.substring(offset) + pom.suffix

                break
            }

            /**
             * This code shouldn't be unreachable at all
             */
            if (!eventReader.hasNext())
                throw IllegalStateException("Couldn't find document start")
        }

        /**
         * Serializes it back from (string to ByteArray)
         */
        val serializedContent = xmlRepresentation.toByteArray(pom.charset)

        return serializedContent
    }

    companion object {

        val LOGGER: Logger = LoggerFactory.getLogger(FormatCommand::class.java)
    }
}
