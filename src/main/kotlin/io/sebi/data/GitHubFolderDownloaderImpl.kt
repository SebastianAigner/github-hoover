package io.sebi.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.sebi.common.Constant
import io.sebi.domain.downloader.GitHubFolderDownloader
import io.sebi.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GitHubFolderDownloaderImpl(
    private val myClient: HttpClient
) : GitHubFolderDownloader {

    private val urlToFileListResponse = ConcurrentHashMap<String, FileListResponse>()

    private val cs = CoroutineScope(Dispatchers.Default)
    private suspend fun getFileListResponse(url: String): FileListResponse {
        return urlToFileListResponse.getOrPut(url) {
            println("Getting $url from network")
            cs.launch {
                delay(Constant.GITHUB_API_CACHING_LIFETIME)
                urlToFileListResponse.remove(url)
            }
            myClient.get(url).body<FileListResponse>()
        }
    }

    private val RepoPath.url
        get() =
            "https://api.github.com/repos/${user}/${name}/git/trees/${branch}"

    private suspend fun listFiles(repo: RepoPath): FileListResponse {
        val root = getFileListResponse(repo.url)
        val directoryContents = repo.path.split("/")
            .filter { it.isNotBlank() }
            .fold(root) { directoryContents, s ->
                val directory: TreeNode = directoryContents.treeNode.find { it.path == s }
                    ?: throw IllegalArgumentException("Invalid path: ${repo.path}")
                getFileListResponse(directory.url)
            }
        return directoryContents
    }

    private suspend fun downloadFile(url: String): ByteArray {
        val file = myClient.get(url).body<SingleFile>()
        val b64string = file.content.replace("\n", "")
        // base64 decode string
        return Base64.getDecoder().decode(b64string)
    }


    private fun listAllFiles(repo: RepoPath): Flow<PathWithUrlAndPermissions> {
        return channelFlow {
            val sem = Semaphore(10)
            suspend fun walk(currDir: String) {
                for (treeNode in listFiles(repo.copy(path = repo.path + currDir)).treeNode.sortedBy { it.isDirectory }) {
                    println("Looking at ${repo.path} / $currDir / ${treeNode.path} (${treeNode.type})")
                    if (treeNode.isDirectory) {
                        launch {
                            sem.withPermit {
                                walk(currDir + "/" + treeNode.path)
                            }
                        }
                    } else {
                        // file
                        launch {
                            send(
                                PathWithUrlAndPermissions(
                                    currDir + "/" + treeNode.path,
                                    treeNode.url,
                                    convertToPermissions(treeNode.mode)
                                )
                            )
                        }
                    }
                }
            }
            walk("")
        }
    }


    // Here's a fun one: The docs mention nothing about these being octal numbers.
    // Also, everything you find on https://www.tabnine.com/code/java/methods/org.apache.commons.compress.archivers.zip.ZipArchiveEntry/setUnixMode,
    // INCLUDING APACHE PROJECTS gets this wrong.
    private fun convertToPermissions(githubPermissionString: String): Int {
        return when (val unix = githubPermissionString.takeLast(3)) {
            "755" -> "755".toInt(8) // <-- Apache Commons expects these to be octal numbers. Ugh.
            "644" -> "644".toInt(8) // <-- Apache Commons expects these to be octal numbers. Ugh.
            else -> error("Expected '755' or '644' for permissions, got $unix")
        }
    }


    private suspend fun getDownloadedFiles(
        username: String,
        repoName: String,
        branch: String,
        folderPath: String
    ): Flow<PathAndFileContents> {
        val allFiles = listAllFiles(
            RepoPath(username, repoName, branch, folderPath)
        )
        return channelFlow {
            val sem = Semaphore(20)
            allFiles.collect {
                launch {
                    sem.withPermit<Unit> {
                        println("Getting ${it.path} from ${it.url}")
                        this@channelFlow.send(PathAndFileContents(it.path, downloadFile(it.url), it.permissions))
                    }
                }
            }
        }
    }

    override suspend fun downloadFilesAsZip(path: RepoPath): ByteArray {
        val username = path.user
        val repoName = path.name
        val branch = path.branch
        val folderPath = path.path
        val baos = ByteArrayOutputStream()
        ZipArchiveOutputStream(baos).use { zipOutputStream ->
            coroutineScope {
                getDownloadedFiles(username, repoName, branch, folderPath)
                    .onEach {
                        println("File ${it.path} is downloaded")
                        launch {
                            Mutex().withLock {
                                zipOutputStream.putInZip(it.path, it.byteArray, it.permissions)
                            }
                        }
                    }
                    .flowOn(Dispatchers.IO)
                    .collect()
            }
        }
        return baos.toByteArray()
    }

    private fun ZipArchiveOutputStream.putInZip(path: String, byteArray: ByteArray, permissions: Int) {
        // Note: In the generated zip file, files and folders appear with a '/' at the beginning in the root directory.
        val entry = ZipArchiveEntry(path.replaceFirst("/", "")).apply {
            println("Setting permissions to $permissions")
            unixMode = permissions
        }
        putArchiveEntry(entry)
        write(byteArray)
        closeArchiveEntry()
    }

    override suspend fun getSha(it: RepoPath): String {
        return listFiles(it).sha
    }
}