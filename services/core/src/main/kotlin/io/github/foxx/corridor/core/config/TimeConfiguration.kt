package io.github.foxx.corridor.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit

@Configuration
class TimeConfiguration {

    @Bean
    fun clock(): Clock = Clock.tick(Clock.systemUTC(), Duration.of(1, ChronoUnit.MICROS))

}
