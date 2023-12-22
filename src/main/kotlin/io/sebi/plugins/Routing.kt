package io.sebi.plugins

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import java.io.File
import java.util.*


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

suspend fun listAllFiles(repo: RepoPath): List<PathWithUrl> {
    val foo = buildList {
        suspend fun walk(currDir: String) {
            for (treeNode in listFiles(repo.copy(path = repo.path + currDir)).tree) {
                println("Looking at ${repo.path} / $currDir / ${treeNode.path} (${treeNode.type})")
                if (treeNode.isDirectory) {
                    walk(currDir + "/" + treeNode.path)
                } else {
                    // file
                    add(PathWithUrl(currDir + treeNode.path, treeNode.url))
                }
            }
        }
        walk("")
    }
    println(foo)
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
//            get("/{id...}") {
//                // https://github.com/SebastianAigner/twemoji-amazing/tree/renovate/gradle-8.x/src/main/kotlin
//                // zip/Seb/repo/folder
//                // [Seb, repo, folder]
//                val repoPath = call.parameters.getAll("id") ?: error("Provide")
//                val x = myClient.get("https://jsonplaceholder.typicode.com/todos/1").body<String>()
//                call.respondText(listFiles(repoPath).toString())
//            }
            get<RepoPath> { repoPath ->
                call.respondText(listAllFiles(repoPath).toString())
            }
        }
    }
}

suspend fun main() {
//    val allFiles = listAllFiles(
//        RepoPath("JetBrains", "compose-multiplatform", "master", "/examples/imageviewer")
//    )
//    println(allFiles.joinToString("\n"))
//    println(allFiles.size)
    downloadFile("https://api.github.com/repos/JetBrains/compose-multiplatform/git/blobs/c3bdd530d66b9ed19c5270fa390df26ced44eca5")
}
