package org.decembrist.realtime.plugins.install

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*

object StatusPagesInstall {

    fun Application.install() = install(StatusPages) {
        exception<UserException> { ex -> call.respond(HttpStatusCode.BadRequest, ex.message!!) }
    }

}

open class UserException(message: String): RuntimeException(message)

open class ServerException(message: String): RuntimeException(message)