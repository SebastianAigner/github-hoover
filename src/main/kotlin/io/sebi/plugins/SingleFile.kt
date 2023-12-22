package io.sebi.plugins

import kotlinx.serialization.Serializable

@Serializable
data class SingleFile(
    val content: String,
    val encoding: String,
    val node_id: String,
    val sha: String,
    val size: Int,
    val url: String
)