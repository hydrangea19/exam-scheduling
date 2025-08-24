package mk.ukim.finki.examscheduling.usermanagement.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import mk.ukim.finki.examscheduling.shared.logging.CorrelationIdContext
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration.ExternalCourseSearchResponse
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration.ExternalCoursesResponse
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration.ExternalServicePingResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
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
    fun ping(): CompletableFuture<ExternalServicePingResponse> {
        val correlationId = CorrelationIdContext.getCorrelationId()
        val requestId = CorrelationIdContext.getRequestId()

        logger.info(
            "Calling external integration service ping endpoint",
            MDC.getCopyOfContextMap()?.plus(
                mapOf(
                    "operation" to "external_service_ping",
                    "targetService" to "external-integration-service",
                    "correlationId" to correlationId,
                    "requestId" to requestId
                )
            )
        )

        return webClient
            .get()
            .uri("/api/test/ping")
            .retrieve()
            .bodyToMono(ExternalServicePingResponse::class.java)
            .doOnSuccess { response ->
                logger.info(
                    "Successfully pinged external integration service",
                    MDC.getCopyOfContextMap()?.plus(
                        mapOf(
                            "operation" to "external_service_ping_success",
                            "targetService" to "external-integration-service",
                            "responseService" to response.service,
                            "responseVersion" to response.version,
                            "correlationId" to correlationId,
                            "requestId" to requestId
                        )
                    )
                )
            }
            .doOnError { error ->
                logger.error(
                    "Failed to ping external integration service",
                    MDC.getCopyOfContextMap()?.plus(
                        mapOf(
                            "operation" to "external_service_ping_error",
                            "targetService" to "external-integration-service",
                            "errorType" to error.javaClass.simpleName,
                            "errorMessage" to error.message,
                            "correlationId" to correlationId,
                            "requestId" to requestId
                        )
                    ), error
                )
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getAllCoursesFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getAllCourses(): CompletableFuture<ExternalCoursesResponse> {
        val correlationId = CorrelationIdContext.getCorrelationId()
        val requestId = CorrelationIdContext.getRequestId()

        logger.info(
            "Fetching all courses from external integration service",
            MDC.getCopyOfContextMap()?.plus(
                mapOf(
                    "operation" to "fetch_all_courses",
                    "targetService" to "external-integration-service",
                    "correlationId" to correlationId,
                    "requestId" to requestId
                )
            )
        )

        return webClient
            .get()
            .uri("/api/test/courses")
            .retrieve()
            .bodyToMono(ExternalCoursesResponse::class.java)
            .doOnSuccess { response ->
                logger.info(
                    "Successfully fetched courses from external integration service",
                    MDC.getCopyOfContextMap()?.plus(
                        mapOf(
                            "operation" to "fetch_all_courses_success",
                            "targetService" to "external-integration-service",
                            "coursesCount" to response.count,
                            "correlationId" to correlationId,
                            "requestId" to requestId
                        )
                    )
                )
            }
            .doOnError { error ->
                logger.error(
                    "Failed to fetch courses from external integration service",
                    MDC.getCopyOfContextMap()?.plus(
                        mapOf(
                            "operation" to "fetch_all_courses_error",
                            "targetService" to "external-integration-service",
                            "errorType" to error.javaClass.simpleName,
                            "errorMessage" to error.message,
                            "correlationId" to correlationId,
                            "requestId" to requestId
                        )
                    ), error
                )
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "searchCoursesFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun searchCourses(query: String): CompletableFuture<ExternalCourseSearchResponse> {
        val correlationId = CorrelationIdContext.getCorrelationId()
        val requestId = CorrelationIdContext.getRequestId()
        logger.debug("Searching courses with query '{}' in external integration service", query)

        return webClient
            .get()
            .uri("/api/test/courses/search?query={query}", query)
            .retrieve()
            .bodyToMono(ExternalCourseSearchResponse::class.java)
            .doOnSuccess { response ->
                logger.info(
                    "Successfully searched courses from external integration service",
                    MDC.getCopyOfContextMap()?.plus(
                        mapOf(
                            "operation" to "search_courses_sucess",
                            "targetService" to "external-integration-service",
                            "coursesCount" to response.count,
                            "correlationId" to correlationId,
                            "requestId" to requestId
                        )
                    )
                )
            }
            .doOnError { error ->
                logger.error(
                    "Failed to search courses from external integration service",
                    MDC.getCopyOfContextMap()?.plus(
                        mapOf(
                            "operation" to "search_courses_Error",
                            "targetService" to "external-integration-service",
                            "errorType" to error.javaClass.simpleName,
                            "errorMessage" to error.message,
                            "correlationId" to correlationId,
                            "requestId" to requestId
                        )
                    ), error
                )
            }
            .toFuture()
    }

    fun pingFallback(exception: Exception): CompletableFuture<ExternalServicePingResponse> {
        val correlationId = CorrelationIdContext.getCorrelationId()
        val requestId = CorrelationIdContext.getRequestId()

        logger.warn(
            "Using ping fallback for external integration service",
            MDC.getCopyOfContextMap()?.plus(
                mapOf(
                    "operation" to "external_service_ping_fallback",
                    "targetService" to "external-integration-service",
                    "fallbackReason" to exception.message,
                    "correlationId" to correlationId,
                    "requestId" to requestId
                )
            )
        )

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
        val correlationId = CorrelationIdContext.getCorrelationId()
        val requestId = CorrelationIdContext.getRequestId()

        logger.warn(
            "Using getAllCourses fallback for external integration service",
            MDC.getCopyOfContextMap()?.plus(
                mapOf(
                    "operation" to "fetch_all_courses_fallback",
                    "targetService" to "external-integration-service",
                    "fallbackReason" to exception.message,
                    "correlationId" to correlationId,
                    "requestId" to requestId
                )
            )
        )

        return CompletableFuture.completedFuture(
            ExternalCoursesResponse(
                courses = emptyList(),
                statistics = emptyList(),
                count = 0
            )
        )
    }

    fun searchCoursesFallback(query: String, exception: Exception): CompletableFuture<ExternalCourseSearchResponse> {
        val correlationId = CorrelationIdContext.getCorrelationId()
        val requestId = CorrelationIdContext.getRequestId()
        logger.warn(
            "Using searchCoursesFallback fallback for search query",
            MDC.getCopyOfContextMap()?.plus(
                mapOf(
                    "operation" to "search_courses_fallback",
                    "targetService" to "external-integration-service",
                    "fallbackReason" to exception.message,
                    "correlationId" to correlationId,
                    "requestId" to requestId
                )
            )
        )
        return CompletableFuture.completedFuture(
            ExternalCourseSearchResponse(
                query = query,
                results = emptyList(),
                count = 0
            )
        )
    }
}