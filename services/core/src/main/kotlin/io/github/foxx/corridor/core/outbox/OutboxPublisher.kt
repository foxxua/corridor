package io.github.foxx.corridor.core.outbox

import io.github.foxx.corridor.contract.order.OrderCreated
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Component
class OutboxPublisher(
    private val jdbcClient: JdbcClient,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {

    @Transactional
    fun publishBatch(limit: Int = 50): Int {
        val rows = jdbcClient.sql(
            """
            select id, topic, partition_key, payload
            from outbox
            where published_at is null
            order by id
            limit :limit
            for update skip locked
            """.trimIndent())
            .param("limit", limit)
            .query { rs, _ ->
                OutboxRow(
                    id = rs.getLong("id"),
                    topic = rs.getString("topic"),
                    partitionKey = rs.getString("partition_key"),
                    payload = rs.getString("payload")
                )
            }
            .list()

        rows.forEach { row ->
            kafkaTemplate.send(
                row.topic,
                row.partitionKey,
                row.payload
            ).get(5, TimeUnit.SECONDS)

            jdbcClient.sql("update outbox set published_at = now() where id = :id")
                .param("id", row.id)
                .update()
        }

        return rows.size
    }

}

data class OutboxRow(
    val id: Long,
    val topic: String,
    val partitionKey: String,
    val payload: String,
)