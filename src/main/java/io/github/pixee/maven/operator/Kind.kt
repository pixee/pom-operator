package io.github.pixee.maven.operator

/**
 * Represents a Kind of Version Pinning in a Maven File, representing the `-source` and `-target`
 * flags as well as the newer `-release` flag of `javac`.
 */
enum class Kind {
    SOURCE, TARGET, RELEASE
}