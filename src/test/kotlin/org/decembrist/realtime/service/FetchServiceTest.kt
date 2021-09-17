package org.decembrist.realtime.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.decembrist.realtime.TestContainersTest
import org.decembrist.realtime.plugins.configure.Database
import org.decembrist.realtime.plugins.configure.Database.DEFAULT_COLLECTION
import org.decembrist.realtime.service.FetchService.FetchParams.Companion.FETCH
import org.decembrist.realtime.service.InsertService.Companion.ID_SUFFIX
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.koin.core.component.inject

internal class FetchServiceTest : TestContainersTest() {

    private val fetchService by inject<FetchService>()
    private val pathService by inject<PathService>()
    private val connectionFactory by inject<Database.ConnectionFactory>()
    private val insertService by inject<InsertService>()

    @ParameterizedTest
    @ValueSource(strings = [DEFAULT_COLLECTION, "table1/table2"])
    fun `should fetch data 1 deep`(path: String) = withApplication {
        val nestedFieldName = "field4"
        val data = mutableMapOf(
            "field1" to "value1",
            "field2" to 2,
            "field3" to true,
            nestedFieldName to mutableMapOf(
                "field1" to "value1",
                "field2" to 2,
                "field3" to true,
                nestedFieldName to mapOf(
                    "field1" to "value1",
                    "field2" to 2,
                    "field3" to true,
                ),
            ),
        )
        val dbData = insertService.save(path, data).let(::unWrap)
        dbData[nestedFieldName] shouldBe null
        val fetched = fetchService.fetch(path, dbData, FetchService.FetchParams(mapOf(FETCH to 1)))
            .let(::unWrap)
            .toMutableMap()
        fetched.remove("id") shouldNotBe null
        val fetchedNested1 = unWrap(fetched[nestedFieldName]!!).toMutableMap()
        fetched[nestedFieldName] = fetchedNested1
        fetchedNested1.remove("id") shouldNotBe null
        //remove field4-id
        fetchedNested1.remove("$nestedFieldName$ID_SUFFIX") shouldNotBe null
        fetchedNested1[nestedFieldName] shouldBe null
        (data[nestedFieldName] as MutableMap<String, Any>).remove(nestedFieldName) shouldNotBe null
        fetched shouldBe data
    }

    @ParameterizedTest
    @ValueSource(strings = [DEFAULT_COLLECTION, "table1/table2"])
    fun `should fetch data 2 deep`(path: String) = withApplication {
        val nestedFieldName = "field4"
        val data = mutableMapOf(
            "field1" to "value1",
            "field2" to 2,
            "field3" to true,
            nestedFieldName to mapOf(
                "field1" to "value1",
                "field2" to 2,
                "field3" to true,
                nestedFieldName to mapOf(
                    "field1" to "value1",
                    "field2" to 2,
                    "field3" to true,
                ),
            ),
        )
        val dbData = insertService.save(path, data).let(::unWrap)
        dbData[nestedFieldName] shouldBe null
        val fetched = fetchService.fetch(path, dbData, FetchService.FetchParams(mapOf(FETCH to 2)))
            .let(::unWrap)
            .toMutableMap()
        fetched.remove("id") shouldNotBe null
        val fetchedNested1 = unWrap(fetched[nestedFieldName]!!).toMutableMap()
        fetchedNested1.remove("id") shouldNotBe null
        fetched[nestedFieldName] = fetchedNested1
        val fetchedNested2 = unWrap(fetchedNested1[nestedFieldName]!!).toMutableMap()
        fetchedNested2.remove("id") shouldNotBe null
        fetchedNested1[nestedFieldName] = fetchedNested2
        fetched shouldBe data
    }
}