package io.github.foxx.corridor.core

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.UUID

object KafkaTestMessages {

    fun pullOne(
        bootstrapServers: String,
        topic: String,
        timeout: Duration = Duration.ofSeconds(10),
        predicate: (ConsumerRecord<String, String>) -> Boolean = { true }
    ): ConsumerRecord<String, String> {
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "test-${UUID.randomUUID()}",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        )

        KafkaConsumer<String, String>(props).use { consumer ->
            consumer.subscribe(listOf(topic))
            val deadline = System.nanoTime() + timeout.toNanos()

            while (System.nanoTime() < deadline) {
                for (record in consumer.poll(Duration.ofSeconds(10))) {
                    if (predicate(record)) return record
                }
            }

            error("no record in $topic withing $timeout")
        }
    }

}