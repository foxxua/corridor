package io.github.foxx.corridor.core.order

import io.github.foxx.corridor.core.TestcontainersConfiguration
import io.github.foxx.corridor.core.outbox.OutboxRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class OrderCreationTransactionTest {

    @Autowired
    lateinit var orders: OrderService

    @Autowired
    lateinit var jdbcClient: JdbcClient

    @MockitoBean
    lateinit var outbox: OutboxRepository

    @Test
    fun `keeps no orders where outbox write fails`() {
        willThrow(IllegalStateException("broken down"))
            .given(outbox)
            .append(anyString(), anyString(), anyString())

        assertThatThrownBy { orders.create() }
            .hasMessage("broken down")

        val count = jdbcClient.sql("select count(*) from orders")
            .query(Int::class.java)
            .single()

        assertThat(count).isZero
    }

}