package io.github.foxx.corridor.core

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.errors.TopicExistsException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

object KafkaTestTopics {

    fun create(
        bootstrapServers: String,
        topic: String,
        partitions: Int = 3,
        replicationFactor: Short = 1,
    ) {
        AdminClient.create(
            mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,)
        ).use { admin ->
            try {
                admin.createTopics(listOf(NewTopic(topic, partitions, replicationFactor)))
                    .all()
                    .get(30, TimeUnit.SECONDS)
            } catch (e: ExecutionException) {
                if (e.cause !is TopicExistsException) throw e
            }
        }
    }

}