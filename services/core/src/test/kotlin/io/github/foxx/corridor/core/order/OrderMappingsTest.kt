package io.github.foxx.corridor.core.order

import io.github.foxx.corridor.contract.order.OrderCreated
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class OrderMappingsTest {

    @Test
    fun `keeps order id and timestamp`() {
        val id = OrderId(UUID.randomUUID())
        val createdAt = Instant.parse("2026-11-25T09:00:00Z")
        val order = Order(id, OrderStatus.CREATED, createdAt)

        assertThat(OrderCreated(id.value, createdAt))
            .isEqualTo(order.toOrderCreated())
    }

}