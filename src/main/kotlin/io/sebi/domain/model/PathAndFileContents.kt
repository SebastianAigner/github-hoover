package io.sebi.domain.model

class PathAndFileContents(
    val path: String,
    val byteArray: ByteArray,
    val permissions: Int
)