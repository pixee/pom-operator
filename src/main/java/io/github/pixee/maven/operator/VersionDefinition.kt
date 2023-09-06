package io.github.pixee.maven.operator

/**
 * Represents a tuple (kind / version string) applicable from a pom.xml file
 */
data class VersionDefinition(
    val kind: Kind,
    val value: String,
)