package io.sebi.plugins

import kotlinx.serialization.Serializable

@Serializable
data class FileListResponse(
    val sha: String,
    val tree: List<Tree>,
    val truncated: Boolean,
    val url: String
)