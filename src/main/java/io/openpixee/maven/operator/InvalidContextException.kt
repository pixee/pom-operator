package io.openpixee.maven.operator

import java.lang.RuntimeException

/**
 * This is an exception to tag when the output file couldn't be generated - perhaps due a missing or incompatible maven installation
 */
internal class InvalidContextException() : RuntimeException()