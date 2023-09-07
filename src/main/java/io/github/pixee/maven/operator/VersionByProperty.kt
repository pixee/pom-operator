package io.github.pixee.maven.operator

import java.util.*

class VersionByProperty : AbstractVersionCommand() {
    override fun execute(pm: ProjectModel): Boolean {
        val definedProperties: MutableSet<VersionDefinition> =
            TreeSet<VersionDefinition>(VERSION_KIND_COMPARATOR)

        pm.propertiesDefinedByFile.entries.filter { PROPERTY_TO_KIND.containsKey(it.key) }
            .forEach { entry ->
                val kind = PROPERTY_TO_KIND[entry.key]!!

                definedProperties.add(VersionDefinition(kind, entry.value.first().first))
            }

        this.result.addAll(definedProperties)

        return definedProperties.isNotEmpty()
    }
}