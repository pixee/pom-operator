package io.openpixee.maven.operator

import org.dom4j.Document
import org.dom4j.io.SAXReader
import java.io.File
import java.io.InputStream
import java.net.URL

/**
 * Builder Object for ProjectModel instances
 */
class ProjectModelFactory private constructor(
    private var pomPath: URL?,
    private var pomDocument: Document,
    private var dependency: Dependency? = null,
    private var skipIfNewer: Boolean = false,
    private var useProperties: Boolean = false,
    private var activeProfiles: Set<String> = emptySet(),
    private var overrideIfAlreadyExists: Boolean = false,
) {
    /**
     * Fluent Setter
     * @param pomPath pomPath
     */
    fun withPomPath(pomPath: URL): ProjectModelFactory = this.apply {
        this.pomPath = pomPath
    }

    /**
     * Fluent Setter
     *
     * @param dep dependency
     */
    fun withDependency(dep: Dependency): ProjectModelFactory = this.apply {
        this.dependency = dep
    }

    /**
     * Fluent Setter
     */
    fun withSkipIfNewer(skipIfNewer: Boolean): ProjectModelFactory = this.apply {
        this.skipIfNewer = skipIfNewer
    }

    /**
     * Fluent Setter
     */
    fun withUseProperties(useProperties: Boolean): ProjectModelFactory = this.apply {
        this.useProperties = useProperties
    }

    /**
     * Fluent Setter
     */
    fun withActiveProfiles(vararg activeProfiles: String): ProjectModelFactory = this.apply {
        this.activeProfiles = setOf(*activeProfiles)
    }

    /**
     * Fluent Setter
     */
    fun withOverrideIfAlreadyExists(overrideIfAlreadyExists: Boolean) = this.apply {
        this.overrideIfAlreadyExists = overrideIfAlreadyExists
    }

    /**
     * Fluent Setter
     */
    fun build(): ProjectModel {
        return ProjectModel(
            pomPath = pomPath,
            pomDocument = pomDocument,
            dependency = dependency,
            skipIfNewer = skipIfNewer,
            useProperties = useProperties,
            activeProfiles = activeProfiles,
            overrideIfAlreadyExists = overrideIfAlreadyExists
        )
    }

    companion object {
        @JvmStatic
        fun load(`is`: InputStream): ProjectModelFactory {
            val pomDocument = SAXReader().read(`is`)!!

            return ProjectModelFactory(pomPath = null, pomDocument = pomDocument)
        }

        @JvmStatic
        fun load(f: File) =
            load(f.toURI().toURL())

        @JvmStatic
        fun load(url: URL): ProjectModelFactory {
            val pomDocument = SAXReader().read(url.openStream())

            return ProjectModelFactory(pomPath = url, pomDocument = pomDocument)
        }
    }
}