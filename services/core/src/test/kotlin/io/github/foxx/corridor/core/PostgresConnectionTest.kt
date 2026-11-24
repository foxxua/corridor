package io.github.foxx.corridor.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.simple.JdbcClient

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class PostgresConnectionTest {

    @Autowired
    lateinit var jdbcClient: JdbcClient

    @Test
    fun `reads from postgres`() {
        val value = jdbcClient.sql("select 1").query().singleValue()

        assertThat(value).isEqualTo(1)
    }
}
