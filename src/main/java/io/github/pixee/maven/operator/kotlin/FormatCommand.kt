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
            val content = FormatCommandJ.serializePomFile(inputFactory, outputFactory, singleElementsWithAttributes, pomFile)

            pomFile.resultPomBytes = content
        }

        return super.postProcess(pm)
    }

    companion object {

        val LOGGER: Logger = LoggerFactory.getLogger(FormatCommand::class.java)
    }
}
