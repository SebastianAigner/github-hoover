package io.sebi.domain.downloader

interface GitHubFolderDownloader {

    suspend fun downloadFilesAsZip(
        targetPath: String,
        username: String,
        repoName: String,
        branch: String,
        folderPath: String
    )

}