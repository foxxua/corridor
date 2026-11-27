package io.github.foxx.corridor.core

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class KafkaTestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun kafka(): KafkaContainer =
        KafkaContainer(DockerImageName.parse("apache/kafka:4.4.0"))

}