package io.sebi.domain.model

import io.ktor.resources.*

@Resource("/repo")
data class RepoPath(
    val user: String,
    val name: String,
    val branch: String,
    val path: String
)