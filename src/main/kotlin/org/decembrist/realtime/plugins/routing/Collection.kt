package org.decembrist.realtime.plugins.routing

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.decembrist.realtime.plugins.configure.Database.DEFAULT_COLLECTION
import org.decembrist.realtime.service.FetchService
import org.decembrist.realtime.service.FetchService.FetchParams
import org.decembrist.realtime.service.InsertService
import org.decembrist.realtime.service.PathService
import org.decembrist.realtime.service.PathService.Companion.ID_IN_PATH_MESSAGE
import org.decembrist.realtime.service.SearchService

object Collection : Routing("collection") {

    private val searchService by inject<SearchService>()
    private val insertService by inject<InsertService>()
    private val fetchService by inject<FetchService>()
    private val pathService by inject<PathService>()

    @RequestMapping
    private fun findNode(): RouteCallback = {
        get("/{...}") {
            val path = call.cleanPath
            val id = pathService.getId(path)
            val result = if (id == null) {
                searchService.all(path)
            } else {
                searchService.findById(path)
            }
            if (result == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(result)
            }
        }
    }

    @RequestMapping
    private fun saveNode(): RouteCallback = {
        post<Map<String, Any?>>("/{...}") { body ->
            val path = call.cleanPath
            val id = pathService.getId(path)
            if (id == null) {
                var result = insertService.save(path, body)
                result = fetchService.fetch(path, result, call.fetchParams())
                call.respond(result)
            } else {
                pathService.pathError(path, ID_IN_PATH_MESSAGE)
            }
        }
    }

    @RequestMapping
    private fun saveNodeArray(): RouteCallback = {
        post<List<Map<String, Any?>>>("/{...}") { body ->
            val path = call.cleanPath
            val id = pathService.getId(path)
            if (id == null) {
                var result = insertService.save(path, body)
                result = fetchService.fetch(path, result, call.fetchParams())
                call.respond(result)
            } else {
                pathService.pathError(path, ID_IN_PATH_MESSAGE)
            }
        }
    }

    private fun ApplicationCall.fetchParams(): FetchParams = FetchParams(request.queryParameters)

    private val ApplicationCall.cleanPath: String get() = request.uri.substringAfter(parentPath!!)
}


//    routing {
//        // Static plugin. Try to access `/static/index.html`
//        static("/static") {
//            resources("static")
//        }
//    }
