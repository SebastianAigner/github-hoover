package io.sebi.plugins

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.measureTime


val myClient = HttpClient {
    install(ContentNegotiation) {
        json()
    }
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
    defaultRequest {
        val token = File("key.local").readLines().first().trim()

        headers {
            set("Authorization", "Bearer $token")
        }
    }
}

val urlToFileListResponse = ConcurrentHashMap<String, FileListResponse>()

suspend fun getFileListResponse(url: String): FileListResponse {
    return urlToFileListResponse.getOrPut(url) {
        println("Getting $url from network")
        myClient.get(url).body<FileListResponse>()
    }
}

suspend fun listFiles(repo: RepoPath): FileListResponse {
    val urlString = "https://api.github.com/repos/${repo.user}/${repo.name}/git/trees/${repo.branch}"
    val root = getFileListResponse(urlString)
    val directoryContents = repo.path.split("/")
        .filter { it.isNotBlank() }
        .fold(root) { directoryContents, s ->
            val directory: TreeNode = directoryContents.treeNode.find { it.path == s }
                ?: throw IllegalArgumentException("Invalid path: ${repo.path}")
            getFileListResponse(directory.url)
        }
    return directoryContents
}


data class PathWithUrlAndPermissions(val path: String, val url: String, val permissions: Int)

fun listAllFiles(repo: RepoPath): Flow<PathWithUrlAndPermissions> {
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
fun convertToPermissions(githubPermissionString: String): Int {
    return when (val unix = githubPermissionString.takeLast(3)) {
        "755" -> "755".toInt(8) // <-- Apache Commons expects these to be octal numbers. Ugh.
        "644" -> "644".toInt(8) // <-- Apache Commons expects these to be octal numbers. Ugh.
        else -> error("Expected '755' or '644' for permissions, got $unix")
    }
}

suspend fun downloadFile(url: String): ByteArray {
    val file = myClient.get(url).body<SingleFile>()
    val b64string = file.content.replace("\n", "")
    // base64 decode string
    val decodedBytes: ByteArray = Base64.getDecoder().decode(b64string)
    return decodedBytes
}


class PathAndFileContents(val path: String, val byteArray: ByteArray, val permissions: Int)

suspend fun main() {
    val time = measureTime {
        val allFiles = listAllFiles(
            RepoPath("JetBrains", "compose-multiplatform", "master", "/examples/imageviewer")
        )

        val allDownloadedFiles = channelFlow {
            val sem = Semaphore(20)
            allFiles.collect {
                launch {
                    sem.withPermit<Unit> {
                        this@channelFlow.send(PathAndFileContents(it.path, downloadFile(it.url), it.permissions))
                    }
                }
            }
        }

        val mutex = Mutex()
        ZipArchiveOutputStream(File("output.zip")).use { zipOutputStream ->
            coroutineScope {
                allDownloadedFiles
                    .onEach {
                        println("File ${it.path} is downloaded")
                        launch {
                            mutex.withLock {
                                zipOutputStream.putInZip(it.path, it.byteArray, it.permissions)
                            }
                        }
                    }
                    .flowOn(Dispatchers.IO)
                    .collect()
            }
        }
    }
    println("Completed in $time")
}

fun ZipArchiveOutputStream.putInZip(path: String, byteArray: ByteArray, permissions: Int) {
    val entry = ZipArchiveEntry(path).apply {
        println("Setting permissions to $permissions")
        unixMode = permissions
    }
    putArchiveEntry(entry)
    write(byteArray)
    closeArchiveEntry()
}