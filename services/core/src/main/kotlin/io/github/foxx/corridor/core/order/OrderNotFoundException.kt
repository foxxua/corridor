package io.github.foxx.corridor.core.order

class OrderNotFoundException(
    id: OrderId,
) : RuntimeException("Order with id ${id.value} was not found")