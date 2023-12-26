package io.sebi.domain.downloader

interface GitHubFolderDownloader {

    suspend fun downloadFilesAsZip(
        username: String,
        repoName: String,
        branch: String,
        folderPath: String
    ): ByteArray

}