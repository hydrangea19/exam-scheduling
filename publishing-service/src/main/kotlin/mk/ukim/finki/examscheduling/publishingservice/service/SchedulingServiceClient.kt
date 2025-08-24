package mk.ukim.finki.examscheduling.publishingservice.service

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
import java.time.LocalDate
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
class SchedulingServiceClient(
    @Qualifier("schedulingWebClient") private val webClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(SchedulingServiceClient::class.java)

    companion object {
        private const val CIRCUIT_BREAKER_NAME = "scheduling-service"
        private const val RETRY_NAME = "scheduling-service"
        private const val TIME_LIMITER_NAME = "scheduling-service"
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "pingFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun ping(): CompletableFuture<Map<String, Any>> {
        logger.debug("Calling scheduling service ping endpoint")

        return webClient
            .get()
            .uri("/api/test/ping")
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                logger.info("Successfully pinged scheduling service")
            }
            .doOnError { error ->
                logger.error("Failed to ping scheduling service", error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getAllSchedulesFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getAllSchedules(): CompletableFuture<Map<String, Any>> {
        logger.debug("Fetching all schedules from scheduling service")

        return webClient
            .get()
            .uri("/api/test/schedules")
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                val count = (response["count"] as? Int) ?: 0
                logger.info("Successfully fetched {} schedules from scheduling service", count)
            }
            .doOnError { error ->
                logger.error("Failed to fetch schedules from scheduling service", error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getScheduleByIdFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getScheduleById(scheduleId: UUID): CompletableFuture<Map<String, Any>?> {
        logger.debug("Fetching schedule by ID {} from scheduling service", scheduleId)

        return webClient
            .get()
            .uri("/api/test/schedules/{id}", scheduleId)
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                logger.info("Successfully fetched schedule: {}", scheduleId)
            }
            .onErrorResume(WebClientResponseException::class.java) { ex ->
                if (ex.statusCode == HttpStatus.NOT_FOUND) {
                    logger.warn("Schedule with ID {} not found in scheduling service", scheduleId)
                    Mono.empty()
                } else {
                    Mono.error(ex)
                }
            }
            .doOnError { error ->
                logger.error("Failed to fetch schedule by ID {} from scheduling service", scheduleId, error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getExamsByDateFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getExamsByDate(date: LocalDate): CompletableFuture<Map<String, Any>> {
        logger.debug("Fetching exams by date {} from scheduling service", date)

        return webClient
            .get()
            .uri("/api/test/exams/daily/{date}", date)
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                val count = (response["count"] as? Int) ?: 0
                logger.info("Successfully fetched {} exams for date {} from scheduling service", count, date)
            }
            .doOnError { error ->
                logger.error("Failed to fetch exams for date {} from scheduling service", date, error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getFinalizedSchedulesFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getFinalizedSchedules(): CompletableFuture<Map<String, Any?>> {
        logger.debug("Fetching finalized schedules from scheduling service")

        return webClient
            .get()
            .uri("/api/test/schedules")
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .map { response ->
                val schedules = response["schedules"] as? List<Map<String, Any>> ?: emptyList()
                val finalizedSchedules = schedules.filter { schedule ->
                    val status = schedule["status"] as? String
                    status == "FINALIZED" || status == "PUBLISHED"
                }

                mapOf(
                    "schedules" to finalizedSchedules,
                    "count" to finalizedSchedules.size,
                    "statistics" to response["statistics"]
                )
            }
            .doOnSuccess { response ->
                val count = (response["count"] as? Int) ?: 0
                logger.info("Successfully fetched {} finalized schedules from scheduling service", count)
            }
            .doOnError { error ->
                logger.error("Failed to fetch finalized schedules from scheduling service", error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getScheduleStatisticsFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun getScheduleStatistics(): CompletableFuture<Map<String, Any>> {
        logger.debug("Fetching schedule statistics from scheduling service")

        return webClient
            .get()
            .uri("/api/test/statistics/comprehensive")
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                logger.info("Successfully fetched schedule statistics from scheduling service")
            }
            .doOnError { error ->
                logger.error("Failed to fetch schedule statistics from scheduling service", error)
            }
            .toFuture()
    }

    // Fallback methods
    fun pingFallback(exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using ping fallback for scheduling service due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "message" to "Scheduling Service unavailable",
                "timestamp" to Instant.now(),
                "service" to "scheduling-service",
                "status" to "fallback"
            )
        )
    }

    fun getAllSchedulesFallback(exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using getAllSchedules fallback due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "schedules" to emptyList<Any>(),
                "count" to 0,
                "error" to "Scheduling Service unavailable"
            )
        )
    }

    fun getScheduleByIdFallback(scheduleId: UUID, exception: Exception): CompletableFuture<Map<String, Any>?> {
        logger.warn("Using getScheduleById fallback for schedule {} due to: {}", scheduleId, exception.message)
        return CompletableFuture.completedFuture(null)
    }

    fun getExamsByDateFallback(date: LocalDate, exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using getExamsByDate fallback for date {} due to: {}", date, exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "date" to date,
                "exams" to emptyList<Any>(),
                "count" to 0,
                "error" to "Scheduling Service unavailable"
            )
        )
    }

    fun getFinalizedSchedulesFallback(exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using getFinalizedSchedules fallback due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "schedules" to emptyList<Any>(),
                "count" to 0,
                "error" to "Scheduling Service unavailable"
            )
        )
    }

    fun getScheduleStatisticsFallback(exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using getScheduleStatistics fallback due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "statistics" to emptyMap<String, Any>(),
                "overview" to emptyMap<String, Any>(),
                "error" to "Scheduling Service unavailable"
            )
        )
    }
}