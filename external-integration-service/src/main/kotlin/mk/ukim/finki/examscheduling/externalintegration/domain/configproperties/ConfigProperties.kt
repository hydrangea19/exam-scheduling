package mk.ukim.finki.examscheduling.externalintegration.domain.configproperties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "external-services")
data class ExternalServicesProperties(
    val accreditation: ServiceConfig,
    val raspredelba: ServiceConfig,
    val caching: CachingConfig,
    val circuitBreaker: CircuitBreakerConfig
)

data class ServiceConfig(
    val url: String,
    val connectTimeout: Duration = Duration.ofSeconds(5),
    val readTimeout: Duration = Duration.ofSeconds(30),
    val retryAttempts: Int = 3,
    val retryDelay: Duration = Duration.ofSeconds(1)
)

data class CachingConfig(
    val courseCacheTtl: Duration = Duration.ofHours(6),
    val professorCacheTtl: Duration = Duration.ofHours(4),
    val roomCacheTtl: Duration = Duration.ofHours(4),
    val enrollmentCacheTtl: Duration = Duration.ofMinutes(30)
)

data class CircuitBreakerConfig(
    val failureRateThreshold: Float = 50.0f,
    val waitDurationInOpenState: Duration = Duration.ofMinutes(1),
    val slidingWindowSize: Int = 10,
    val minimumNumberOfCalls: Int = 5
)