package io.openpixee.maven.operator

import java.lang.RuntimeException

class MissingDependencyException : RuntimeException {
    constructor(message: String?) : super(message)
}