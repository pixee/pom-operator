package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.VersionDefinitionJ
import java.util.*

class VersionByProperty : AbstractVersionCommand() {
    override fun execute(pm: ProjectModel): Boolean {
        val definedProperties: MutableSet<VersionDefinitionJ> =
            TreeSet<VersionDefinitionJ>(VERSION_KIND_COMPARATOR)

        pm.propertiesDefinedByFile.entries.filter { PROPERTY_TO_KIND.containsKey(it.key) }
            .forEach { entry ->
                val kind = PROPERTY_TO_KIND[entry.key]!!

                definedProperties.add(VersionDefinitionJ(kind, entry.value.first().first))
            }

        this.result.addAll(definedProperties)

        return definedProperties.isNotEmpty()
    }
}