package io.github.foxx.corridor.core.order

import io.github.foxx.corridor.core.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest
import org.springframework.context.annotation.Import
import java.time.Instant
import java.util.UUID

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
@Import(TestcontainersConfiguration::class, OrderRepository::class)
class OrderRepositoryTest {

    @Autowired
    lateinit var orders: OrderRepository

    @Test
    fun `stores and reads order`() {
        val order = Order(
            id = OrderId(UUID.randomUUID()),
            status = OrderStatus.CREATED,
            createdAt = Instant.parse("2026-11-25T12:00:00Z"),
        )

        orders.insert(order)

        assertThat(orders.findById(order.id)).isEqualTo(order)
    }

}