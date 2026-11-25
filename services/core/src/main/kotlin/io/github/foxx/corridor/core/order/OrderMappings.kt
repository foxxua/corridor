package io.github.foxx.corridor.core.order

import io.github.foxx.corridor.contract.order.OrderCreated

internal fun Order.toOrderCreated(): OrderCreated =
    OrderCreated(orderId = id.value, occurredAt = createdAt)