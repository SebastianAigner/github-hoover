package io.sebi.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SingleFile(
    val content: String,
    val encoding: String,
    @SerialName("node_id")
    val nodeId: String,
    val sha: String,
    val size: Int,
    val url: String
)