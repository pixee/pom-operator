package io.openpixee.maven.operator

/**
 * Command Class to Short-Circuit/Discard Processing when no pom changes were made
 */
class DiscardFormatCommand : AbstractSimpleCommand() {
    override fun postProcess(c: ProjectModel): Boolean {
        if (!c.modifiedByCommand) {
            c.resultPomBytes = c.originalPom

            return true
        }

        return super.postProcess(c)
    }
}