package io.sebi.plugins

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.slf4j.helpers.CheckReturnValue
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


private val Tree.isDirectory: Boolean
    get() =
        this.type == "tree"

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

val urlToFileListResponse = mutableMapOf<String, FileListResponse>()

suspend fun getFileListResponse(url: String): FileListResponse {
    return urlToFileListResponse.getOrPut(url) {
        println("Getting $url from network")
//        delay(50)
        myClient.get(url).body<FileListResponse>()
    }
}

suspend fun listFiles(repo: RepoPath): FileListResponse {
    //                       https://api.github.com/repos/twitter/twemoji/git/trees/master
    val urlString = "https://api.github.com/repos/${repo.user}/${repo.name}/git/trees/${repo.branch}"
    val root = getFileListResponse(urlString)
    val directoryContents = repo.path.split("/")
        .filter { it.isNotBlank() }
        .fold(root) { directoryContents, s ->
            val directory: Tree = directoryContents.tree.find { it.path == s }
                ?: throw IllegalArgumentException("Invalid path: ${repo.path}")
            getFileListResponse(directory.url)
        }
    return directoryContents
}


data class PathWithUrl(val path: String, val url: String)

fun listAllFiles(repo: RepoPath): Flow<PathWithUrl> {
    val foo = flow {
        suspend fun walk(currDir: String) {
            for (treeNode in listFiles(repo.copy(path = repo.path + currDir)).tree.sortedBy { it.isDirectory }) {
                println("Looking at ${repo.path} / $currDir / ${treeNode.path} (${treeNode.type})")
                if (treeNode.isDirectory) {
                    walk(currDir + "/" + treeNode.path)
                } else {
                    // file
                    emit(PathWithUrl(currDir + "/" + treeNode.path, treeNode.url))
                }
            }
        }
        walk("")
    }
    return foo
}

suspend fun downloadFile(url: String): ByteArray {
    val file = myClient.get(url).body<SingleFile>()
    val b64string = file.content.replace("\n", "")
    // base64 decode string
    val decodedBytes: ByteArray = Base64.getDecoder().decode(b64string)
    return decodedBytes
}

@Resource("/repo")
data class RepoPath(val user: String, val name: String, val branch: String, val path: String)

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        route("zip") {
            get<RepoPath> { repoPath ->
                call.respondText(listAllFiles(repoPath).toString())
            }
        }
    }
}

data class PathAndFileContents(val path: String, val byteArray: ByteArray)

suspend fun main() {
    val allFiles = listAllFiles(
        RepoPath("JetBrains", "compose-multiplatform", "master", "/examples/imageviewer")
    )

    sequenceOf("a", "b", "c")
    val otherString = "aString"
    val areEqual = "aString" === otherString
    println(areEqual)

    val allDownloadedFiles = channelFlow {
        val sem = Semaphore(20)
        allFiles.collect {
            launch {
                sem.withPermit {
                    send(PathAndFileContents(it.path, downloadFile(it.url)))
                }
            }
        }
    }

    val mutex = Mutex()
    ZipOutputStream(FileOutputStream("output.zip")).use { zipOutputStream ->
        coroutineScope {
            allDownloadedFiles
                .onEach { (path, byteArray) ->
                    println("File $path is downloaded")
                    launch {
                        mutex.withLock {
                            zipOutputStream.putInZip(path, byteArray)
                        }
                    }
                }
                .flowOn(Dispatchers.IO)
                .collect()
        }
    }
}

fun ZipOutputStream.putInZip(path: String, byteArray: ByteArray) {
    val entry = ZipEntry(path)
    putNextEntry(entry)
    write(byteArray)
    closeEntry()
}