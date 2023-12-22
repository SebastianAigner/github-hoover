package io.sebi.plugins

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Tree")
data class TreeNode(
    val mode: String,
    val path: String,
    val sha: String,
    val size: Int? = null,
    val type: String,
    val url: String
)

val TreeNode.isDirectory: Boolean
    get() =
        this.type == "tree"

val TreeNode.isFile: Boolean
    get() =
        this.type == "blob"