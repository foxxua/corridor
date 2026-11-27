package io.github.foxx.corridor.core.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@ConditionalOnProperty(
    prefix = "corridor.outbox",
    name = ["scheduler-enabled"],
    havingValue = "true"
)
class SchedulingConfiguration