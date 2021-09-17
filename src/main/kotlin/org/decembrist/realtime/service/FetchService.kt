package org.decembrist.realtime.service

import io.ktor.http.*
import org.decembrist.realtime.plugins.configure.Database
import org.decembrist.realtime.service.InsertService.Companion.ID_SUFFIX
import org.slf4j.Logger

class FetchService(
    private val dataService: DataService,
    private val pathService: PathService,
    connectionFactory: Database.ConnectionFactory,
    logger: Logger
) :
    DatabaseService(connectionFactory, logger) {

    suspend fun fetch(path: String, data: Map<String, Any?>, fetchParams: FetchParams) = connectionScope {
        val fetch = fetchParams.fetch
        if (fetch > 0) {
            val idFields = data.keys.filter { it.endsWith(ID_SUFFIX) }
            val result = if (idFields.isNotEmpty()) {
                val objectFields = data.flatMap { (key, value) ->
                    if (key in idFields) {
                        val field = key.substringBefore(ID_SUFFIX)
                        val newPath = pathService.joinPaths(path, field)
                        val table = pathService.pathAsTable(newPath)
                        listOf(field, Database.db.table(table).get(value))
                    } else {
                        listOf(key, value)
                    }
                }
                Database.rethink.`object`(*objectFields.toTypedArray()).singleResult<Map<String, Any?>>()!!
            } else data
            fetchSubObjects(path, result, fetchParams)
        } else data
    }

    private suspend fun fetchSubObjects(
        path: String,
        data: Map<String, Any?>,
        fetchParams: FetchParams,
    ): Map<String, Any?> = data.map { (key, value) ->
        val nestedData = dataService.asDataOrNull(value)
        if (nestedData != null) {
            val newPath = pathService.joinPaths(path, key)
            key to fetch(newPath, nestedData, fetchParams.copy(fetchParams.fetch - 1))
        } else {
            key to value
        }
    }.toMap()

    @JvmInline
    value class FetchParams(val paramsMap: Map<String, Int>) {
        constructor(queryParams: Parameters) : this(
            mapOf(
                FETCH to (queryParams["fetch"]?.toIntOrNull() ?: 0),
            )
        )

        val fetch: Int get() = paramsMap[FETCH] as Int

        fun copy(fetch: Int) = FetchParams(paramsMap + mapOf(FETCH to fetch))

        companion object {
            const val FETCH = "fetch"
        }
    }

}