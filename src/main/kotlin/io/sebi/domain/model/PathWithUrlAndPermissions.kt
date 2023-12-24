package io.sebi.domain.model

data class PathWithUrlAndPermissions(
    val path: String,
    val url: String,
    val permissions: Int
)