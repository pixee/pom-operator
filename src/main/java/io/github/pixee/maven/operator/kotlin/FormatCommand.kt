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
            parseXmlAndCharset(pomFile)

            pomFile.endl = parseLineEndings(pomFile)
            pomFile.indent = guessIndent(pomFile)
        }

        return super.execute(pm)
    }

    /**
     * This one is quite fun yet important. Let me explain:
     *
     * The DOM doesn't track records if empty elements are either `<element>` or `<element/>`. Therefore we need to scan all ocurrences of
     * singleton elements.
     *
     * Therefore we use a bitSet to keep track of each element and offset, scanning it forward
     * when serializing we pick backwards and rewrite tags accordingly
     *
     * @param doc Raw Document Bytes
     * @see RE_EMPTY_ELEMENT_NO_ATTRIBUTES
     * @return bitSet of
     *
     */
    private fun elementBitSet(doc: ByteArray): BitSet {
        return FormatCommandJ.elementBitSet(inputFactory, outputFactory, doc)
    }

    /**
     * Returns a reverse-ordered list of all the single element matches from the pom document
     * raw string
     *
     * this is important so we can mix and match offsets and apply formatting accordingly
     *
     * @param xmlDocumentString Rendered POM Document Contents (string-formatted)
     * @return map of (index, matchData object) reverse ordered
     */
    private fun findSingleElementMatchesFrom(xmlDocumentString: String): LinkedHashMap<Int, MatchDataJ> {
        val allFoundMatches = RE_EMPTY_ELEMENT_NO_ATTRIBUTES.findAll(xmlDocumentString).toList()

        val emptyMappedTags: List<MatchDataJ> =
            allFoundMatches.map {
                MatchDataJ(
                    it.range,
                    it.value,
                    ((it.groups[1]?.value ?: it.groups[2]?.value)!!),
                    false,
                    null
                )
            }.toList()

        val allTags = listOf(emptyMappedTags).flatMap { it }
            .map { it.range.first to it }

        return allTags.sortedByDescending { it.first }.toMap(LinkedHashMap())
    }

    /**
     * Guesses the indent character (spaces / tabs) and length from the original document
     * formatting settings
     *
     * @param pomFile (project model) where it takes its input pom
     * @return indent string
     */
    private fun guessIndent(pomFile: POMDocument): String {
        val eventReader = inputFactory.createXMLEventReader(pomFile.originalPom.inputStream())

        val freqMap: MutableMap<Int, Int> = mutableMapOf()
        val charFreqMap: MutableMap<Char, Int> = mutableMapOf()

        /**
         * Parse, while grabbing whitespace sequences and examining it
         */
        while (eventReader.hasNext()) {
            val event = eventReader.nextEvent()

            if (event is Characters) {
                if (StringUtils.isWhitespace(event.asCharacters().data)) {
                    val patterns = event.asCharacters().data.split(*LINE_ENDINGS.toTypedArray())

                    /**
                     * Updates space / character frequencies found
                     */
                    val blankPatterns = patterns
                        .filter { it.isNotEmpty() }
                        .filter { StringUtils.isAllBlank(it) }

                    blankPatterns
                        .map { it to it.length }
                        .forEach {
                            freqMap.merge(it.second, 1) { a, b -> a + b }
                        }

                    blankPatterns.map { it[0] }
                        .forEach {
                            charFreqMap.merge(it, 1) { a, b ->
                                a + b
                            }
                        }
                }
            }
        }

        /**
         * Assign most frequent indent char
         */
        val indentCharacter: Char = charFreqMap.entries.maxBy { it.value }.key

        /**
         * Casts as a String
         */
        val indentcharacterAsString = String(charArrayOf(indentCharacter))

        /**
         * Picks the length
         */
        val indentLength = freqMap.entries.minBy { it.key }.key

        /**
         * Builds the standard indent string (length vs char)
         */
        val indentString = StringUtils.repeat(indentcharacterAsString, indentLength)

        /**
         * Returns it
         */
        return indentString
    }

    private fun parseLineEndings(pomFile: POMDocument): String {
        val str = String(pomFile.originalPom.inputStream().readBytes(), pomFile.charset)

        return LINE_ENDINGS.associateWith { str.split(it).size }
            .maxBy { it.value }
            .key
    }

    private fun parseXmlAndCharset(pomFile: POMDocument) {
        /**
         * Performs a StAX Parsing to Grab the first element
         */
        val eventReader = inputFactory.createXMLEventReader(pomFile.originalPom.inputStream())

        var charset: Charset? = null

        /**
         * Parse, while grabbing its preamble and encoding
         */
        var elementIndex: Int = 0
        var mustTrack: Boolean = false

        var hasPreamble = false

        var elementStart: Int = 0
        var elementEnd: Int = 0

        var prevEvents: MutableList<XMLEvent> = arrayListOf()

        while (eventReader.hasNext()) {
            val event = eventReader.nextEvent()

            if (event.isStartDocument && (event as StartDocument).encodingSet()) {
                /**
                 * Processing Instruction Found - Store its Character Encoding
                 */
                charset = Charset.forName(event.characterEncodingScheme)
            } else if (event.isStartElement) {
                val asStartElement = event.asStartElement()

                val name = asStartElement.name.localPart

                val attributes = asStartElement.attributes.asSequence().toList()

                if (elementIndex > 0 && attributes.isNotEmpty()) {
                    // record this guy
                    mustTrack = true

                    val lastCharacterEvent =
                        prevEvents.filter { it.isCharacters }.last().asCharacters()

                    elementStart =
                        lastCharacterEvent.location.characterOffset - lastCharacterEvent.data.length
                } else if (mustTrack) { // turn it off
                    mustTrack = false
                }

                elementIndex++
            } else if (event.isEndElement) {
                /**
                 * First End of Element ("Tag") found - store its offset
                 */
                val endElementEvent = event.asEndElement()

                val location = endElementEvent.location

                val offset = location.characterOffset

                if (mustTrack) {
                    mustTrack = false

                    val localPart = event.asEndElement().name.localPart

                    val untrimmedOriginalContent = pomFile.originalPom.toString(pomFile.charset)
                        .substring(elementStart, offset)

                    val trimmedOriginalContent = untrimmedOriginalContent.trim()

                    val realElementStart = pomFile.originalPom.toString(pomFile.charset)
                        .indexOf(trimmedOriginalContent, elementStart)

                    val contentRange = IntRange(
                        realElementStart,
                        realElementStart + 1 + trimmedOriginalContent.length
                    )

                    val contentRe = writeAsRegex(prevEvents.filter { it.isStartElement }.last()
                        .asStartElement()
                    )

                    val modifiedContentRE =
                        Regex(contentRe)

                    singleElementsWithAttributes.add(
                        MatchDataJ(
                            contentRange,
                            trimmedOriginalContent,
                            localPart,
                            true,
                            modifiedContentRE,
                        )
                    )
                }

                mustTrack = false

                /**
                 * Sets Preamble - keeps parsing anyway
                 */
                if (!hasPreamble) {
                    pomFile.preamble =
                        pomFile.originalPom.toString(pomFile.charset).substring(0, offset)

                    hasPreamble = true
                }
            }

            prevEvents.add(event)

            while (prevEvents.size > 4)
                prevEvents.removeAt(0)

            if (!eventReader.hasNext())
                if (!hasPreamble)
                    throw IllegalStateException("Couldn't find document start")
        }

        if (null == charset) {
            val detectedCharsetName =
                UniversalDetector.detectCharset(pomFile.originalPom.inputStream())

            charset = Charset.forName(detectedCharsetName)
        }

        pomFile.charset = charset!!

        val lastLine = String(pomFile.originalPom, pomFile.charset)

        val lastLineTrimmed = lastLine.trimEnd()

        pomFile.suffix = lastLine.substring(lastLineTrimmed.length)
    }

    /**
     * A Slight variation on writeAsUnicode from stax which writes as a regex
     * string so we could rewrite its output
     */
    private fun writeAsRegex(element: StartElement): String {
        val writer = StringWriter()

        writer.write("<")
        writer.write(Pattern.quote(element.name.localPart))

        val attrIter: Iterator<*> = element.getAttributes()
        while (attrIter.hasNext()) {
            val attr = attrIter.next() as Attribute

            writer.write("\\s+")

            writer.write(Pattern.quote(attr.name.localPart))
            writer.write("=[\\\"\']")
            writer.write(Pattern.quote(attr.value))
            writer.write("[\\\"']")

        }
        writer.write("\\s*\\/\\>")

        return writer.toString()
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

        val originalElementMap = elementBitSet(pom.originalPom)
        val targetElementMap = elementBitSet(xmlRepresentation.toByteArray())

        // Let's find out the original empty elements from the original pom and store into a stack
        val elementsToReplace: MutableList<MatchDataJ> = ArrayList<MatchDataJ>().apply {
            val matches =
                findSingleElementMatchesFrom(pom.originalPom.toString(pom.charset)).values

            val filteredMatches =
                matches.filter { it.hasAttributes == false && originalElementMap[it.range.first] }

            this.addAll(filteredMatches)
        }

        // Lets to the replacements backwards on the existing, current pom
        val emptyElements = findSingleElementMatchesFrom(xmlRepresentation)
            .filter { targetElementMap[it.value.range.first] }

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
        val LINE_ENDINGS = setOf("\r\n", "\n", "\r")

        val RE_EMPTY_ELEMENT_NO_ATTRIBUTES =
            Regex("""<([\p{Alnum}_\-.]+)>\s*</\1>|<([\p{Alnum}_\-.]+)\s*/>""")

        val LOGGER: Logger = LoggerFactory.getLogger(FormatCommand::class.java)
    }
}
