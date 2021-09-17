package org.decembrist.realtime.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.future.await
import org.decembrist.realtime.TestContainersTest
import org.decembrist.realtime.plugins.configure.Database
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.koin.core.component.inject

internal class SearchServiceTest : TestContainersTest() {

    private val searchService by inject<SearchService>()
    private val pathService by inject<PathService>()
    private val connectionFactory by inject<Database.ConnectionFactory>()

    @ParameterizedTest
    @ValueSource(strings = ["", "/", "table1/table2"])
    fun `should find all`(path: String) = withApplication {
        val data = mapOf(
            "field1" to "value1",
            "field2" to 2,
            "field3" to true,
        )
        with(connectionFactory) {
            val truePath = pathService.defaultIfEmpty(path)
            val table = pathService.pathAsTable(truePath)
            Database.db.tableCreate(table).runAsync().await()
            Database.db.table(table).insert(data).runAsync().await()
        }
        val rows = searchService.all(path)
        rows.size shouldBe 1

        val result = (rows.first() as Map<String, Any>).toMutableMap()
        result.remove("id") shouldNotBe null
        unWrap(result) shouldBe data
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "/", "table1/table2"])
    fun `should find by id`(path: String) = withApplication {
        val data = mapOf(
            "field1" to "value1",
            "field2" to 2,
            "field3" to true,
        )
        val dbData: Map<String, Any> = with(connectionFactory) {
            val truePath = pathService.defaultIfEmpty(path)
            val table = pathService.pathAsTable(truePath)
            Database.db.tableCreate(table).runAsync().await()
            Database.db.table(table).insert(data).changesResult().first().newVal!!
        }
        val path = pathService.joinPaths(path, dbData["id"].toString())
        val result = searchService.findById(path).let { it as Map<String, Any> }.toMutableMap()

        result.remove("id") shouldNotBe null
        unWrap(result) shouldBe data
    }

}