package io.sebi.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileListResponse(
    val sha: String,
    @SerialName("tree")
    val treeNode: List<TreeNode>,
    val truncated: Boolean,
    val url: String
)