package io.github.foxx.corridor.core.order

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
class OrderService(
    private val orders: OrderRepository,
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
        return order
    }

    @Transactional(readOnly = true)
    fun get(id: OrderId): Order =
        orders.findById(id) ?: throw OrderNotFoundException(id)

}