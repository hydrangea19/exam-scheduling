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
        return try {
            val session = sessionRepository.findById(sessionId).orElse(null)
                ?: return ResponseEntity.notFound().build()

            if (session.isWindowOpen) {
                return ResponseEntity.badRequest().body(mapOf(
                    "success" to false,
                    "error" to "Window is already open"
                ))
            }

            val updatedSession = session.copy(
                isWindowOpen = true,
                submissionDeadline = request.submissionDeadline,
                windowOpenedAt = Instant.now()
            )
            sessionRepository.save(updatedSession)

            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Submission window opened successfully"
            ))
        } catch (e: Exception) {
            logger.error("Failed to open window", e)
            ResponseEntity.badRequest().body(mapOf("success" to false, "error" to e.message))
        }
    }

    @PostMapping("/close-window")
    fun closeSubmissionWindow(
        @RequestParam sessionId: String,
        @Valid @RequestBody request: CloseSubmissionWindowRequest
    ): ResponseEntity<Map<String, Any?>> {
        logger.info("Closing submission window for session: {}", sessionId)

        return try {
            val session = sessionRepository.findById(sessionId).orElse(null)
                ?: return ResponseEntity.notFound().build()

            if (!session.isWindowOpen) {
                return ResponseEntity.badRequest().body(mapOf(
                    "success" to false,
                    "error" to "Window is already closed"
                ))
            }

            val updatedSession = session.copy(
                isWindowOpen = false,
                windowClosedAt = Instant.now()
            )
            sessionRepository.save(updatedSession)

            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Submission window closed successfully"
            ))
        } catch (e: Exception) {
            logger.error("Failed to close window", e)
            ResponseEntity.badRequest().body(mapOf("success" to false, "error" to e.message))
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