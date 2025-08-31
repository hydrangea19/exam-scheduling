package mk.ukim.finki.examscheduling.preferencemanagement.controller

import jakarta.validation.Valid
import mk.ukim.finki.examscheduling.preferencemanagement.domain.CloseSubmissionWindowRequest
import mk.ukim.finki.examscheduling.preferencemanagement.domain.CreateExamSessionPeriodRequest
import mk.ukim.finki.examscheduling.preferencemanagement.domain.OpenSubmissionWindowRequest
import mk.ukim.finki.examscheduling.preferencemanagement.query.repository.ExamSessionPeriodViewRepository
import mk.ukim.finki.examscheduling.preferencemanagement.service.PreferenceManagementApplicationService
import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/admin/sessions")
@PreAuthorize("hasRole('ADMIN')")
@Validated
class ExamSessionAdminController(
    private val preferenceService: PreferenceManagementApplicationService,
    private val sessionRepository: ExamSessionPeriodViewRepository
) {
    private val logger = LoggerFactory.getLogger(ExamSessionAdminController::class.java)

    @PostMapping
    fun createExamSessionPeriod(@Valid @RequestBody request: CreateExamSessionPeriodRequest): ResponseEntity<Map<String, Any?>> {
        logger.info("Creating exam session period: {} {}", request.academicYear, request.examSession)

        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            val sessionId = preferenceService.createExamSessionPeriod(
                request.copy(createdBy = currentUser?.id ?: "UNKNOWN")
            ).get()

            logger.info("Exam session period created successfully: {}", sessionId)

            ResponseEntity.status(HttpStatus.CREATED)
                .body(
                    mapOf(
                        "success" to true,
                        "examSessionPeriodId" to sessionId,
                        "message" to "Exam session period created successfully",
                        "timestamp" to Instant.now()
                    )
                )
        } catch (e: Exception) {
            logger.error("Error creating exam session period", e)
            ResponseEntity.badRequest()
                .body(
                    mapOf(
                        "success" to false,
                        "error" to e.message,
                        "timestamp" to Instant.now()
                    )
                )
        }
    }

    @PostMapping("/open-window")
    fun openSubmissionWindow(
        @RequestParam sessionId: String,
        @Valid @RequestBody request: OpenSubmissionWindowRequest
    ): ResponseEntity<Map<String, Any?>> {
        logger.info("Opening submission window for session: {}", sessionId)

        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            preferenceService.openPreferenceSubmissionWindow(
                request.copy(
                    examSessionPeriodId = sessionId,
                    openedBy = currentUser?.id ?: "UNKNOWN"
                )
            ).get()

            logger.info("Submission window opened successfully for session: {}", sessionId)

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Submission window opened successfully",
                    "sessionId" to sessionId,
                    "submissionDeadline" to request.submissionDeadline,
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error opening submission window for session: {}", sessionId, e)
            ResponseEntity.badRequest()
                .body(
                    mapOf(
                        "success" to false,
                        "error" to e.message,
                        "timestamp" to Instant.now()
                    )
                )
        }
    }

    @PostMapping("/close-window")
    fun closeSubmissionWindow(
        @RequestParam sessionId: String,
        @Valid @RequestBody request: CloseSubmissionWindowRequest
    ): ResponseEntity<Map<String, Any?>> {
        logger.info("Closing submission window for session: {}", sessionId)

        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            preferenceService.closePreferenceSubmissionWindow(
                request.copy(
                    examSessionPeriodId = sessionId,
                    closedBy = currentUser?.id ?: "UNKNOWN"
                )
            ).get()

            logger.info("Submission window closed successfully for session: {}", sessionId)

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Submission window closed successfully",
                    "sessionId" to sessionId,
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error closing submission window for session: {}", sessionId, e)
            ResponseEntity.badRequest()
                .body(
                    mapOf(
                        "success" to false,
                        "error" to e.message,
                        "timestamp" to Instant.now()
                    )
                )
        }
    }

    @GetMapping
    fun getAllExamSessionPeriods(): ResponseEntity<Map<String, Any?>> {
        logger.debug("Fetching all exam session periods")

        return try {
            val sessions = sessionRepository.findAll().sortedByDescending { it.createdAt }

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "sessions" to sessions,
                    "count" to sessions.size,
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error fetching exam session periods", e)
            ResponseEntity.badRequest()
                .body(
                    mapOf(
                        "success" to false,
                        "error" to e.message,
                        "timestamp" to Instant.now()
                    )
                )
        }
    }
}