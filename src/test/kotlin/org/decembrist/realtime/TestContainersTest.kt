package org.decembrist.realtime

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rethinkdb.gen.ast.Javascript
import com.rethinkdb.gen.ast.ReqlFunction0
import com.rethinkdb.gen.ast.ReqlFunction1
import io.kotest.matchers.shouldBe
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.decembrist.realtime.plugins.configure.Database
import org.decembrist.realtime.plugins.configure.Database.DATABASE_PORT_PROPERTY
import org.decembrist.realtime.plugins.configure.Database.DATABASE_URL_PROPERTY
import org.decembrist.realtime.plugins.install.JacksonInstall
import org.decembrist.realtime.plugins.install.KoinInstall
import org.junit.jupiter.api.AfterEach
import org.koin.core.KoinApplication
import org.koin.core.component.inject
import org.koin.ktor.ext.Koin
import org.koin.test.KoinTest
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
abstract class TestContainersTest: KoinTest {

    private val objectMapper by inject<ObjectMapper>()

    @Container
    var rethink: GenericContainer<*> = GenericContainer<GenericContainer<Nothing>>(
        DockerImageName.parse("rethinkdb:2.4.1")
    ).withExposedPorts(RETHINK_DB_DEFAULT_PORT)

    private val testEnv by lazy {
        createTestEnvironment {
            (config as MapApplicationConfig).apply {
                put(DATABASE_URL_PROPERTY, rethink.host)
                put(DATABASE_PORT_PROPERTY, rethink.getMappedPort(RETHINK_DB_DEFAULT_PORT).toString())
                put(Database.DATABASE_NAME_PROPERTY, "root")
            }
            modules.add { with(Database) { configure() } }
            modules.add { with(KoinInstall) { install() } }
            modules.add { with(JacksonInstall) { install() } }
        }
    }

    fun withApplication(block: suspend TestApplicationEngine.() -> Unit) = withApplication(testEnv) {
        runBlocking {
            block()
        }
    }

    @AfterEach
    fun tearDown() = withApplication {
        val connection = Database.rethink.connection()
            .hostname(rethink.host).port(rethink.getMappedPort(RETHINK_DB_DEFAULT_PORT)).connect()
        Database.db.tableList().forEach(ReqlFunction1 { Database.db.tableDrop(it) }).run(connection)
    }

    protected fun unWrap(data: Any): Map<String, Any> {
        val dbResultString = objectMapper.writeValueAsString(data as Map<String, Any>)
        return objectMapper.readValue(dbResultString)
    }

    companion object {
        const val RETHINK_DB_DEFAULT_PORT = 28015
    }

}