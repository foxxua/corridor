package io.github.foxx.corridor.core.actuator

import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component

@Component
class PostgresInfoContributor(
    private val jdbcClient: JdbcClient,
) : InfoContributor {

    override fun contribute(builder: Info.Builder) {
        val details: Map<String, Any?> = try {
            jdbcClient.sql(
                """
                select current_database()                as database,
                       current_user                      as user,
                       current_setting('server_version') as version
                """.trimIndent()
            ).query().singleRow()
        } catch (e: DataAccessException) {
            mapOf(
                "status" to "unavailable",
                "error"  to (e.mostSpecificCause.message ?: "unknown")
            )
        }

        builder.withDetail("database", details)
    }

}