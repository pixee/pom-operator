package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.KindJ
import org.apache.commons.lang3.builder.CompareToBuilder
import java.util.*

/**
 * Base Class for Version Detection Commands
 */
open class AbstractVersionCommand : AbstractCommand() {
    /**
     * Internal Result
     */
    internal val result: MutableSet<VersionDefinition> =
        TreeSet<VersionDefinition>(VERSION_KIND_COMPARATOR)

    companion object {
        internal val VERSION_KIND_COMPARATOR = object : Comparator<VersionDefinition> {
            override fun compare(o1: VersionDefinition?, o2: VersionDefinition?): Int {
                if (o1 == null) return 1
                if (o2 == null) return -1

                return CompareToBuilder().append(o1.kind, o2.kind).toComparison()
            }
        }

        internal val TYPE_TO_KIND = mapOf(
            "source" to KindJ.SOURCE,
            "target" to KindJ.TARGET,
            "release" to KindJ.RELEASE,
        )

        internal val PROPERTY_TO_KIND = TYPE_TO_KIND.entries.map {
            "maven.compiler.${it.key}" to it.value
        }.toMap()
    }
}