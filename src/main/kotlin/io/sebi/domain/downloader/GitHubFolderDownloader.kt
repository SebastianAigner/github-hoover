package io.sebi.domain.downloader

import io.sebi.domain.model.RepoPath

interface GitHubFolderDownloader {

    suspend fun downloadFilesAsZip(
        path: RepoPath
    ): ByteArray

    suspend fun getSha(it: RepoPath): String

}