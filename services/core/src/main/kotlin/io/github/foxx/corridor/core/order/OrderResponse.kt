package io.github.foxx.corridor.core.order

import java.time.format.DateTimeFormatter
import java.util.UUID

data class OrderResponse(
    val id: UUID,
    val status: String,
    val createdAt: String,
) {

    companion object {
        fun from(order: Order) = OrderResponse(
            id = order.id.value,
            status = order.status.name,
            createdAt = DateTimeFormatter.ISO_INSTANT.format(order.createdAt),
        )
    }

}