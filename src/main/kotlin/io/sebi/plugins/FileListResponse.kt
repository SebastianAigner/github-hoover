package io.sebi.plugins

import kotlinx.serialization.Serializable

@Serializable
data class FileListResponse(
    val sha: String,
    val treeNode: List<TreeNode>,
    val truncated: Boolean,
    val url: String
)