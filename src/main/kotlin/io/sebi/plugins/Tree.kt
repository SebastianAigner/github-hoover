package io.sebi.plugins

import kotlinx.serialization.Serializable

@Serializable
data class Tree(
    val mode: String,
    val path: String,
    val sha: String,
    val size: Int? = null,
    val type: String,
    val url: String
)