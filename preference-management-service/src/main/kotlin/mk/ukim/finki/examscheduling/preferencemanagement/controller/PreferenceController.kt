package mk.ukim.finki.examscheduling.preferencemanagement.controller

import jakarta.validation.Valid
import mk.ukim.finki.examscheduling.preferencemanagement.domain.SubmitPreferencesRequest
import mk.ukim.finki.examscheduling.preferencemanagement.domain.UpdatePreferencesRequest
import mk.ukim.finki.examscheduling.preferencemanagement.domain.WithdrawPreferencesRequest
import mk.ukim.finki.examscheduling.preferencemanagement.query.repository.PreferenceStatisticsViewRepository
import mk.ukim.finki.examscheduling.preferencemanagement.query.repository.PreferenceSubmissionSummaryRepository
import mk.ukim.finki.examscheduling.preferencemanagement.service.PreferenceManagementApplicationService
import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/preferences")
@Validated
class PreferenceController(
    private val preferenceService: PreferenceManagementApplicationService,
    private val submissionRepository: PreferenceSubmissionSummaryRepository,
    private val statisticsRepository: PreferenceStatisticsViewRepository
) {
    private val logger = LoggerFactory.getLogger(PreferenceController::class.java)

    @PostMapping("/submit")
    @PreAuthorize("hasRole('PROFESSOR') or hasRole('ADMIN')")
    fun submitPreferences(@Valid @RequestBody request: SubmitPreferencesRequest): ResponseEntity<Map<String, Any?>> {
        logger.info("Preference submission request received for professor: {}", request.professorId)

        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            if (!SecurityUtils.hasRole("ADMIN") && currentUser?.id != request.professorId.toString()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "Can only submit preferences for yourself"))
            }

            val submissionId = preferenceService.submitProfessorPreferences(request).get()

            logger.info("Preferences submitted successfully with ID: {}", submissionId)

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "submissionId" to submissionId,
                    "message" to "Preferences submitted successfully",
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error submitting preferences", e)
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

    @PutMapping("/{submissionId}")
    @PreAuthorize("hasRole('PROFESSOR') or hasRole('ADMIN')")
    fun updatePreferences(
        @PathVariable submissionId: String,
        @Valid @RequestBody request: UpdatePreferencesRequest
    ): ResponseEntity<Map<String, Any?>> {
        logger.info("Preference update request received for submission: {}", submissionId)

        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            if (!SecurityUtils.hasRole("ADMIN") && currentUser?.id != request.professorId.toString()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "Can only update your own preferences"))
            }

            preferenceService.updateProfessorPreferences(request.copy(submissionId = submissionId)).get()

            logger.info("Preferences updated successfully for submission: {}", submissionId)

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Preferences updated successfully",
                    "submissionId" to submissionId,
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error updating preferences", e)
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

    @DeleteMapping("/{submissionId}")
    @PreAuthorize("hasRole('PROFESSOR') or hasRole('ADMIN')")
    fun withdrawPreferences(
        @PathVariable submissionId: String,
        @Valid @RequestBody request: WithdrawPreferencesRequest
    ): ResponseEntity<Map<String, Any?>> {
        logger.info("Preference withdrawal request received for submission: {}", submissionId)

        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            if (!SecurityUtils.hasRole("ADMIN") && currentUser?.id != request.professorId.toString()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "Can only withdraw your own preferences"))
            }

            preferenceService.withdrawPreferences(request.copy(submissionId = submissionId)).get()

            logger.info("Preferences withdrawn successfully for submission: {}", submissionId)

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Preferences withdrawn successfully",
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error withdrawing preferences", e)
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

    @GetMapping("/professor/{professorId}")
    @PreAuthorize("hasRole('PROFESSOR') or hasRole('ADMIN')")
    fun getPreferencesByProfessor(
        @PathVariable professorId: UUID,
        @RequestParam(required = false) sessionId: String?
    ): ResponseEntity<Map<String, Any?>> {
        logger.debug("Fetching preferences for professor: {}", professorId)

        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            if (!SecurityUtils.hasRole("ADMIN") && currentUser?.id != professorId.toString()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "Can only view your own preferences"))
            }

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
                    "professorId" to professorId,
                    "sessionId" to sessionId,
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error fetching preferences for professor: {}", professorId, e)
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

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getPreferencesBySession(@PathVariable sessionId: String): ResponseEntity<Map<String, Any?>> {
        logger.debug("Fetching preferences for session: {}", sessionId)

        return try {
            val preferences = submissionRepository.findByExamSessionPeriodId(sessionId)
            val statistics = statisticsRepository.findByExamSessionPeriodId(sessionId)

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "preferences" to preferences,
                    "statistics" to statistics,
                    "sessionId" to sessionId,
                    "totalSubmissions" to preferences.size,
                    "uniqueProfessors" to preferences.map { it.professorId }.toSet().size,
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error fetching preferences for session: {}", sessionId, e)
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

    @GetMapping("/session/{sessionId}/conflicts")
    @PreAuthorize("hasRole('ADMIN')")
    fun getConflictingPreferences(@PathVariable sessionId: String): ResponseEntity<Map<String, Any?>> {
        logger.debug("Fetching conflicting preferences for session: {}", sessionId)

        return try {
            val conflicts = statisticsRepository.findConflictingTimeSlots(sessionId)

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "conflicts" to conflicts,
                    "sessionId" to sessionId,
                    "conflictCount" to conflicts.size,
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Error fetching conflicting preferences for session: {}", sessionId, e)
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