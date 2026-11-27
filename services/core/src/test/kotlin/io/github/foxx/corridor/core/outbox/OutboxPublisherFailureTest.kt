package io.github.foxx.corridor.core.outbox

import io.github.foxx.corridor.core.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.willThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.UUID

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class OutboxPublisherFailureTest {

    @Autowired
    lateinit var publisher: OutboxPublisher

    @Autowired
    lateinit var outbox: OutboxRepository

    @Autowired
    lateinit var jdbcClient: JdbcClient

    @MockitoBean
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @BeforeEach
    fun cleanOutbox() {
        jdbcClient.sql("truncate outbox").update()
    }

    @Test
    fun `keeps row unpublished when send fails`() {
        val orderId = UUID.randomUUID()
        outbox.append(
            OutboxTopics.ORDERS,
            orderId.toString(),
            """{"orderId":"$orderId"}"""
        )

        willThrow(IllegalStateException("broken down"))
            .given(kafkaTemplate)
            .send(anyString(), anyString(), anyString())

        assertThatThrownBy { publisher.publishBatch() }
            .hasMessage("broken down")

        val unpublishedCount = jdbcClient.sql(
            """
            select count(*) from outbox
            where partition_key = :key and published_at is null
            """.trimIndent())
            .param("key", orderId.toString())
            .query(Int::class.java)
            .single()

        assertThat(unpublishedCount).isEqualTo(1)
    }

}