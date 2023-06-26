package io.github.pixee.maven.operator

import java.io.File
import java.io.IOException

class InvalidPathException(
    val parentPath: File,
    val relativePath: String
) : IOException("Invalid Relative Path $relativePath (from ${parentPath.absolutePath})")