package io.github.foxx.corridor.core.order

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class OrderRepository(
    private val jdbcClient: JdbcClient,
) {

    fun insert(order: Order) {
        jdbcClient.sql(
            """
            insert into orders (id, status, created_at)
            values (:id, :status, :createdAt)
            """.trimIndent())
            .param("id", order.id.value)
            .param("status", order.status.name)
            .param("createdAt", OffsetDateTime.ofInstant(order.createdAt, ZoneOffset.UTC))
            .update()
    }

    fun findById(id: OrderId): Order? =
        jdbcClient.sql(
            """
            select id, status, created_at
            from orders
            where id = :id
            """.trimIndent())
            .param("id", id.value)
            .query { rs, _ ->
                Order(
                    id = OrderId(rs.getObject("id", UUID::class.java)),
                    status = OrderStatus.valueOf(rs.getString("status")),
                    createdAt = rs.getObject("created_at", OffsetDateTime::class.java).toInstant()
                )
            }
            .optional()
            .orElse(null)

}