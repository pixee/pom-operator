@file:Suppress("DEPRECATION")

package io.openpixee.maven.operator

import com.github.zafarkhaja.semver.Version
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
import java.io.*


/**
 * Common Utilities
 */
object Util {
    fun Element.addIndentedElement(c: ProjectModel, name: String): Element {
        val contentList = this.content()

        val prefix = c.endl + StringUtils.repeat(c.indent, 1 + findIndentLevel(this))

        val suffix = c.endl + StringUtils.repeat(c.indent, findIndentLevel(this))

        if (contentList.isNotEmpty() && contentList.last() is Text) {
            val lastElement = contentList.last() as Text

            if (StringUtils.isWhitespace(lastElement.text)) {
                contentList.remove(contentList.last())
            }
        }

        contentList.add(DefaultText(prefix))

        val newElement = this.addElement(name)

        contentList.add(DefaultText(suffix))

        return newElement
    }

    /**
     * Guesses the current indent level of the nearest nodes
     */
    internal fun findIndentLevel(startingNode: Element): Int {
        var level = 0

        var node = startingNode

        while (node.parent != null) {
            level += 1
            node = node.parent
        }

        return level
    }

    /**
     * Represents a Property Reference - as a regex
     */
    internal val PROPERTY_REFERENCE_REGEX = Regex("^\\\$\\{(.*)}$")

    /**
     * Upserts a given property
     */
    internal fun upgradeProperty(c: ProjectModel, propertyName: String) {
        if (null == c.resultPom.rootElement.element("properties")) {
            c.resultPom.rootElement.addIndentedElement(c, "properties")
        }

        val parentPropertyElement = c.resultPom.rootElement.element("properties")

        if (null == parentPropertyElement.element(propertyName)) {
            parentPropertyElement.addIndentedElement(c, propertyName)
        } else {
            if (!c.overrideIfAlreadyExists) {
                val propertyReferenceRE = Regex.fromLiteral("\${$propertyName}")

                val numberOfAllCurrentMatches =
                    propertyReferenceRE.findAll(c.pomDocument.asXML()).toList().size

                if (numberOfAllCurrentMatches > 1) {
                    throw IllegalStateException("Property $propertyName is already defined - and used more than once.")
                }
            }
        }

        val propertyElement = parentPropertyElement.element(propertyName)

        propertyElement.text = c.dependency!!.version
    }

    /**
     * Creates a property Name
     */
    internal fun propertyName(c: ProjectModel, versionNode: Element): String {
        val version = versionNode.textTrim

        if (PROPERTY_REFERENCE_REGEX.matches(version)) {
            val match = PROPERTY_REFERENCE_REGEX.find(version)

            val firstMatch = match!!.groups[1]!!

            return firstMatch.value
        }

        return "versions." + c.dependency!!.artifactId
    }

    /**
     * Identifies if an upgrade is needed
     */
    internal fun findOutIfUpgradeIsNeeded(c: ProjectModel, versionNode: Element): Boolean {
        val currentVersionNodeText = resolveVersion(c, versionNode.text!!)

        val currentVersion = Version.valueOf(currentVersionNodeText)
        val newVersion = Version.valueOf(c.dependency!!.version)

        @Suppress("UnnecessaryVariable") val versionsAreIncreasing =
            newVersion.greaterThan(currentVersion)

        return versionsAreIncreasing
    }

    internal fun resolveVersion(c: ProjectModel, versionText: String): String =
        if (PROPERTY_REFERENCE_REGEX.matches(versionText)) {
            @Suppress("DEPRECATION")
            StrSubstitutor(c.resolvedProperties).replace(versionText)
        } else {
            versionText
        }

    /**
     * Escapes a Property Name
     */
    internal fun escapedPropertyName(propertyName: String): String =
        "\${$propertyName}"

    /**
     * Given a Version Node, upgrades a resulting POM
     */
    internal fun upgradeVersionNode(c: ProjectModel, versionNode: Element) {
        if (c.useProperties) {
            val propertyName = propertyName(c, versionNode)

            // define property
            upgradeProperty(c, propertyName)

            versionNode.text = escapedPropertyName(propertyName)
        } else {
            versionNode.text = c.dependency!!.version
        }
    }

    /**
     * Builds a Lookup Expression String for a given dependency
     *
     * @param dependency Dependency
     */
    fun buildLookupExpressionForDependency(dependency: Dependency): String =
        "/m:project" +
                "/m:dependencies" +
                "/m:dependency" +
                /* */ "[./m:groupId[text()='${dependency.groupId}'] and " +
                /*  */ "./m:artifactId[text()='${dependency.artifactId}']" +
                "]"

    /**
     * Builds a Lookup Expression String for a given dependency, but under the &gt;dependencyManagement&gt; section
     *
     * @param dependency Dependency
     */
    fun buildLookupExpressionForDependencyManagement(dependency: Dependency): String =
        "/m:project" +
                "/m:dependencyManagement" +
                "/m:dependencies" +
                "/m:dependency" +
                /* */ "[./m:groupId[text()='${dependency.groupId}'] and " +
                /*  */ "./m:artifactId[text()='${dependency.artifactId}']" +
                "]"

    /**
     * Extension Function to Select the XPath Nodes
     *
     * @param expression expression to use
     */
    @Suppress("UNCHECKED_CAST")
    fun Node.selectXPathNodes(expression: String) =
        createXPathExpression(expression).selectNodes(this)!! as List<Node>

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
            AbstractSimpleQueryCommand.LOGGER.warn(
                "Unable to find mvn executable (execs: {}, path: {})",
                nativeExecutables.joinToString("/"),
                pathContentString
            )
        }

        return result
    }
}
