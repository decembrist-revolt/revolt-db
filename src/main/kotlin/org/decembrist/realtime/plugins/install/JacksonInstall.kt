package org.decembrist.realtime.plugins.install

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.util.*

val Application.objectMapper: ObjectMapper get() {
    val attributeKey = JacksonInstall.attributeKey
    return attributes[attributeKey]
}

object JacksonInstall: Install {

    val attributeKey = AttributeKey<ObjectMapper>(ObjectMapper::class.simpleName!!)

    override fun Application.install() = install(ContentNegotiation) {
        jackson {
            attributes.put(attributeKey, this)
        }
    }

}