package org.decembrist.realtime.service

import org.decembrist.realtime.DataException

class DataService {

    fun validate(data: Map<String, Any?>, deep: Boolean = false) {
        fun validate(propName: String) {
            if (propName.contains("-")) dataError(data, DASH_IN_KEY_MESSAGE.format(propName))
        }
        data.forEach { (key, value) ->
            validate(key)
            if (deep && value is Map<*, *>) {
                validate(value as Map<String, Any?>, deep)
            }
        }
    }

    fun asDataOrNull(obj: Any?): Map<String, Any?>? = obj?.takeIf { obj is Map<*, *> }?.let { it as Map<String, Any?> }

    fun dataError(data: String?, message: String): Nothing = throw DataException(data, message)

    companion object {
        const val DATA_IS_NULL_MESSAGE = "data is null"
        const val DASH_IN_KEY_MESSAGE = "'-' in key %s"
    }

}