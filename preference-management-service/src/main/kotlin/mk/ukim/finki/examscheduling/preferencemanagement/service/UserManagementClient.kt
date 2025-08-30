/*
package mk.ukim.finki.examscheduling.preferencemanagement.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*
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
s
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

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserByIdFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getUserById(userId: UUID): CompletableFuture<Map<String, Any>?> {
        logger.debug("Fetching user by ID {} from user management service", userId)

        return webClient
            .get()
            .uri("/api/test/users/{id}", userId)
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                logger.info("Successfully fetched user: {}", userId)
            }
            .onErrorResume(WebClientResponseException::class.java) { ex ->
                if (ex.statusCode == HttpStatus.NOT_FOUND) {
                    logger.warn("User with ID {} not found in user management service", userId)
                    Mono.empty()
                } else {
                    Mono.error(ex)
                }
            }
            .doOnError { error ->
                logger.error("Failed to fetch user by ID {} from user management service", userId, error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getAllUsersFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getAllUsers(): CompletableFuture<Map<String, Any>> {
        logger.debug("Fetching all users from user management service")

        return webClient
            .get()
            .uri("/api/test/users")
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                val count = (response["count"] as? Int) ?: 0
                logger.info("Successfully fetched {} users from user management service", count)
            }
            .doOnError { error ->
                logger.error("Failed to fetch users from user management service", error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "createUserFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun createUser(userRequest: Map<String, Any>): CompletableFuture<Map<String, Any>> {
        logger.debug("Creating user in user management service")

        return webClient
            .post()
            .uri("/api/test/users")
            .bodyValue(userRequest)
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                logger.info("Successfully created user: {}", response["email"])
            }
            .doOnError { error ->
                logger.error("Failed to create user in user management service", error)
            }
            .toFuture()
    }

    // Fallback methods
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

    fun getUserByIdFallback(userId: UUID, exception: Exception): CompletableFuture<Map<String, Any>?> {
        logger.warn("Using getUserById fallback for user {} due to: {}", userId, exception.message)
        return CompletableFuture.completedFuture(null)
    }

    fun getAllUsersFallback(exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using getAllUsers fallback due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "users" to emptyList<Any>(),
                "count" to 0,
                "error" to "User Management Service unavailable"
            )
        )
    }

    fun createUserFallback(userRequest: Map<String, Any>, exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using createUser fallback due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "error" to "User Management Service unavailable",
                "message" to "Could not create user at this time"
            )
        )
    }
}*/
