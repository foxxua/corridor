package io.github.foxx.corridor.core.outbox

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "corridor.outbox",
    name = ["scheduler-enabled"],
    havingValue = "true"
)
class OutboxRelay(
    private val publisher: OutboxPublisher
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "PT0.5S")
    fun tick() {
        try {
            val published = publisher.publishBatch()
            if (published > 0) log.info("published {} outbox row(s)", published)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.warn("outbox publish interrupted", e)
        } catch (e: Exception) {
            log.warn("outbox publish failed, retrying on next tick", e)
        }
    }

}