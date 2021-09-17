package org.decembrist.realtime.service

import org.decembrist.realtime.PathException
import org.decembrist.realtime.plugins.configure.Database.DEFAULT_COLLECTION

class PathService {

    fun pathAsTable(path: String, id: String? = null): String = path.split("/")
        .filterNot { it == id }
        .filterNot { it.isBlank() }
        .joinToString("_")

    fun joinPaths(path: String, segment: String) = "$path/$segment"

    fun getId(path: String): String? = path.removeSuffix("/").split("/").last().takeIf { it.contains("-") }

    fun removePrefix(path: String, prefix: String) = path.substringAfter(prefix)

    /**
     * ""               -> /default
     * /path            -> /path
     * /id-123          -> /default/id-123
     * /path/id-123     -> /path/id-123
     */
    fun defaultIfEmpty(path: String): String {
        val id = getId(path) ?: ""
        val truePath = if (isEmpty(path.substringBeforeLast(id))) DEFAULT_COLLECTION else path
        return joinPaths(truePath, id)
    }

    fun isEmpty(path: String) = path.isBlank() || path.all { it == '/'}

    fun validate(
        path: String,
        requireEmptyId: Boolean = false,
        requireId: Boolean = false
    ) {
        if (path == DEFAULT_COLLECTION && !requireId) return
        if (path.contains(" ")) pathError(path, "space in path")
        val id = getId(path)
        val strictPath = if (id != null) {
            if (requireEmptyId) pathError(path, ID_IN_PATH_MESSAGE)
            path.substringBeforeLast("/")
        } else if (requireId.not()) {
            path
        } else {
            pathError(path, ID_REQUIRED_MESSAGE)
        }
        if (strictPath.contains("-")) pathError(path, "'-' in path")
        if (strictPath.any { it.isUpperCase() }) pathError(path, "upper case letter in path")
    }

    fun pathError(path: String, message: String): Nothing = throw PathException(path, message)

    companion object {
        const val ID_REQUIRED_MESSAGE = "id required"
        const val ID_IN_PATH_MESSAGE = "id in path"
    }

}