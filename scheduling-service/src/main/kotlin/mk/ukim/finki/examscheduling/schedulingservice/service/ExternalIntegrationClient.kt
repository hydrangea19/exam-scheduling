package mk.ukim.finki.examscheduling.schedulingservice.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Service
class ExternalIntegrationClient(
    @Qualifier("externalIntegrationWebClient") private val webClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(ExternalIntegrationClient::class.java)

    companion object {
        private const val CIRCUIT_BREAKER_NAME = "external-integration-service"
        private const val RETRY_NAME = "external-integration-service"
        private const val TIME_LIMITER_NAME = "external-integration-service"
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "pingFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun ping(): CompletableFuture<Map<String, Any>> {
        logger.debug("Calling external integration service ping endpoint")

        return webClient
            .get()
            .uri("/api/test/ping")
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                logger.info("Successfully pinged external integration service")
            }
            .doOnError { error ->
                logger.error("Failed to ping external integration service", error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getAllCoursesFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getAllCourses(): CompletableFuture<Map<String, Any>> {
        logger.debug("Fetching all courses from external integration service")

        return webClient
            .get()
            .uri("/api/test/courses")
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                val count = (response["count"] as? Int) ?: 0
                logger.info("Successfully fetched {} courses from external integration service", count)
            }
            .doOnError { error ->
                logger.error("Failed to fetch courses from external integration service", error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getEnrollmentDataFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getEnrollmentData(): CompletableFuture<Map<String, Any>> {
        logger.debug("Fetching enrollment data from external integration service")

        return webClient
            .get()
            .uri("/api/test/enrollment")
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                logger.info("Successfully fetched enrollment data from external integration service")
            }
            .doOnError { error ->
                logger.error("Failed to fetch enrollment data from external integration service", error)
            }
            .toFuture()
    }

    // Fallback methods
    fun pingFallback(exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using ping fallback for external integration service due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "message" to "External Integration Service unavailable",
                "timestamp" to Instant.now(),
                "service" to "external-integration-service",
                "status" to "fallback"
            )
        )
    }

    fun getAllCoursesFallback(exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using getAllCourses fallback due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "courses" to emptyList<Any>(),
                "count" to 0,
                "error" to "External Integration Service unavailable"
            )
        )
    }

    fun getEnrollmentDataFallback(exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using getEnrollmentData fallback due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "enrollmentData" to emptyMap<String, Any>(),
                "courses" to emptyList<Any>(),
                "error" to "External Integration Service unavailable"
            )
        )
    }
}