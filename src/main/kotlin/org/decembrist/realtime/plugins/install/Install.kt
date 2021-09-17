package org.decembrist.realtime.plugins.install

import io.ktor.application.*

interface Install {

    fun Application.install(): Any

}