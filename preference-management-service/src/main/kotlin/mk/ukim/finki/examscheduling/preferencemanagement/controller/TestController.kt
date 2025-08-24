package mk.ukim.finki.examscheduling.preferencemanagement.controller

import mk.ukim.finki.examscheduling.preferencemanagement.domain.ProfessorPreference
import mk.ukim.finki.examscheduling.preferencemanagement.domain.TimePreference
import mk.ukim.finki.examscheduling.preferencemanagement.domain.enums.PreferenceLevel
import mk.ukim.finki.examscheduling.preferencemanagement.domain.events.PreferenceSubmissionWindowOpenedEvent
import mk.ukim.finki.examscheduling.preferencemanagement.domain.events.ProfessorPreferenceSubmittedEvent
import mk.ukim.finki.examscheduling.preferencemanagement.domain.events.SystemNotificationEvent
import mk.ukim.finki.examscheduling.preferencemanagement.repository.ProfessorPreferenceRepository
import mk.ukim.finki.examscheduling.preferencemanagement.repository.TimePreferenceRepository
import mk.ukim.finki.examscheduling.preferencemanagement.service.EventPublisher
import mk.ukim.finki.examscheduling.preferencemanagement.service.ExternalIntegrationClient
import mk.ukim.finki.examscheduling.preferencemanagement.service.UserManagementClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalTime
import java.util.*
import java.util.concurrent.CompletionException

@RestController
@RequestMapping("/api/test")
class TestController @Autowired constructor(
    private val professorPreferenceRepository: ProfessorPreferenceRepository,
    private val timePreferenceRepository: TimePreferenceRepository,
    private val userManagementClient: UserManagementClient,
    private val externalIntegrationClient: ExternalIntegrationClient,
    private val eventPublisher: EventPublisher
) {
    private val logger = LoggerFactory.getLogger(TestController::class.java)

    @GetMapping("/ping")
    fun ping(): Map<String, Any> {
        return mapOf(
            "message" to "Preference Management Service is running",
            "timestamp" to Instant.now(),
            "service" to "preference-management-service",
            "version" to "1.0.0-SNAPSHOT"
        )
    }

    // === Kafka Testing Endpoints ===

    @PostMapping("/kafka/publish-test-event")
    fun publishTestEvent(@RequestBody request: Map<String, Any>): ResponseEntity<Map<String, Any?>> {
        return try {
            val eventType = request["eventType"] as? String ?: "system-notification"
            val message = request["message"] as? String ?: "Test message from preference management service"

            val event = when (eventType) {
                "system-notification" -> SystemNotificationEvent(
                    notificationType = "TEST",
                    message = message,
                    targetService = "all-services",
                    metadata = mapOf(
                        "source" to "preference-management-service",
                        "testId" to UUID.randomUUID().toString()
                    )
                )
                "preference-window-opened" -> PreferenceSubmissionWindowOpenedEvent(
                    examSessionPeriodId = "TEST_SESSION_${System.currentTimeMillis()}",
                    academicYear = "2024-2025",
                    examSession = "TEST_EXAM",
                    openedBy = "TEST_ADMIN",
                    submissionDeadline = Instant.now().plusSeconds(3600),
                    description = message
                )
                "preference-submitted" -> ProfessorPreferenceSubmittedEvent(
                    professorId = UUID.randomUUID(),
                    examSessionPeriodId = "TEST_SESSION",
                    preferenceId = UUID.randomUUID(),
                    courseIds = listOf("TEST_COURSE_001", "TEST_COURSE_002")
                )
                else -> SystemNotificationEvent(
                    notificationType = "UNKNOWN_TEST",
                    message = message
                )
            }

            logger.info("Publishing test event: type={}, event={}", eventType, event.javaClass.simpleName)

            when (eventType) {
                "system-notification" -> eventPublisher.publishSystemNotification(event)
                else -> eventPublisher.publishPreferenceEvent(event)
            }

            ResponseEntity.ok(
                mapOf(
                    "status" to "SUCCESS",
                    "message" to "Event published successfully",
                    "eventType" to eventType,
                    "eventId" to event.eventId,
                    "timestamp" to event.timestamp,
                    "serviceName" to event.serviceName
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to publish test event", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "status" to "ERROR",
                    "message" to "Failed to publish event",
                    "error" to e.message
                )
            )
        }
    }

    @PostMapping("/kafka/publish-preference-workflow-events")
    fun publishPreferenceWorkflowEvents(): ResponseEntity<Map<String, Any?>> {
        return try {
            val sessionId = "KAFKA_TEST_${System.currentTimeMillis()}"
            val professorId = UUID.randomUUID()
            val preferenceId = UUID.randomUUID()

            logger.info("Publishing complete preference workflow events for session: {}", sessionId)

            val windowOpenedEvent = PreferenceSubmissionWindowOpenedEvent(
                examSessionPeriodId = sessionId,
                academicYear = "2024-2025",
                examSession = "KAFKA_TEST",
                openedBy = "KAFKA_TEST_ADMIN",
                submissionDeadline = Instant.now().plusSeconds(7200),
                description = "Kafka testing preference submission window"
            )
            eventPublisher.publishPreferenceEvent(windowOpenedEvent, "window-$sessionId")

            Thread.sleep(100)

            val preferenceSubmittedEvent = ProfessorPreferenceSubmittedEvent(
                professorId = professorId,
                examSessionPeriodId = sessionId,
                preferenceId = preferenceId,
                courseIds = listOf("SOA", "WP", "DIS")
            )
            eventPublisher.publishPreferenceEvent(preferenceSubmittedEvent, "pref-$professorId")

            Thread.sleep(100)

            val notificationEvent = SystemNotificationEvent(
                notificationType = "WORKFLOW_TEST",
                message = "Preference workflow test completed successfully",
                targetService = "scheduling-service",
                metadata = mapOf(
                    "sessionId" to sessionId,
                    "professorId" to professorId.toString(),
                    "preferenceId" to preferenceId.toString(),
                    "coursesCount" to 3
                )
            )
            eventPublisher.publishSystemNotification(notificationEvent, "workflow-$sessionId")

            ResponseEntity.ok(
                mapOf(
                    "status" to "SUCCESS",
                    "message" to "Preference workflow events published successfully",
                    "eventsPublished" to 3,
                    "sessionId" to sessionId,
                    "events" to listOf(
                        mapOf(
                            "type" to "PreferenceSubmissionWindowOpenedEvent",
                            "eventId" to windowOpenedEvent.eventId
                        ),
                        mapOf(
                            "type" to "ProfessorPreferenceSubmittedEvent",
                            "eventId" to preferenceSubmittedEvent.eventId
                        ),
                        mapOf(
                            "type" to "SystemNotificationEvent",
                            "eventId" to notificationEvent.eventId
                        )
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to publish preference workflow events", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "status" to "ERROR",
                    "message" to "Failed to publish workflow events",
                    "error" to e.message
                )
            )
        }
    }

    @PostMapping("/kafka/simulate-preference-submission")
    fun simulatePreferenceSubmission(@RequestBody request: Map<String, Any>): ResponseEntity<Map<String, Any?>> {
        return try {
            val professorId = UUID.fromString(request["professorId"] as? String ?: UUID.randomUUID().toString())
            val courseIds = request["courseIds"] as? List<String> ?: listOf("SOA", "WP")
            val sessionId = request["sessionId"] as? String ?: "SIM_SESSION_${System.currentTimeMillis()}"

            logger.info("Simulating preference submission: professorId={}, courseIds={}, sessionId={}",
                professorId, courseIds, sessionId)

            val preference = ProfessorPreference(
                id = UUID.randomUUID(),
                professorId = professorId,
                academicYear = "2024-2025",
                examSession = "KAFKA_SIMULATION"
            )
            val savedPreference = professorPreferenceRepository.save(preference)

            val timePreferences = listOf(
                TimePreference(
                    preferenceSubmission = savedPreference,
                    dayOfWeek = 1,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(11, 0),
                    preferenceLevel = PreferenceLevel.PREFERRED
                ),
                TimePreference(
                    preferenceSubmission = savedPreference,
                    dayOfWeek = 3,
                    startTime = LocalTime.of(14, 0),
                    endTime = LocalTime.of(16, 0),
                    preferenceLevel = PreferenceLevel.ACCEPTABLE
                )
            )
            val savedTimePreferences = timePreferenceRepository.saveAll(timePreferences)

            val event = ProfessorPreferenceSubmittedEvent(
                professorId = professorId,
                examSessionPeriodId = sessionId,
                preferenceId = savedPreference.id!!,
                courseIds = courseIds
            )
            eventPublisher.publishPreferenceEvent(event, "sim-$professorId")

            val notificationEvent = SystemNotificationEvent(
                notificationType = "PREFERENCE_SIMULATION",
                message = "Preference simulation completed with database persistence and event publishing",
                metadata = mapOf(
                    "professorId" to professorId.toString(),
                    "preferenceId" to savedPreference.id.toString(),
                    "courseIds" to courseIds,
                    "timePreferencesCount" to savedTimePreferences.size,
                    "databasePersisted" to true,
                    "eventPublished" to true
                )
            )
            eventPublisher.publishSystemNotification(notificationEvent, "sim-complete-$sessionId")

            ResponseEntity.ok(
                mapOf(
                    "status" to "SUCCESS",
                    "message" to "Preference submission simulation completed",
                    "simulation" to mapOf(
                        "professorId" to professorId,
                        "preferenceId" to savedPreference.id,
                        "courseIds" to courseIds,
                        "sessionId" to sessionId,
                        "timePreferencesCreated" to savedTimePreferences.size
                    ),
                    "events" to mapOf(
                        "preferenceEvent" to mapOf(
                            "eventId" to event.eventId,
                            "type" to "ProfessorPreferenceSubmittedEvent"
                        ),
                        "notificationEvent" to mapOf(
                            "eventId" to notificationEvent.eventId,
                            "type" to "SystemNotificationEvent"
                        )
                    ),
                    "integration" to mapOf(
                        "databasePersisted" to true,
                        "eventsPublished" to true,
                        "kafkaWorking" to true
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to simulate preference submission", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "status" to "ERROR",
                    "message" to "Preference submission simulation failed",
                    "error" to e.message
                )
            )
        }
    }

    @GetMapping("/kafka/health")
    fun kafkaHealth(): ResponseEntity<Map<String, Any?>> {
        return try {
            val healthEvent = SystemNotificationEvent(
                notificationType = "HEALTH_CHECK",
                message = "Kafka health check from preference management service"
            )

            eventPublisher.publishSystemNotification(healthEvent, "health-check")

            ResponseEntity.ok(
                mapOf(
                    "status" to "HEALTHY",
                    "message" to "Kafka is working properly",
                    "service" to "preference-management-service",
                    "kafkaProducer" to "CONNECTED",
                    "testEventPublished" to mapOf(
                        "eventId" to healthEvent.eventId,
                        "timestamp" to healthEvent.timestamp
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Kafka health check failed", e)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                mapOf(
                    "status" to "UNHEALTHY",
                    "message" to "Kafka health check failed",
                    "error" to e.message,
                    "kafkaProducer" to "ERROR"
                )
            )
        }
    }

    // === Service Communication Testing Endpoints ===

    @GetMapping("/test-user-management-service")
    fun testUserManagementService(): Map<String, Any?> {
        return try {
            logger.info("Testing communication with user management service")

            val pingResponse = userManagementClient.ping().get()

            mapOf(
                "status" to "SUCCESS",
                "message" to "User management service communication working",
                "userManagementService" to mapOf(
                    "ping" to pingResponse,
                    "reachable" to true
                )
            )
        } catch (e: Exception) {
            logger.error("User management service communication failed", e)
            mapOf(
                "status" to "ERROR",
                "message" to "User management service communication failed",
                "error" to when (e) {
                    is CompletionException -> e.cause?.message ?: e.message
                    else -> e.message
                },
                "fallbackUsed" to true
            )
        }
    }

    @GetMapping("/test-external-integration-service")
    fun testExternalIntegrationService(): Map<String, Any?> {
        return try {
            logger.info("Testing communication with external integration service")

            val pingResponse = externalIntegrationClient.ping().get()
            val coursesResponse = externalIntegrationClient.getAllCourses().get()

            mapOf(
                "status" to "SUCCESS",
                "message" to "External integration service communication working",
                "externalIntegrationService" to mapOf(
                    "ping" to pingResponse,
                    "courses" to mapOf(
                        "count" to coursesResponse["count"],
                        "available" to ((coursesResponse["count"]?.toString()?.toIntOrNull() ?: 0) > 0)
                    ),
                    "reachable" to true
                )
            )
        } catch (e: Exception) {
            logger.error("External integration service communication failed", e)
            mapOf(
                "status" to "ERROR",
                "message" to "External integration service communication failed",
                "error" to when (e) {
                    is CompletionException -> e.cause?.message ?: e.message
                    else -> e.message
                },
                "fallbackUsed" to true
            )
        }
    }

    @GetMapping("/test-full-service-integration")
    fun testFullServiceIntegration(): Map<String, Any?> {
        return try {
            logger.info("Testing full service integration: Preference Management + User Management + External Integration")

            val userServicePing = userManagementClient.ping().get()
            val externalServicePing = externalIntegrationClient.ping().get()
            val coursesData = externalIntegrationClient.getAllCourses().get()
            val usersData = userManagementClient.getAllUsers().get()

            val testProfessorId = UUID.randomUUID()

            val preference = ProfessorPreference(
                id = UUID.randomUUID(),
                professorId = testProfessorId,
                academicYear = "2024-2025",
                examSession = "INTEGRATION_TEST"
            )

            val savedPreference = professorPreferenceRepository.save(preference)

            val timePreference = TimePreference(
                preferenceSubmission = savedPreference,
                dayOfWeek = 1,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(12, 0),
                preferenceLevel = PreferenceLevel.PREFERRED
            )
            timePreferenceRepository.save(timePreference)

            mapOf(
                "status" to "SUCCESS",
                "message" to "Full service integration test completed",
                "results" to mapOf(
                    "userManagementService" to mapOf(
                        "status" to "CONNECTED",
                        "service" to userServicePing["service"],
                        "usersCount" to usersData["count"]
                    ),
                    "externalIntegrationService" to mapOf(
                        "status" to "CONNECTED",
                        "service" to externalServicePing["service"],
                        "coursesCount" to coursesData["count"]
                    ),
                    "preferenceCreated" to mapOf(
                        "preferenceId" to savedPreference.id,
                        "professorId" to testProfessorId,
                        "status" to savedPreference.status,
                        "timePreferencesCount" to 1
                    ),
                    "integrationWorking" to true
                )
            )
        } catch (e: Exception) {
            logger.error("Full service integration test failed", e)
            mapOf(
                "status" to "ERROR",
                "message" to "Full service integration test failed",
                "error" to when (e) {
                    is CompletionException -> e.cause?.message ?: e.message
                    else -> e.message
                },
                "integrationWorking" to false
            )
        }
    }

    @GetMapping("/test-professor-preference-with-user-data/{professorId}")
    fun testProfessorPreferenceWithUserData(@PathVariable professorId: UUID): ResponseEntity<Map<String, Any?>> {
        return try {
            logger.info("Testing professor preference with user data integration for professor: {}", professorId)

            val professorData = userManagementClient.getUserById(professorId).get()

            val preferences = professorPreferenceRepository.findByProfessorId(professorId)

            val coursesData = externalIntegrationClient.getAllCourses().get()

            ResponseEntity.ok(
                mapOf(
                    "professorId" to professorId,
                    "professor" to (professorData ?: mapOf(
                        "error" to "Professor not found",
                        "fallbackUsed" to true
                    )),
                    "preferences" to preferences.map { p ->
                        mapOf(
                            "id" to p.id,
                            "academicYear" to p.academicYear,
                            "examSession" to p.examSession,
                            "status" to p.status,
                            "submittedAt" to p.submittedAt
                        )
                    },
                    "availableCourses" to mapOf(
                        "count" to coursesData["count"],
                        "serviceWorking" to true
                    ),
                    "servicesIntegrated" to mapOf(
                        "userManagementConnected" to (professorData != null),
                        "externalIntegrationConnected" to true,
                        "preferencesFound" to preferences.isNotEmpty()
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to fetch professor preference with user data", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch professor preference with integrated data",
                    "message" to when (e) {
                        is CompletionException -> e.cause?.message ?: e.message
                        else -> e.message
                    },
                    "professorId" to professorId
                )
            )
        }
    }

    @PostMapping("/seed-test-data")
    fun seedTestData(): ResponseEntity<Map<String, Any?>> {
        return try {
            val testPreference1 = ProfessorPreference(
                id = UUID.randomUUID(),
                professorId = UUID.randomUUID(),
                academicYear = "2024-2025",
                examSession = "WINTER_2025_MIDTERM"
            )

            val testPreference2 = ProfessorPreference(
                id = UUID.randomUUID(),
                professorId = UUID.randomUUID(),
                academicYear = "2024-2025",
                examSession = "WINTER_2025_FINAL"
            )

            val savedPreference1 = professorPreferenceRepository.save(testPreference1)
            val savedPreference2 = professorPreferenceRepository.save(testPreference2)

            val timePreference1 = TimePreference(
                preferenceSubmission = savedPreference1,
                dayOfWeek = 1,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(11, 0),
                preferenceLevel = PreferenceLevel.PREFERRED
            )

            val timePreference2 = TimePreference(
                preferenceSubmission = savedPreference1,
                dayOfWeek = 3,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(12, 0),
                preferenceLevel = PreferenceLevel.ACCEPTABLE
            )

            val timePreference3 = TimePreference(
                preferenceSubmission = savedPreference2,
                dayOfWeek = 2,
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(16, 0),
                preferenceLevel = PreferenceLevel.PREFERRED
            )

            timePreferenceRepository.saveAll(listOf(timePreference1, timePreference2, timePreference3))

            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "message" to "Test data seeded successfully",
                    "professorPreferences" to 2,
                    "timePreferences" to 3,
                    "createdPreferenceIds" to listOf(savedPreference1.id, savedPreference2.id)
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to seed test data",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/preferences")
    fun getAllPreferences(): ResponseEntity<Map<String, Any?>> {
        return try {
            val preferences = professorPreferenceRepository.findAll()
            val statistics = professorPreferenceRepository.getPreferenceStatistics()

            ResponseEntity.ok(
                mapOf(
                    "preferences" to preferences.map { preference ->
                        mapOf(
                            "id" to preference.id,
                            "professorId" to preference.professorId,
                            "academicYear" to preference.academicYear,
                            "examSession" to preference.examSession,
                            "status" to preference.status,
                            "submittedAt" to preference.submittedAt,
                            "createdAt" to preference.createdAt,
                            "updatedAt" to preference.updatedAt
                        )
                    },
                    "statistics" to statistics,
                    "count" to preferences.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch preferences",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/preferences/{id}")
    fun getPreferenceById(@PathVariable id: UUID): ResponseEntity<Map<String, Any?>> {
        return try {
            val preference = professorPreferenceRepository.findById(id)
            if (preference.isPresent) {
                val p = preference.get()
                val timePreferences = timePreferenceRepository.findByPreferenceSubmissionId(p.id!!)

                ResponseEntity.ok(
                    mapOf(
                        "preference" to mapOf(
                            "id" to p.id,
                            "professorId" to p.professorId,
                            "academicYear" to p.academicYear,
                            "examSession" to p.examSession,
                            "status" to p.status,
                            "submittedAt" to p.submittedAt,
                            "createdAt" to p.createdAt,
                            "updatedAt" to p.updatedAt
                        ),
                        "timePreferences" to timePreferences.map { tp ->
                            mapOf(
                                "id" to tp.id,
                                "dayOfWeek" to tp.dayOfWeek,
                                "startTime" to tp.startTime,
                                "endTime" to tp.endTime,
                                "preferenceLevel" to tp.preferenceLevel
                            )
                        }
                    )
                )
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch preference",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/preferences/professor/{professorId}")
    fun getPreferencesByProfessor(@PathVariable professorId: UUID): ResponseEntity<Map<String, Any?>> {
        return try {
            val preferences = professorPreferenceRepository.findByProfessorId(professorId)

            ResponseEntity.ok(
                mapOf(
                    "professorId" to professorId,
                    "preferences" to preferences.map { p ->
                        mapOf(
                            "id" to p.id,
                            "academicYear" to p.academicYear,
                            "examSession" to p.examSession,
                            "status" to p.status,
                            "submittedAt" to p.submittedAt
                        )
                    },
                    "count" to preferences.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch preferences for professor",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/time-preferences/statistics")
    fun getTimePreferenceStatistics(): ResponseEntity<Map<String, Any?>> {
        return try {
            val statistics = timePreferenceRepository.getTimePreferenceStatistics()
            val mostPreferred = timePreferenceRepository.findMostPreferredTimeSlots()

            ResponseEntity.ok(
                mapOf(
                    "timePreferenceStatistics" to statistics,
                    "mostPreferredTimeSlots" to mostPreferred,
                    "totalTimePreferences" to timePreferenceRepository.count()
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch time preference statistics",
                    "message" to e.message
                )
            )
        }
    }

    @PostMapping("/preferences/mock-submission")
    fun mockPreferenceSubmission(): ResponseEntity<Map<String, Any?>> {
        return try {
            val professorId = UUID.randomUUID()

            val preference = ProfessorPreference(
                professorId = professorId,
                academicYear = "2024-2025",
                examSession = "SPRING_2025_FINAL",
            )

            val savedPreference = professorPreferenceRepository.save(preference)

            val timePreferences = listOf(
                TimePreference(
                    preferenceSubmission = savedPreference,
                    dayOfWeek = 1, // Monday
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(10, 0),
                    preferenceLevel = PreferenceLevel.PREFERRED
                ),
                TimePreference(
                    preferenceSubmission = savedPreference,
                    dayOfWeek = 2, // Tuesday
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(11, 0),
                    preferenceLevel = PreferenceLevel.PREFERRED
                ),
                TimePreference(
                    preferenceSubmission = savedPreference,
                    dayOfWeek = 4, // Thursday
                    startTime = LocalTime.of(13, 0),
                    endTime = LocalTime.of(15, 0),
                    preferenceLevel = PreferenceLevel.ACCEPTABLE
                ),
                TimePreference(
                    preferenceSubmission = savedPreference,
                    dayOfWeek = 5, // Friday
                    startTime = LocalTime.of(16, 0),
                    endTime = LocalTime.of(18, 0),
                    preferenceLevel = PreferenceLevel.NOT_PREFERRED
                )
            )

            val savedTimePreferences = timePreferenceRepository.saveAll(timePreferences)

            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "message" to "Mock preference submission created successfully",
                    "professorId" to professorId,
                    "preferenceId" to savedPreference.id,
                    "academicYear" to savedPreference.academicYear,
                    "examSession" to savedPreference.examSession,
                    "timePreferencesCount" to savedTimePreferences.size,
                    "status" to savedPreference.status
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to create mock preference submission",
                    "message" to e.message
                )
            )
        }
    }

    @DeleteMapping("/preferences/clear-test-data")
    fun clearTestData(): ResponseEntity<Map<String, Any?>> {
        return try {
            val timePreferenceCount = timePreferenceRepository.count()
            val preferenceCount = professorPreferenceRepository.count()

            timePreferenceRepository.deleteAll()
            professorPreferenceRepository.deleteAll()

            ResponseEntity.ok(
                mapOf(
                    "message" to "Test data cleared successfully",
                    "deletedTimePreferences" to timePreferenceCount,
                    "deletedPreferences" to preferenceCount
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to clear test data",
                    "message" to e.message
                )
            )
        }
    }
}