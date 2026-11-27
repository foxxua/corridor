package io.github.foxx.corridor.core.order

import io.github.foxx.corridor.core.outbox.OutboxRepository
import io.github.foxx.corridor.core.outbox.OutboxTopics
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.util.UUID

@Service
class OrderService(
    private val orders: OrderRepository,
    private val outbox: OutboxRepository,
    private val json: JsonMapper,
    private val clock: Clock,
) {

    @Transactional
    fun create(): Order {
        val order = Order(
            id = OrderId(UUID.randomUUID()),
            status = OrderStatus.CREATED,
            createdAt = clock.instant(),
        )
        orders.insert(order)

        val event = order.toOrderCreated()
        outbox.append(
            topic = OutboxTopics.ORDERS,
            partitionKey = event.orderId.toString(),
            payload = json.writeValueAsString(event),
        )

        return order
    }

    @Transactional(readOnly = true)
    fun get(id: OrderId): Order =
        orders.findById(id) ?: throw OrderNotFoundException(id)

}