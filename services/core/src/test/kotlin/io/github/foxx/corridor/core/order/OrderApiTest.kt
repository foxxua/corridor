package io.github.foxx.corridor.core.order

import com.jayway.jsonpath.JsonPath
import io.github.foxx.corridor.core.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@Transactional
class OrderApiTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var jdbcClient: JdbcClient

    @Test
    fun `creates and reads order`() {
        val body = mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andReturn()
            .response
            .contentAsString

        val id: String = JsonPath.read(body, "$.id")

        val event = jdbcClient.sql(
            """
            select payload->>'orderId' as order_id, payload->>'occurredAt' as occurred_at
            from outbox
            where partition_key = :key
            """.trimIndent())
            .param("key", id)
            .query()
            .singleRow()
        assertThat(event["order_id"]).isEqualTo(id)
        assertThat(Instant.parse(event["occurred_at"] as String)).isNotNull()

        mvc.perform(get("/api/v1/orders/{id}", id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.status").value("CREATED"))
    }

    @Test
    fun `returns 404 for unknown order`() {
        mvc.perform(get("/api/v1/orders/{id}", UUID.randomUUID()))
            .andExpect(status().isNotFound)
    }

}