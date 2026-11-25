package io.github.foxx.corridor.core.order

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orders: OrderService
) {

    @PostMapping
    fun create(): ResponseEntity<OrderResponse> {
        val order = orders.create()

        return ResponseEntity
            .created(URI.create("/api/v1/orders/${order.id.value}"))
            .body(OrderResponse.from(order))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): OrderResponse =
        OrderResponse.from(
            orders.get(OrderId(id))
        )

}