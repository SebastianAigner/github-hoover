package io.sebi.plugins

import io.ktor.client.*
import io.ktor.client.call.*
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


val myClient = HttpClient {
    install(ContentNegotiation) {
        json()
    }
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
}

suspend fun listFiles(repo: RepoPath): FileListResponse {
    //                       https://api.github.com/repos/twitter/twemoji/git/trees/master
    val root = myClient.get("https://api.github.com/repos/${repo.user}/${repo.name}/git/trees/${repo.branch}").body<FileListResponse>()
    var directoryContents = root
    for(element in repo.path.split("/")) {
        if(element.isEmpty()) continue
        val directory: Tree = directoryContents.tree.find { it.path == element } ?: throw IllegalArgumentException("Invalid path: ${repo.path}")
        directoryContents = myClient.get(directory.url).body<FileListResponse>()
    }
    return directoryContents
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
                call.respondText(listFiles(repoPath).toString())
            }
        }
    }
}
