package org.decembrist.realtime.service

import kotlinx.coroutines.future.await
import org.decembrist.realtime.DbException
import org.decembrist.realtime.plugins.configure.Database
import org.decembrist.realtime.service.DataService.Companion.DATA_IS_NULL_MESSAGE
import org.slf4j.Logger
import java.util.*

class InsertService(
    private val dataService: DataService,
    private val pathService: PathService,
    connectionFactory: Database.ConnectionFactory,
    logger: Logger
) : DatabaseService(connectionFactory, logger) {

    suspend fun save(path: String, data: Map<String, Any?>?): Map<String, Any?> = connectionScope {
        if (data == null) dataService.dataError(data, DATA_IS_NULL_MESSAGE)
        if (pathService.isEmpty(path)) pathService.pathError(path, PROPERTY_NOT_FOUND.format(path))
        val truePath = pathService.defaultIfEmpty(path)
        pathService.validate(truePath, requireEmptyId = true)
        dataService.validate(data, deep = true)
        saveRecursively(truePath, data)
    }

    suspend fun save(path: String, data: List<Map<String, Any?>>): Map<String, Any?> = connectionScope {
        if (data.isEmpty()) dataService.dataError("[]", DATA_IS_NULL_MESSAGE)
        if (pathService.isEmpty(path)) pathService.pathError(path, PROPERTY_NOT_FOUND.format(path))
        val truePath = pathService.defaultIfEmpty(path)
        pathService.validate(truePath, requireEmptyId = true)
        data.forEach { dataService.validate(it, deep = true) }
        saveRecursively(truePath, data)
    }

    suspend fun update(path: String, data: Map<String, Any?>?, id: String) {
        pathService.validate(path, requireId = true)
        val id = pathService.getId(path)
    }

    suspend fun delete(path: String, id: String) = connectionScope {
        if (pathService.isEmpty(path)) pathService.pathError(path, "collection not found")
        val table = pathService.pathAsTable(path)
        if (!isTableExists(table)) throw DbException(mapOf(ID to id), "path [$path] is not exists")
        Database.db.table(table).get(id).delete().runAsync().await()
    }

    private suspend fun saveRecursively(path: String, data: Map<String, Any?>): Map<String, Any> = connectionScope {
        val subObjects = mutableMapOf<String, Map<String, Any>>()
        val newData = mutableMapOf<String, Any?>()
        for ((key, value) in data.entries) {
            if (value is MutableMap<*, *>) {
                val id = UUID.randomUUID().toString()
                val newValue = value.toMutableMap()
                newValue[ID] = id
                val newPath = pathService.joinPaths(path, key)
                subObjects[newPath] = newValue as Map<String, Any>
                val idField = key + ID_SUFFIX
                newData[idField] = id
            } else {
                newData[key] = value
            }
        }
        if (saveSubObjects(subObjects)) {
            val table = pathService.pathAsTable(path)
            isTableExists(table, create = true)
            val changes = Database.db.table(table).insert(newData).changesResult().first()
            changes.newVal!!
        } else {
            throw DbException(data, "sub object insert failed")
        }
    }

    /**
     * @return save result
     */
    private suspend fun saveSubObjects(subObjects: Map<String, Map<String, Any>>): Boolean {
        val savedIds = mutableListOf<String>()
        for ((path, data) in subObjects) {
            try {
                saveRecursively(path, data)
                savedIds.add(data[ID] as String)
            } catch (ex: Exception) {
                logger.error("saveRecursively error for path: [$path] and data: [$data], message: ${ex.message}")
                savedIds.forEach { id -> delete(path, id) }
                return false
            }
        }
        return true
    }

    companion object {
        const val ID = "id"
        const val ID_SUFFIX = "-id"
        const val PROPERTY_NOT_FOUND = "property not found, '%s/property'"
    }

}