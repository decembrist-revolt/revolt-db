package org.decembrist.realtime.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.decembrist.realtime.TestContainersTest
import org.decembrist.realtime.plugins.configure.Database
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.koin.core.component.inject

internal class InsertServiceTest : TestContainersTest() {

    private val insertService by inject<InsertService>()
    private val connectionFactory by inject<Database.ConnectionFactory>()
    private val objectMapper by inject<ObjectMapper>()
    private val pathService by inject<PathService>()

    @ParameterizedTest
    @ValueSource(strings = ["", "/", "table1/table2"])
    fun `save data 0 deep`(path: String) = withApplication {
        val data = mapOf(
            "field1" to "value1",
            "field2" to 2,
            "field3" to true,
        )
        val result = insertService.save(path, data).toMutableMap()
        result.remove("id") shouldNotBe null
        val resultString = objectMapper.writeValueAsString(result)
        objectMapper.readValue<Map<String, Any>>(resultString) shouldBe data

        val rootRows = with(connectionFactory) {
            val truePath = pathService.defaultIfEmpty(path)
            val table = pathService.pathAsTable(truePath)
            Database.db.table(table).listResult<Map<String, Any>>()
        }
        rootRows.size shouldBe 1
        val dbData = rootRows.first().toMutableMap()
        dbData.remove("id") shouldNotBe null
        unWrap(result) shouldBe data
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "/", "table1/table2"])
    fun `save data 1 deep`(path: String) = withApplication {
        val data = mutableMapOf(
            "field1" to "value1",
            "field2" to 2,
            "field3" to true,
            "field4" to mapOf(
                "field1" to "value1",
                "field2" to 2,
                "field3" to true,
            ),
        )
        val result = insertService.save(path, data).toMutableMap()
        result.remove("id") shouldNotBe null
        result.remove("field4-id") shouldNotBe null
        val deep1Data = data.remove("field4")
        val resultString = objectMapper.writeValueAsString(result)
        objectMapper.readValue<Map<String, Any>>(resultString) shouldBe data

        var rootRows = with(connectionFactory) {
            val truePath = pathService.defaultIfEmpty(path)
            val table = pathService.pathAsTable(truePath)
            Database.db.table(table).listResult<Map<String, Any>>()
        }
        rootRows.size shouldBe 1
        var dbData = rootRows.first().toMutableMap()
        dbData.remove("id") shouldNotBe null
        unWrap(result) shouldBe data

        rootRows = with(connectionFactory) {
            val truePath = pathService.defaultIfEmpty(path)
            val table = pathService.pathAsTable(truePath)
            Database.db.table("${table}_field4").listResult()
        }
        rootRows.size shouldBe 1
        dbData = rootRows.first().toMutableMap()
        dbData.remove("id") shouldNotBe null
        unWrap(result) shouldBe deep1Data
    }

    @Test
    fun update() {
    }

    @Test
    fun delete() {
    }
}