@file:Suppress("DEPRECATION")

package io.github.pixee.maven.operator

import com.github.zafarkhaja.semver.Version
import io.github.pixee.maven.operator.java.ProjectModelJ
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.SystemUtils
import org.apache.commons.lang3.text.StrSubstitutor
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.Text
import org.dom4j.tree.DefaultText
import org.jaxen.SimpleNamespaceContext
import org.jaxen.XPath
import org.jaxen.dom4j.Dom4jXPath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File


/**
 * Common Utilities
 */
object Util {
    private val LOGGER: Logger = LoggerFactory.getLogger(Util::class.java)



    /**
     * Extension Function to Select the XPath Nodes
     *
     * @param expression expression to use
     */
    // TODO-CARLOS
    @Suppress("UNCHECKED_CAST")
    fun selectXPathNodes(node: Node, expression: String) : List<Node> =
        createXPathExpression(expression).selectNodes(node)!! as List<Node>

    /**
     * Creates a XPath Expression from a given expression string
     *
     * @param expression expression to create xpath from
     */
    private fun createXPathExpression(expression: String): XPath {
        val xpath = Dom4jXPath(expression)

        xpath.namespaceContext = namespaceContext

        return xpath
    }

    /**
     * Hard-Coded POM Namespace Map
     */
    private val namespaceContext = SimpleNamespaceContext(
        mapOf(
            "m" to "http://maven.apache.org/POM/4.0.0"
        )
    )


    internal fun which(path: String): File? {
        val nativeExecutables: List<String> = if (SystemUtils.IS_OS_WINDOWS) {
            listOf("", ".exe", ".bat", ".cmd").map { path + it }.toList()
        } else {
            listOf(path)
        }

        val pathContentString = System.getenv("PATH")

        val pathElements = pathContentString.split(File.pathSeparatorChar)

        val possiblePaths = nativeExecutables.flatMap { executable ->
            pathElements.map { pathElement ->
                File(File(pathElement), executable)
            }
        }

        val isCliCallable: (File) -> Boolean = if (SystemUtils.IS_OS_WINDOWS) { it ->
            it.exists() && it.isFile
        } else { it ->
            it.exists() && it.isFile && it.canExecute()
        }

        val result = possiblePaths.findLast(isCliCallable)

        if (null == result) {
            LOGGER.warn(
                "Unable to find mvn executable (execs: {}, path: {})",
                nativeExecutables.joinToString("/"),
                pathContentString
            )
        }

        return result
    }
}