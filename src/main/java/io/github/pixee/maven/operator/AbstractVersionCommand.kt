package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.AbstractCommandJ
import io.github.pixee.maven.operator.java.AbstractVersionCommandJ
import io.github.pixee.maven.operator.java.VersionDefinitionJ
import org.apache.commons.lang3.builder.CompareToBuilder
import java.util.*

// TODO-CARLOS this is not fully migrated
/**
 * Base Class for Version Detection Commands
 */
open class AbstractVersionCommand : AbstractCommandJ() {
    /**
     * Internal Result
     */
    internal val result: MutableSet<VersionDefinitionJ> =
        TreeSet<VersionDefinitionJ>(VERSION_KIND_COMPARATOR)

    companion object {
        internal val VERSION_KIND_COMPARATOR = AbstractVersionCommandJ.VERSION_KIND_COMPARATOR

        internal val TYPE_TO_KIND = mapOf(
            "source" to Kind.SOURCE,
            "target" to Kind.TARGET,
            "release" to Kind.RELEASE,
        )

        internal val PROPERTY_TO_KIND = TYPE_TO_KIND.entries.map {
            "maven.compiler.${it.key}" to it.value
        }.toMap()
    }
}
