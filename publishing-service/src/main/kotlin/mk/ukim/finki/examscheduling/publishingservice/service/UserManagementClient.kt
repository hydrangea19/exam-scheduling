package mk.ukim.finki.examscheduling.publishingservice.service

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
class UserManagementClient(
    @Qualifier("userManagementWebClient") private val webClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(UserManagementClient::class.java)

    companion object {
        private const val CIRCUIT_BREAKER_NAME = "user-management-service"
        private const val RETRY_NAME = "user-management-service"
        private const val TIME_LIMITER_NAME = "user-management-service"
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "pingFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun ping(): CompletableFuture<Map<String, Any>> {
        logger.debug("Calling user management service ping endpoint")

        return webClient
            .get()
            .uri("/api/test/ping")
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                logger.info("Successfully pinged user management service")
            }
            .doOnError { error ->
                logger.error("Failed to ping user management service", error)
            }
            .toFuture()
    }

    fun pingFallback(exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using ping fallback for user management service due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "message" to "User Management Service unavailable",
                "timestamp" to Instant.now(),
                "service" to "user-management-service",
                "status" to "fallback"
            )
        )
    }
}