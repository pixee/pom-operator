package io.github.pixee.maven.operator

import java.util.*

class VersionByProperty : AbstractVersionCommand() {
    override fun execute(pm: ProjectModel): Boolean {
        val definedProperties: MutableSet<VersionDefinition> =
            TreeSet<VersionDefinition>(VERSION_KIND_COMPARATOR)

        pm.propertiesDefinedByFile.entries.filter { PROPERTY_MAP.containsKey(it.key) }
            .forEach { entry ->
                val kind = PROPERTY_MAP[entry.key]!!

                definedProperties.add(VersionDefinition(kind, entry.value.first().first))
            }

        this.result.addAll(definedProperties)

        return definedProperties.isNotEmpty()
    }

    companion object {
        internal val PROPERTY_MAP = mapOf(
            "maven.compiler.source" to Kind.SOURCE,
            "maven.compiler.target" to Kind.TARGET,
            "maven.compiler.release" to Kind.RELEASE,
        )
    }
}