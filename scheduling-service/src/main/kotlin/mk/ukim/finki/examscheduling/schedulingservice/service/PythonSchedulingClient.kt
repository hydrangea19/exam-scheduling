package mk.ukim.finki.examscheduling.schedulingservice.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import mk.ukim.finki.examscheduling.schedulingservice.domain.PythonConstraintViolation
import mk.ukim.finki.examscheduling.schedulingservice.domain.PythonSchedulingMetrics
import mk.ukim.finki.examscheduling.schedulingservice.domain.PythonSchedulingRequest
import mk.ukim.finki.examscheduling.schedulingservice.domain.PythonSchedulingResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Service
class PythonSchedulingClient(
    @Qualifier("pythonSchedulingWebClient") private val webClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(PythonSchedulingClient::class.java)

    companion object {
        private const val CIRCUIT_BREAKER_NAME = "python-scheduling-service"
        private const val RETRY_NAME = "python-scheduling-service"
        private const val TIME_LIMITER_NAME = "python-scheduling-service"
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "generateScheduleFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun generateSchedule(request: PythonSchedulingRequest): CompletableFuture<PythonSchedulingResponse> {
        logger.info("Calling Python scheduling service for {} courses", request.courses.size)

        return webClient
            .post()
            .uri("/api/schedule/generate")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PythonSchedulingResponse::class.java)
            .doOnSuccess { response ->
                logger.info(
                    "Successfully received schedule from Python service: {} exams, quality: {}",
                    response.scheduledExams.size, response.qualityScore
                )
            }
            .doOnError { error ->
                logger.error("Failed to call Python scheduling service", error)
            }
            .toFuture()
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "pingFallback")
    @Retry(name = RETRY_NAME)
    @TimeLimiter(name = TIME_LIMITER_NAME)
    fun ping(): CompletableFuture<Map<String, Any>> {
        logger.debug("Pinging Python scheduling service")

        return webClient
            .get()
            .uri("/api/health")
            .retrieve()
            .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
            .doOnSuccess { response ->
                logger.info("Successfully pinged Python scheduling service")
            }
            .doOnError { error ->
                logger.error("Failed to ping Python scheduling service", error)
            }
            .toFuture()
    }

    fun generateScheduleFallback(
        request: PythonSchedulingRequest,
        exception: Exception
    ): CompletableFuture<PythonSchedulingResponse> {
        logger.error("Using generateSchedule fallback due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            PythonSchedulingResponse(
                success = false,
                errorMessage = "Python scheduling service unavailable: ${exception.message}",
                scheduledExams = emptyList(),
                metrics = PythonSchedulingMetrics(
                    totalCoursesScheduled = 0,
                    totalProfessorPreferencesConsidered = 0,
                    preferencesSatisfied = 0,
                    preferenceSatisfactionRate = 0.0,
                    totalConflicts = 0,
                    resolvedConflicts = 0,
                    roomUtilizationRate = 0.0,
                    averageStudentExamsPerDay = 0.0,
                    processingTimeMs = 0L
                ),
                qualityScore = 0.0,
                violations = listOf(
                    PythonConstraintViolation(
                        violationType = "SERVICE_UNAVAILABLE",
                        severity = "CRITICAL",
                        description = "Python scheduling service is unavailable",
                        affectedExamIds = request.courses.map { it.courseId },
                        affectedStudents = request.courses.sumOf { it.studentCount },
                        suggestedResolution = "Check Python service status and retry"
                    )
                )
            )
        )
    }

    fun pingFallback(exception: Exception): CompletableFuture<Map<String, Any>> {
        logger.warn("Using ping fallback for Python scheduling service due to: {}", exception.message)
        return CompletableFuture.completedFuture(
            mapOf(
                "message" to "Python Scheduling Service unavailable",
                "timestamp" to Instant.now(),
                "service" to "python-scheduling-service",
                "status" to "fallback"
            )
        )
    }
}