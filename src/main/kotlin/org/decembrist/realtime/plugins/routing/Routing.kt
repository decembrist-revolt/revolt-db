package org.decembrist.realtime.plugins.routing

import io.ktor.application.*
import io.ktor.routing.*
import org.koin.ktor.ext.inject
import kotlin.reflect.full.findAnnotation

typealias RouteCallback = Route.() -> Unit

abstract class Routing(val parentPath: String? = null) {

    protected lateinit var application: Application

    protected inline fun <reified T : Any> inject(): Lazy<T> = application.inject<T>()

    open fun Application.routing() {
        val routes = this::class.members
            .filter { it.parameters.isEmpty() }
            .filter { it.findAnnotation<RequestMapping>() != null }
            .map { it.call(this@Routing) }
            .map { it as RouteCallback }
        application = this
        routing {
            if (parentPath != null) {
                route(parentPath) {
                    applyRoutes(routes)
                }
            } else {
                applyRoutes(routes)
            }
        }
    }

    private fun Route.applyRoutes(routes: List<RouteCallback>) = routes.forEach { it.invoke(this) }

    annotation class RequestMapping

}