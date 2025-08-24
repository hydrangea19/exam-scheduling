package mk.ukim.finki.examscheduling.schedulingservice.service

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
class PreferenceManagementClient(
    @Qualifier("preferenceManagementWebClient") private val webClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(PreferenceManagementClient::class.java)

    companion object {
        private const val CIRCUIT_BREAKER_NAME = "preference-management-service"
        private const val RETRY_NAME = "preference-management-service"
        private const val TIME_LIMITER_NAME = "preference-management-service"
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "pingFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun ping(): CompletableFuture<Map<String, Any>> {
        logger.debug("Calling preference management service ping endpoint")

        return webClient
            .get()
            .uri("/api/test/ping")
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                logger.info("Successfully pinged preference management service")
            }
            .doOnError { error ->
                logger.error("Failed to ping preference management service", error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getAllPreferencesFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getAllPreferences(): CompletableFuture<Map<String, Any>> {
        logger.debug("Fetching all preferences from preference management service")

        return webClient
            .get()
            .uri("/api/test/preferences")
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                val count = (response["count"] as? Int) ?: 0
                logger.info("Successfully fetched {} preferences from preference management service", count)
            }
            .doOnError { error ->
                logger.error("Failed to fetch preferences from preference management service", error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPreferencesByProfessorFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getPreferencesByProfessor(professorId: UUID): CompletableFuture<Map<String, Any>> {
        logger.debug("Fetching preferences for professor {} from preference management service", professorId)

        return webClient
            .get()
            .uri("/api/test/preferences/professor/{professorId}", professorId)
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                val count = (response["count"] as? Int) ?: 0
                logger.info(
                    "Successfully fetched {} preferences for professor {} from preference management service",
                    count,
                    professorId
                )
            }
            .onErrorResume(WebClientResponseException::class.java) { ex ->
                if (ex.statusCode == HttpStatus.NOT_FOUND) {
                    logger.warn("No preferences found for professor {} in preference management service", professorId)
                    Mono.just(
                        mapOf(
                            "professorId" to professorId,
                            "preferences" to emptyList<Any>(),
                            "count" to 0
                        )
                    )
                } else {
                    Mono.error(ex)
                }
            }
            .doOnError { error ->
                logger.error(
                    "Failed to fetch preferences for professor {} from preference management service",
                    professorId,
                    error
                )
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPreferencesBySessionFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getPreferencesBySession(academicYear: String, examSession: String): CompletableFuture<Map<String, Any>> {
        logger.debug(
            "Fetching preferences for session {}/{} from preference management service",
            academicYear,
            examSession
        )

        return webClient
            .get()
            .uri("/api/test/preferences/session/{academicYear}/{examSession}", academicYear, examSession)
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                val count = (response["professorsCount"] as? Int) ?: 0
                logger.info(
                    "Successfully fetched preferences for {} professors from session {}/{}",
                    count,
                    academicYear,
                    examSession
                )
            }
            .doOnError { error ->
                logger.error(
                    "Failed to fetch preferences for session {}/{} from preference management service",
                    academicYear,
                    examSession,
                    error
                )
            }
            .toFuture()
    }

    // Fallback methods
    fun pingFallback(exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using ping fallback for preference management service due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "message" to "Preference Management Service unavailable",
                "timestamp" to Instant.now(),
                "service" to "preference-management-service",
                "status" to "fallback"
            )
        )
    }

    fun getAllPreferencesFallback(exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using getAllPreferences fallback due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "preferences" to emptyList<Any>(),
                "count" to 0,
                "error" to "Preference Management Service unavailable"
            )
        )
    }

    fun getPreferencesByProfessorFallback(
        professorId: UUID,
        exception: Exception
    ): CompletableFuture<Map<String, Any>> {
        logger.warn(
            "Using getPreferencesByProfessor fallback for professor {} due to: {}",
            professorId,
            exception.message
        )
        return CompletableFuture.completedFuture(
            mapOf(
                "professorId" to professorId,
                "preferences" to emptyList<Any>(),
                "count" to 0,
                "error" to "Preference Management Service unavailable"
            )
        )
    }

    fun getPreferencesBySessionFallback(
        academicYear: String,
        examSession: String,
        exception: Exception
    ): CompletableFuture<Map<String, Any>> {
        logger.warn(
            "Using getPreferencesBySession fallback for session {}/{} due to: {}",
            academicYear,
            examSession,
            exception.message
        )
        return CompletableFuture.completedFuture(
            mapOf(
                "academicYear" to academicYear,
                "examSession" to examSession,
                "preferences" to emptyList<Any>(),
                "professorsCount" to 0,
                "error" to "Preference Management Service unavailable"
            )
        )
    }
}