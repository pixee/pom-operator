package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.AbstractVersionCommandJ
import io.github.pixee.maven.operator.java.ProjectModelJ
import io.github.pixee.maven.operator.java.VersionDefinitionJ
import java.util.*

class VersionByProperty : AbstractVersionCommandJ() {
    override fun execute(pm: ProjectModelJ): Boolean {
        val definedProperties: MutableSet<VersionDefinitionJ> =
            TreeSet<VersionDefinitionJ>(AbstractVersionCommandJ.VERSION_KIND_COMPARATOR)

        pm.propertiesDefinedByFile().entries.filter { AbstractVersionCommandJ.PROPERTY_TO_KIND.containsKey(it.key) }
            .forEach { entry ->
                val kind = AbstractVersionCommandJ.PROPERTY_TO_KIND[entry.key]!!

                definedProperties.add(VersionDefinitionJ(kind, entry.value.first().first))
            }

        this.result.addAll(definedProperties)

        return definedProperties.isNotEmpty()
    }
}