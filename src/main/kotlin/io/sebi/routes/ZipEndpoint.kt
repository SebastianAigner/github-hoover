package io.sebi.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sebi.domain.repository.GitHubFolderDownloaderRepository
import java.io.File
import java.nio.file.Paths


fun Route.downloadZipEndpoint(repository: GitHubFolderDownloaderRepository) {

    route("/download-zip"){
        get("/{owner}/{repository}") {

            val username = call.parameters["owner"] ?: return@get call.respondText("missing owner parameter")
            val repoName = call.parameters["repository"] ?: return@get call.respondText("missing repository parameter")
            val branch = call.request.queryParameters["branch"] ?: return@get call.respondText("missing branch parameter")
            val folderPath = call.request.queryParameters["folder"] ?: return@get call.respondText("missing folder parameter")

            val outputPath = getTempDirectoryPath().plus(File.separator).plus(getLastPartOfPath(folderPath))
            repository.zipDownloadedFiles(outputPath, username, repoName, branch, folderPath)


            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(ContentDisposition.Parameters.FileName, getLastPartOfPath(folderPath))
                    .toString()
            )
            call.respondFile(File(outputPath))

            // get<RepoPath> { repoPath ->
            //  call.respondText(repository.listAllFiles(repoPath).toString())
            //  }
        }
    }
}

fun getTempDirectoryPath(): String {
    val resourcePath = Paths.get("src/main/resources/temp")
    return resourcePath.toAbsolutePath().toString()
}
fun getLastPartOfPath(path: String): String {
    val normalizedPath = Paths.get(path).normalize()
    return normalizedPath.fileName.toString().plus(".zip")
}