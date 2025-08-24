package mk.ukim.finki.examscheduling.shared.config

import jakarta.annotation.PostConstruct
import mk.ukim.finki.examscheduling.shared.logging.CorrelationIdContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LoggingConfiguration {

    private val logger = LoggerFactory.getLogger(LoggingConfiguration::class.java)

    @Value("\${spring.application.name:unknown-service}")
    private lateinit var serviceName: String

    @PostConstruct
    fun initializeLogging() {
        CorrelationIdContext.setServiceName(serviceName)
        logger.info("Initialized structured logging for service: {}", serviceName)
    }

    @Bean
    fun loggingHealthIndicator(): LoggingHealthIndicator {
        return LoggingHealthIndicator()
    }
}