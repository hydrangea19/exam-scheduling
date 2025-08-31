package mk.ukim.finki.examscheduling.preferencemanagement.controller

import jakarta.validation.Valid
import mk.ukim.finki.examscheduling.preferencemanagement.domain.SubmitPreferencesRequest
import mk.ukim.finki.examscheduling.preferencemanagement.query.repository.ExamSessionPeriodViewRepository
import mk.ukim.finki.examscheduling.preferencemanagement.query.repository.PreferenceSubmissionSummaryRepository
import mk.ukim.finki.examscheduling.preferencemanagement.service.PreferenceManagementApplicationService
import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/professor")
@PreAuthorize("hasRole('PROFESSOR')")
@Validated
class ProfessorController(
    private val preferenceService: PreferenceManagementApplicationService,
    private val submissionRepository: PreferenceSubmissionSummaryRepository,
    private val sessionRepository: ExamSessionPeriodViewRepository
) {
    private val logger = LoggerFactory.getLogger(ProfessorController::class.java)

    @GetMapping("/my-preferences")
    fun getMyPreferences(@RequestParam(required = false) sessionId: String?): ResponseEntity<Map<String, Any?>> {
        logger.debug("Fetching preferences for current professor")

        return try {
            val currentUser = SecurityUtils.getCurrentUser()
            val professorId = UUID.fromString(currentUser?.id ?: throw IllegalStateException("No current user"))

            val preferences = if (sessionId != null) {
                submissionRepository.findByProfessorIdAndExamSessionPeriodId(professorId.toString(), sessionId)
                    ?.let { listOf(it) } ?: emptyList()
            } else {
                submissionRepository.findByProfessorId(professorId.toString())
            }

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "preferences" to preferences,
                    "count" to preferences.size,
                    "sessionId" to sessionId,
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error fetching professor's preferences", e)
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

    @PostMapping("/preferences")
    fun submitMyPreferences(@Valid @RequestBody request: SubmitPreferencesRequest): ResponseEntity<Map<String, Any?>> {
        logger.info("Professor submitting preferences")

        return try {
            val currentUser = SecurityUtils.getCurrentUser()
            val professorId = UUID.fromString(currentUser?.id ?: throw IllegalStateException("No current user"))

            val submissionId = preferenceService.submitProfessorPreferences(
                request.copy(professorId = professorId)
            ).get()

            logger.info("Professor preferences submitted successfully with ID: {}", submissionId)

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "submissionId" to submissionId,
                    "message" to "Preferences submitted successfully",
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error submitting professor preferences", e)
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

    @GetMapping("/available-sessions")
    fun getAvailableSessions(): ResponseEntity<Map<String, Any?>> {
        logger.debug("Fetching available sessions for preference submission")

        return try {
            val availableSessions = sessionRepository.findByIsWindowOpenTrue()

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "availableSessions" to availableSessions,
                    "count" to availableSessions.size,
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error fetching available sessions", e)
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