package io.github.foxx.corridor.core.outbox

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class OutboxRepository(
    private val jdbcClient: JdbcClient,
) {

    fun append(topic: String, partitionKey: String, payload: String) {
        jdbcClient.sql(
            """
            insert into outbox (topic, partition_key, payload)
            values (:topic, :partitionKey, cast(:payload as jsonb))
            """.trimIndent())
            .param("topic", topic)
            .param("partitionKey", partitionKey)
            .param("payload", payload)
            .update()
    }

}