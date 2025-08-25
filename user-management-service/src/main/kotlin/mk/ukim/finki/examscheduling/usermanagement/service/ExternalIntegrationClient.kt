package mk.ukim.finki.examscheduling.usermanagement.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration.ExternalCourseSearchResponse
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration.ExternalCoursesResponse
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration.ExternalServicePingResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Service
class ExternalIntegrationClient(
    @Qualifier("serviceWebClient") private val webClient: WebClient
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
    fun ping(): CompletableFuture<ExternalServicePingResponse> {
        logger.info("Calling external integration service ping endpoint with JWT authentication")

        return webClient
            .get()
            .uri("http://localhost:8002/api/test/ping")
            .retrieve()
            .bodyToMono(ExternalServicePingResponse::class.java)
            .doOnSuccess { response ->
                logger.info("Successfully pinged external integration service: {}", response.service)
            }
            .doOnError { error ->
                logger.error("Failed to ping external integration service: {}", error.message, error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getAllCoursesFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getAllCourses(): CompletableFuture<ExternalCoursesResponse> {
        logger.info("Fetching all courses from external integration service with JWT authentication")

        return webClient
            .get()
            .uri("http://localhost:8002/api/test/courses")
            .retrieve()
            .bodyToMono(ExternalCoursesResponse::class.java)
            .doOnSuccess { response ->
                logger.info("Successfully fetched {} courses from external integration service", response.count)
            }
            .doOnError { error ->
                logger.error("Failed to fetch courses from external integration service: {}", error.message, error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "searchCoursesFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun searchCourses(query: String): CompletableFuture<ExternalCourseSearchResponse> {
        logger.info("Searching courses with query '{}' in external integration service with JWT authentication", query)

        return webClient
            .get()
            .uri("http://localhost:8002/api/test/courses/search?query={query}", query)
            .retrieve()
            .bodyToMono(ExternalCourseSearchResponse::class.java)
            .doOnSuccess { response ->
                logger.info("Successfully found {} courses for query '{}'", response.count, query)
            }
            .doOnError { error ->
                logger.error("Failed to search courses for query '{}': {}", query, error.message, error)
            }
            .toFuture()
    }

    // Fallback methods
    fun pingFallback(exception: Exception): CompletableFuture<ExternalServicePingResponse> {
        logger.warn("Using ping fallback due to: {}", exception.message)

        return CompletableFuture.completedFuture(
            ExternalServicePingResponse(
                message = "External Integration Service unavailable",
                timestamp = Instant.now(),
                service = "external-integration-service",
                version = "unknown",
                database = "unavailable"
            )
        )
    }

    fun getAllCoursesFallback(exception: Exception): CompletableFuture<ExternalCoursesResponse> {
        logger.warn("Using getAllCourses fallback due to: {}", exception.message)

        return CompletableFuture.completedFuture(
            ExternalCoursesResponse(
                courses = emptyList(),
                statistics = emptyList(),
                count = 0
            )
        )
    }

    fun searchCoursesFallback(query: String, exception: Exception): CompletableFuture<ExternalCourseSearchResponse> {
        logger.warn("Using searchCourses fallback for query '{}' due to: {}", query, exception.message)

        return CompletableFuture.completedFuture(
            ExternalCourseSearchResponse(
                query = query,
                results = emptyList(),
                count = 0
            )
        )
    }
}