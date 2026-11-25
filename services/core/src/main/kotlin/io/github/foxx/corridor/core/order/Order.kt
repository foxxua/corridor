package io.github.foxx.corridor.core.order

import java.time.Instant

data class Order(
    val id: OrderId,
    val status: OrderStatus,
    val createdAt: Instant,
)
