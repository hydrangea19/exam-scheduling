package mk.ukim.finki.examscheduling.preferencemanagement.controller

import mk.ukim.finki.examscheduling.preferencemanagement.domain.*
import mk.ukim.finki.examscheduling.preferencemanagement.service.EventPublisher
import mk.ukim.finki.examscheduling.preferencemanagement.service.PreferenceManagementApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@RestController
@RequestMapping("/api/test/preferences")
class PreferenceTestController(
    private val preferenceService: PreferenceManagementApplicationService,
    private val eventPublisher: EventPublisher
) {

    @GetMapping("/ping")
    fun ping(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "service" to "preference-management-service",
                "status" to "UP",
                "timestamp" to Instant.now(),
                "message" to "Preference Management Service is running with Axon Framework"
            )
        )
    }

    @PostMapping("/create-test-session")
    fun createTestSession(): ResponseEntity<Map<String, Any?>> {
        return try {
            val sessionId = preferenceService.createExamSessionPeriod(
                CreateExamSessionPeriodRequest(
                    academicYear = "2025",
                    examSession = "TEST_SESSION_${System.currentTimeMillis()}",
                    createdBy = "system-test",
                    plannedStartDate = Instant.now().plus(30, ChronoUnit.DAYS),
                    plannedEndDate = Instant.now().plus(45, ChronoUnit.DAYS),
                    description = "Test session created by system"
                )
            ).get()

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Test exam session created successfully",
                    "examSessionPeriodId" to sessionId,
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "error" to e.message,
                    "timestamp" to Instant.now()
                )
            )
        }
    }

    @PostMapping("/create-full-test-workflow")
    fun createFullTestWorkflow(): ResponseEntity<Map<String, Any?>> {
        return try {
            val results = mutableMapOf<String, Any>()

            val sessionId = preferenceService.createExamSessionPeriod(
                CreateExamSessionPeriodRequest(
                    academicYear = "2025",
                    examSession = "FULL_TEST_${System.currentTimeMillis()}",
                    createdBy = "system-test",
                    plannedStartDate = Instant.now().plus(30, ChronoUnit.DAYS),
                    plannedEndDate = Instant.now().plus(45, ChronoUnit.DAYS),
                    description = "Full workflow test session"
                )
            ).get()
            results["sessionCreated"] = sessionId

            preferenceService.openPreferenceSubmissionWindow(
                OpenSubmissionWindowRequest(
                    examSessionPeriodId = sessionId,
                    academicYear = "2025",
                    examSession = "FULL_TEST",
                    openedBy = "system-test",
                    submissionDeadline = Instant.now().plus(14, ChronoUnit.DAYS),
                    description = "Test submission window"
                )
            ).get()
            results["windowOpened"] = true

            val professorId = UUID.randomUUID().toString()
            val submissionId = preferenceService.submitProfessorPreferences(
                SubmitPreferencesRequest(
                    professorId = UUID.fromString(professorId),
                    examSessionPeriodId = sessionId,
                    preferences = listOf(
                        PreferenceDetailsRequest(
                            courseId = "TEST_COURSE_FULL",
                            timePreferences = listOf(
                                TimeSlotPreferenceRequest(
                                    dayOfWeek = 1,
                                    startTime = "09:00",
                                    endTime = "11:00",
                                    preferenceLevel = "PREFERRED",
                                    reason = "Full workflow test preference"
                                )
                            ),
                            roomPreferences = listOf(
                                RoomPreferenceRequest(
                                    roomId = "TEST_ROOM_FULL",
                                    preferenceLevel = "PREFERRED",
                                    reason = "Test room for full workflow"
                                )
                            ),
                            specialRequirements = "Full workflow test requirements"
                        )
                    )
                )
            ).get()
            results["preferencesSubmitted"] = submissionId

            val statistics = preferenceService.getPreferencesBySession(sessionId).get()
            results["submissionCount"] = statistics.size

            preferenceService.closePreferenceSubmissionWindow(
                CloseSubmissionWindowRequest(
                    examSessionPeriodId = sessionId,
                    closedBy = "system-test",
                    reason = "Full workflow test completed",
                    totalSubmissions = statistics.size
                )
            ).get()
            results["windowClosed"] = true

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Full workflow test completed successfully",
                    "results" to results,
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "error" to e.message,
                    "stackTrace" to e.stackTrace.take(5).map { it.toString() },
                    "timestamp" to Instant.now()
                )
            )
        }
    }

    @PostMapping("/test-event-publishing")
    fun testEventPublishing(): ResponseEntity<Map<String, Any?>> {
        return try {
            eventPublisher.publishSystemNotification(
                mapOf(
                    "notificationType" to "TEST_NOTIFICATION",
                    "message" to "Test event publishing from preference management service",
                    "timestamp" to Instant.now()
                ),
                "test-${System.currentTimeMillis()}"
            )

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Test event published successfully",
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "error" to e.message,
                    "timestamp" to Instant.now()
                )
            )
        }
    }

    @GetMapping("/system-status")
    fun getSystemStatus(): ResponseEntity<Map<String, Any?>> {
        return try {
            val allSessions = preferenceService.getAllExamSessionPeriods().get()

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "axonFramework" to "ENABLED",
                    "eventSourcing" to "ACTIVE",
                    "cqrsPattern" to "IMPLEMENTED",
                    "domainDrivenDesign" to "APPLIED",
                    "kafkaIntegration" to "ACTIVE",
                    "totalExamSessions" to allSessions.size,
                    "activeWindows" to allSessions.count { it.isWindowOpen },
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "error" to e.message,
                    "timestamp" to Instant.now()
                )
            )
        }
    }
}