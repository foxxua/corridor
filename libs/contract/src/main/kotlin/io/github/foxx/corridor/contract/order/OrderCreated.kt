package io.github.foxx.corridor.contract.order

import java.time.Instant
import java.util.UUID

data class OrderCreated(
    val orderId: UUID,
    val occurredAt: Instant,
)
