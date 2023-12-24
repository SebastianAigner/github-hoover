package io.sebi.domain.repository

import io.sebi.domain.model.FileListResponse
import io.sebi.domain.model.PathAndFileContents
import io.sebi.domain.model.PathWithUrlAndPermissions
import io.sebi.domain.model.RepoPath
import kotlinx.coroutines.flow.Flow

interface GitHubFolderDownloaderRepository {

    suspend fun getFileListResponse(url: String): FileListResponse

    suspend fun listFiles(repo: RepoPath): FileListResponse

    suspend fun downloadFile(url: String): ByteArray

    fun listAllFiles(repo: RepoPath): Flow<PathWithUrlAndPermissions>

    suspend fun getDownloadedFiles(username: String,
                                   repoName: String,
                                   branch: String,
                                   folderPath: String): Flow<PathAndFileContents>

    suspend fun zipDownloadedFiles(pathName: String,
                                   username: String,
                                   repoName: String,
                                   branch: String,
                                   folderPath: String)

}