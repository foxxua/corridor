package io.github.foxx.corridor.core.outbox

import io.github.foxx.corridor.core.KafkaTestMessages
import io.github.foxx.corridor.core.KafkaTestTopics
import io.github.foxx.corridor.core.KafkaTestcontainersConfiguration
import io.github.foxx.corridor.core.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.simple.JdbcClient
import org.testcontainers.kafka.KafkaContainer
import java.util.UUID

@SpringBootTest
@Import(TestcontainersConfiguration::class, KafkaTestcontainersConfiguration::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OutboxPublisherTest {

    @Autowired
    lateinit var publisher: OutboxPublisher

    @Autowired
    lateinit var outbox: OutboxRepository

    @Autowired
    lateinit var jdbcClient: JdbcClient

    @Autowired
    lateinit var kafka: KafkaContainer

    @BeforeAll
    fun createTopics() {
        KafkaTestTopics.create(kafka.bootstrapServers, OutboxTopics.ORDERS)
    }

    @Test
    fun `publishes row and marks it`() {
        val orderId = UUID.randomUUID()

        outbox.append(
            OutboxTopics.ORDERS,
            orderId.toString(),
            """{"orderId":"$orderId"}"""
        )

        assertThat(publisher.publishBatch()).isGreaterThanOrEqualTo(1)

        val record = KafkaTestMessages.pullOne(
            kafka.bootstrapServers,
            OutboxTopics.ORDERS,
            predicate = { it.key() == orderId.toString() }
        )
        assertThat(record.key()).isEqualTo(orderId.toString())
        assertThat(record.value()).contains(orderId.toString())

        val marked = jdbcClient.sql(
            """
            select count(*) from outbox
            where partition_key = :key and published_at is not null
            """.trimIndent())
            .param("key", orderId.toString())
            .query(Int::class.java)
            .single()

        assertThat(marked).isEqualTo(1)

        assertThat(publisher.publishBatch()).isZero()
    }

}