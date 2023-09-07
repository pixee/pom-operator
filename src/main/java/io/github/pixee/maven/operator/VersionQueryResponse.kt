package io.github.pixee.maven.operator

import com.github.zafarkhaja.semver.Version

data class VersionQueryResponse(
    val source: Version,
    val target: Version,
)