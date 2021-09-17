package org.decembrist.realtime.service

import kotlinx.coroutines.future.await
import org.decembrist.realtime.plugins.configure.Database
import org.decembrist.realtime.service.PathService.Companion.ID_REQUIRED_MESSAGE
import org.slf4j.Logger

class SearchService(
    private val pathService: PathService,
    connectionFactory: Database.ConnectionFactory,
    logger: Logger
) : DatabaseService(connectionFactory, logger) {

    suspend fun all(path: String): List<Any> = connectionScope {
        pathService.validate(path, requireEmptyId = true)
        val truePath = pathService.defaultIfEmpty(path)
        val table = pathService.pathAsTable(truePath)
        val tableExists = isTableExists(table)
        if (tableExists) Database.db.table(table).listResult() else emptyList()
    }

    suspend fun findById(path: String) = connectionScope {
        if (pathService.isEmpty(path)) pathService.pathError(path, ID_REQUIRED_MESSAGE)
        pathService.validate(path, requireId = true)
        val truePath = pathService.defaultIfEmpty(path)
        val id = pathService.getId(truePath)!!
        val table = pathService.pathAsTable(truePath, id)
        Database.db.table(table).get(id).runAsync().await().firstOrNull()
    }

}