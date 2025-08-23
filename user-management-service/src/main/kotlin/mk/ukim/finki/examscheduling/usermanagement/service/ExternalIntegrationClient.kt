package mk.ukim.finki.examscheduling.usermanagement.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration.ExternalCourseDetailResponse
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration.ExternalCourseSearchResponse
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration.ExternalCoursesResponse
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration.ExternalServicePingResponse
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
    fun ping(): CompletableFuture<ExternalServicePingResponse> {
        logger.debug("Calling external integration service ping endpoint")

        return webClient
            .get()
            .uri("/api/test/ping")
            .retrieve()
            .bodyToMono(ExternalServicePingResponse::class.java)
            .doOnSuccess { response ->
                logger.info("Successfully pinged external integration service: {}", response.message)
            }
            .doOnError { error ->
                logger.error("Failed to ping external integration service", error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getAllCoursesFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getAllCourses(): CompletableFuture<ExternalCoursesResponse> {
        logger.debug("Fetching all courses from external integration service")

        return webClient
            .get()
            .uri("/api/test/courses")
            .retrieve()
            .bodyToMono(ExternalCoursesResponse::class.java)
            .doOnSuccess { response ->
                logger.info(
                    "Successfully fetched {} courses from external integration service",
                    response.count
                )
            }
            .doOnError { error ->
                logger.error("Failed to fetch courses from external integration service", error)
            }
            .toFuture()
    }

    /**
     * Get course by ID from external integration service
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getCourseByIdFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getCourseById(courseId: UUID): CompletableFuture<ExternalCourseDetailResponse?> {
        logger.debug("Fetching course by ID {} from external integration service", courseId)

        return webClient
            .get()
            .uri("/api/test/courses/{id}", courseId)
            .retrieve()
            .bodyToMono(ExternalCourseDetailResponse::class.java)
            .doOnSuccess { response ->
                logger.info(
                    "Successfully fetched course: {} - {}",
                    response.courseCode, response.courseName
                )
            }
            .onErrorResume(WebClientResponseException::class.java) { ex ->
                if (ex.statusCode == HttpStatus.NOT_FOUND) {
                    logger.warn("Course with ID {} not found in external integration service", courseId)
                    Mono.empty()
                } else {
                    Mono.error(ex)
                }
            }
            .doOnError { error ->
                logger.error(
                    "Failed to fetch course by ID {} from external integration service",
                    courseId, error
                )
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "searchCoursesFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun searchCourses(query: String): CompletableFuture<ExternalCourseSearchResponse> {
        logger.debug("Searching courses with query '{}' in external integration service", query)

        return webClient
            .get()
            .uri("/api/test/courses/search?query={query}", query)
            .retrieve()
            .bodyToMono(ExternalCourseSearchResponse::class.java)
            .doOnSuccess { response ->
                logger.info("Found {} courses matching query '{}'", response.count, query)
            }
            .doOnError { error ->
                logger.error(
                    "Failed to search courses with query '{}' in external integration service",
                    query, error
                )
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getCoursesByDepartmentFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getCoursesByDepartment(department: String): CompletableFuture<Map<String, Any>> {
        logger.debug("Fetching courses for department '{}' from external integration service", department)

        return webClient
            .get()
            .uri("/api/test/courses/department/{department}", department)
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                val count = response["count"] as? Int ?: 0
                logger.info("Found {} courses in department '{}'", count, department)
            }
            .doOnError { error ->
                logger.error(
                    "Failed to fetch courses for department '{}' from external integration service",
                    department, error
                )
            }
            .toFuture()
    }

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

    fun getCourseByIdFallback(courseId: UUID, exception: Exception): CompletableFuture<ExternalCourseDetailResponse?> {
        logger.warn("Using getCourseById fallback for ID {} due to: {}", courseId, exception.message)
        return CompletableFuture.completedFuture(null)
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

    fun getCoursesByDepartmentFallback(department: String, exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn(
            "Using getCoursesByDepartment fallback for department '{}' due to: {}",
            department,
            exception.message
        )
        return CompletableFuture.completedFuture(
            mapOf(
                "department" to department,
                "courses" to emptyList<Any>(),
                "count" to 0,
                "error" to "Service unavailable"
            )
        )
    }
}