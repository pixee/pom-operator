package io.github.pixee.maven.operator

import org.apache.commons.lang3.builder.CompareToBuilder
import java.util.*

open class AbstractVersionCommand : AbstractCommand() {
    internal val result: MutableSet<VersionDefinition> =
        TreeSet<VersionDefinition>(VERSION_KIND_COMPARATOR)

    companion object {
        internal val VERSION_KIND_COMPARATOR = object : Comparator<VersionDefinition> {
            override fun compare(o1: VersionDefinition?, o2: VersionDefinition?): Int {
                if (o1 == null)
                    return 1
                if (o2 == null)
                    return -1

                return CompareToBuilder().append(o1.kind, o2.kind).toComparison()
            }
        }
    }
}
