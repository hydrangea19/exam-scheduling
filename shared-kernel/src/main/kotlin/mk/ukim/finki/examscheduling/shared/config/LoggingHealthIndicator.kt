package mk.ukim.finki.examscheduling.shared.config

import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class LoggingHealthIndicator : HealthIndicator {

    override fun health(): Health {
        return try {
            val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
            val rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

            val details = mapOf(
                "loggerContext" to loggerContext.name,
                "rootLogLevel" to rootLogger.level?.toString(),
                "appendersCount" to rootLogger.iteratorForAppenders().asSequence().count()
            )

            Health.up()
                .withDetails(details)
                .build()
        } catch (e: Exception) {
            Health.down()
                .withException(e)
                .build()
        }
    }
}