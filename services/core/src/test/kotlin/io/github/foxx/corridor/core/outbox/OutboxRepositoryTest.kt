package io.github.foxx.corridor.core.outbox

import io.github.foxx.corridor.core.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.simple.JdbcClient
import java.time.OffsetDateTime
import java.util.UUID

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
@Import(TestcontainersConfiguration::class, OutboxRepository::class)
class OutboxRepositoryTest {

    @Autowired
    lateinit var outbox: OutboxRepository

    @Autowired
    lateinit var jdbcClient: JdbcClient

    @Test
    fun `stores unpublished event with json payload`() {
        val orderId = UUID.randomUUID()

        val payload =
            """
            {"orderId":"$orderId"}
            """.trimIndent()

        outbox.append(OutboxTopics.ORDERS, orderId.toString(), payload)

        val row = jdbcClient.sql(
            """
            select topic, partition_key, payload->>'orderId' as orderId, published_at
            from outbox
            """.trimIndent())
            .query().singleRow()

        assertThat(row["topic"]).isEqualTo(OutboxTopics.ORDERS)
        assertThat(row["partition_key"]).isEqualTo(orderId.toString())
        assertThat(row["orderId"]).isEqualTo(orderId.toString())
        assertThat(row["published_at"]).isNull()
    }

}