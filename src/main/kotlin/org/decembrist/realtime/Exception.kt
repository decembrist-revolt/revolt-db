package org.decembrist.realtime

import org.decembrist.realtime.plugins.install.ServerException
import org.decembrist.realtime.plugins.install.UserException

class PathException(path: String, message: String): UserException("Path [$path] validation error: $message")

class DataException(data: String?, message: String): UserException("data error for [$data]: $message")

class DbException(data: Map<String, Any?>?, message: String): ServerException("insert failed for [$data]: $message")