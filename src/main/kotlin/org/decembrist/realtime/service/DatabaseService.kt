package org.decembrist.realtime.service

import kotlinx.coroutines.future.await
import org.decembrist.realtime.plugins.configure.Database
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class DatabaseService(private val connectionFactory: Database.ConnectionFactory,
                               protected val logger: Logger) {

    private val tables: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

    suspend fun Database.ConnectionFactory.isTableExists(table: String, create: Boolean = false): Boolean {
        val tableInCache = tables.contains(table)
        val tableExists = tableInCache || Database.db.tableList().contains(table).singleResult()!!
        return if (!tableExists && create) {
            Database.db.tableCreate(table).runAsync().await()
            tables.add(table)
            true
        } else if (!tableInCache && tableExists) {
            tables.add(table)
            true
        } else tableExists
    }

    suspend fun <T> connectionScope(block: suspend Database.ConnectionFactory.() -> T) =
        with(connectionFactory) { block() }

}