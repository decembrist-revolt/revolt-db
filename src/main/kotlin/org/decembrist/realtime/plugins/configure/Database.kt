package org.decembrist.realtime.plugins.configure

import com.rethinkdb.RethinkDB
import com.rethinkdb.ast.ReqlAst
import com.rethinkdb.gen.ast.Db
import com.rethinkdb.model.OptArgs
import com.rethinkdb.net.Connection
import com.rethinkdb.net.Result
import io.ktor.application.*
import io.ktor.util.*
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

val Application.connectionFactory: Database.ConnectionFactory
    get() {
        val attributeKey = Database.attributeKey
        return attributes[attributeKey]
    }

object Database {

    const val DATABASE_URL_PROPERTY = "database.url"

    const val DATABASE_PORT_PROPERTY = "database.port"
    const val DATABASE_NAME_PROPERTY = "database.name"
    const val DEFAULT_COLLECTION = "root"

    val attributeKey = AttributeKey<ConnectionFactory>(ConnectionFactory::class.simpleName!!)

    val rethink = RethinkDB.r

    lateinit var db: Db

    lateinit var connection: Connection

    fun Application.configure() {
        val url = environment.config.propertyOrNull(DATABASE_URL_PROPERTY)?.getString()?.takeIf(String::isNotBlank)
            ?: error("$DATABASE_URL_PROPERTY property required")
        val port = environment.config.propertyOrNull(DATABASE_PORT_PROPERTY)?.getString()?.toInt()
            ?: error("$DATABASE_PORT_PROPERTY property required")
        val database =
            environment.config.propertyOrNull(DATABASE_NAME_PROPERTY)?.getString()?.takeIf(String::isNotBlank)
                ?: error("$DATABASE_NAME_PROPERTY property required")

        val connectionFactory = ConnectionFactory {
            if (!::connection.isInitialized || !connection.isOpen) {
                connection = rethink.connection().hostname(url).port(port).connect()
            }
            return@ConnectionFactory connection
        }

        attributes.put(attributeKey, connectionFactory)

        migrate(connectionFactory, database)
        db = rethink.db(database)
    }

    private fun migrate(factory: ConnectionFactory, database: String): Unit = with(factory) {
        val databases = rethink.dbList().run().first() as List<String>
        if (!databases.contains(database)) {
            rethink.dbCreate(database).run()
        }
    }

    private fun ConnectionFactory.createRootTable(database: String) {
        rethink.db(database).tableCreate(DEFAULT_COLLECTION).run()
    }

    class ConnectionFactory(val connect: () -> Connection) {
        fun ReqlAst.run(): Result<Any> = run(connect())
        fun ReqlAst.runAsync(returnChanges: Boolean = false): CompletableFuture<Result<Any>> =
            runAsync(connect(), OptArgs().with(OptArgs.RETURN_CHANGES.arg, returnChanges))

        suspend fun <T> ReqlAst.singleResult(returnChanges: Boolean = false): T? =
            runAsync(returnChanges).await().firstOrNull() as T?

        suspend fun <T> ReqlAst.listResult(returnChanges: Boolean = false): List<T> =
            runAsync(returnChanges).await().toList() as List<T>

        suspend fun ReqlAst.changesResult(): List<Changes> =
            runAsync(true).await()
                .first()
                .let { it as Map<String, Any> }
                .let { it["changes"] as List<Map<String, Any>> }
                .map(::Changes)
    }

    @JvmInline
    value class Changes(val data: Map<String, Any>) {
        val newVal get() = data["new_val"] as Map<String, Any>?
        val oldVal get() = data["old_val"] as Map<String, Any>?
    }

    enum class OptArgs(val arg: String) {
        RETURN_CHANGES("return_changes"),
    }

}
