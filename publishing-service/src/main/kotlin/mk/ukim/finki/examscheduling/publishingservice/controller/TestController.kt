package mk.ukim.finki.examscheduling.publishingservice.controller

import mk.ukim.finki.examscheduling.publishingservice.domain.PublicationRecord
import mk.ukim.finki.examscheduling.publishingservice.domain.PublishedSchedule
import mk.ukim.finki.examscheduling.publishingservice.domain.enums.PublicationStatus
import mk.ukim.finki.examscheduling.publishingservice.repository.PublicationRecordRepository
import mk.ukim.finki.examscheduling.publishingservice.repository.PublishedScheduleRepository
import mk.ukim.finki.examscheduling.publishingservice.service.SchedulingServiceClient
import mk.ukim.finki.examscheduling.publishingservice.service.UserManagementClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.util.concurrent.CompletionException

@RestController
@RequestMapping("/api/test")
class TestController @Autowired constructor(
    private val publishedScheduleRepository: PublishedScheduleRepository,
    private val publicationRecordRepository: PublicationRecordRepository,
    private val schedulingServiceClient: SchedulingServiceClient,
    private val userManagementClient: UserManagementClient
) {

    private val logger = LoggerFactory.getLogger(TestController::class.java)

    @GetMapping("/ping")
    fun ping(): Map<String, Any> {
        return mapOf(
            "message" to "Publishing Service is running",
            "timestamp" to Instant.now(),
            "service" to "publishing-service",
            "version" to "1.0.0-SNAPSHOT"
        )
    }

    // === Service Communication Testing Endpoints ===

    @GetMapping("/test-scheduling-service")
    fun testSchedulingService(): Map<String, Any?> {
        return try {
            logger.info("Testing communication with scheduling service")

            val pingResponse = schedulingServiceClient.ping().get()
            val schedulesResponse = schedulingServiceClient.getAllSchedules().get()

            mapOf(
                "status" to "SUCCESS",
                "message" to "Scheduling service communication working",
                "schedulingService" to mapOf(
                    "ping" to pingResponse,
                    "schedules" to mapOf(
                        "count" to schedulesResponse["count"],
                        "available" to ((schedulesResponse["count"]?.toString()?.toIntOrNull() ?: 0) > 0)
                    ),
                    "reachable" to true
                )
            )
        } catch (e: Exception) {
            logger.error("Scheduling service communication failed", e)
            mapOf(
                "status" to "ERROR",
                "message" to "Scheduling service communication failed",
                "error" to when (e) {
                    is CompletionException -> e.cause?.message ?: e.message
                    else -> e.message
                },
                "fallbackUsed" to true
            )
        }
    }

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

    @GetMapping("/test-full-publishing-integration")
    fun testFullPublishingIntegration(): Map<String, Any?> {
        return try {
            logger.info("Testing full publishing integration: Publishing + Scheduling + User Management")

            val schedulingServicePing = schedulingServiceClient.ping().get()
            val userServicePing = userManagementClient.ping().get()

            val finalizedSchedules = schedulingServiceClient.getFinalizedSchedules().get()
            val schedulingStatistics = schedulingServiceClient.getScheduleStatistics().get()

            val mockScheduleId = UUID.randomUUID()
            val publishedSchedule = PublishedSchedule(
                scheduleId = mockScheduleId,
                examSessionPeriodId = "INTEGRATION_TEST_${System.currentTimeMillis()}",
                academicYear = "2024-2025",
                examSession = "INTEGRATION_TEST",
                title = "Integration Test Published Schedule",
                description = "Published schedule created through service integration testing",
                publicationStatus = PublicationStatus.PUBLISHED,
                publishedAt = Instant.now(),
                publishedBy = "INTEGRATION_TEST_ADMIN",
                isPublic = true
            )

            val savedPublishedSchedule = publishedScheduleRepository.save(publishedSchedule)

            val record = PublicationRecord(
                publishedScheduleId = savedPublishedSchedule.id!!,
                recordType = "PUBLISHED",
                actionBy = "INTEGRATION_TEST_ADMIN",
                actionDescription = "Published schedule through integration test",
                metadata = """{"schedulingServiceConnected": true, "userManagementConnected": true}"""
            )
            publicationRecordRepository.save(record)

            mapOf(
                "status" to "SUCCESS",
                "message" to "Full publishing integration test completed",
                "results" to mapOf(
                    "schedulingService" to mapOf(
                        "status" to "CONNECTED",
                        "service" to schedulingServicePing["service"],
                        "finalizedSchedulesCount" to finalizedSchedules["count"],
                        "totalSchedulesInSystem" to (schedulingStatistics["overview"] as? Map<String, Any>)?.get("totalSchedules")
                    ),
                    "userManagementService" to mapOf(
                        "status" to "CONNECTED",
                        "service" to userServicePing["service"]
                    ),
                    "publishedScheduleCreated" to mapOf(
                        "id" to savedPublishedSchedule.id,
                        "examSessionPeriodId" to savedPublishedSchedule.examSessionPeriodId,
                        "status" to savedPublishedSchedule.publicationStatus,
                        "isPublic" to savedPublishedSchedule.isPublic,
                        "publishedAt" to savedPublishedSchedule.publishedAt
                    ),
                    "integrationWorking" to true
                )
            )
        } catch (e: Exception) {
            logger.error("Full publishing integration test failed", e)
            mapOf(
                "status" to "ERROR",
                "message" to "Full publishing integration test failed",
                "error" to when (e) {
                    is CompletionException -> e.cause?.message ?: e.message
                    else -> e.message
                },
                "integrationWorking" to false
            )
        }
    }

    @GetMapping("/test-schedule-publication-simulation")
    fun testSchedulePublicationSimulation(): ResponseEntity<Map<String, Any?>> {
        return try {
            logger.info("Simulating schedule publication with data from scheduling service")

            val finalizedSchedules = schedulingServiceClient.getFinalizedSchedules().get()
            val schedulesData = finalizedSchedules["schedules"] as? List<Map<String, Any>> ?: emptyList()

            if (schedulesData.isEmpty()) {
                return ResponseEntity.ok(
                    mapOf(
                        "message" to "No finalized schedules found in scheduling service",
                        "simulation" to "Mock publication created",
                        "recommendedAction" to "Create finalized schedules in scheduling service first"
                    )
                )
            }

            val scheduleToPublish = schedulesData.first()
            val scheduleId = UUID.fromString(scheduleToPublish["id"] as String)

            val publishedSchedule = PublishedSchedule(
                scheduleId = scheduleId,
                examSessionPeriodId = scheduleToPublish["examSessionPeriodId"] as String,
                academicYear = scheduleToPublish["academicYear"] as String,
                examSession = scheduleToPublish["examSession"] as String,
                title = "Published: ${scheduleToPublish["examSessionPeriodId"]}",
                description = "Automatically published from finalized schedule",
                publicationStatus = PublicationStatus.PUBLISHED,
                publishedAt = Instant.now(),
                publishedBy = "AUTO_PUBLISHER",
                isPublic = true
            )

            val savedPublishedSchedule = publishedScheduleRepository.save(publishedSchedule)

            val mockExamDate = LocalDate.of(2025, 3, 5)
            val examsData = schedulingServiceClient.getExamsByDate(mockExamDate).get()

            val record = PublicationRecord(
                publishedScheduleId = savedPublishedSchedule.id!!,
                recordType = "PUBLISHED",
                actionBy = "AUTO_PUBLISHER",
                actionDescription = "Automated publication from scheduling service data",
                metadata = """{"sourceScheduleId": "$scheduleId", "examsIncluded": ${examsData["count"]}}"""
            )
            publicationRecordRepository.save(record)

            ResponseEntity.ok(
                mapOf(
                    "message" to "Schedule publication simulation completed",
                    "simulationResults" to mapOf(
                        "sourceSchedule" to mapOf(
                            "id" to scheduleId,
                            "examSessionPeriodId" to scheduleToPublish["examSessionPeriodId"],
                            "status" to scheduleToPublish["status"]
                        ),
                        "publishedSchedule" to mapOf(
                            "id" to savedPublishedSchedule.id,
                            "title" to savedPublishedSchedule.title,
                            "publicationStatus" to savedPublishedSchedule.publicationStatus,
                            "isPublic" to savedPublishedSchedule.isPublic,
                            "publishedAt" to savedPublishedSchedule.publishedAt
                        ),
                        "relatedExams" to mapOf(
                            "examDate" to mockExamDate,
                            "examsCount" to examsData["count"]
                        )
                    ),
                    "serviceIntegration" to mapOf(
                        "schedulingServiceConnected" to true,
                        "finalizedSchedulesFound" to schedulesData.size,
                        "publicationCreated" to true
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Schedule publication simulation failed", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Schedule publication simulation failed",
                    "message" to when (e) {
                        is CompletionException -> e.cause?.message ?: e.message
                        else -> e.message
                    }
                )
            )
        }
    }

    @GetMapping("/test-published-schedule-integration/{scheduleId}")
    fun testPublishedScheduleIntegration(@PathVariable scheduleId: UUID): ResponseEntity<Map<String, Any?>> {
        return try {
            logger.info("Testing published schedule integration for schedule: {}", scheduleId)

            val scheduleData = schedulingServiceClient.getScheduleById(scheduleId).get()

            val publishedSchedule = publishedScheduleRepository.findByScheduleId(scheduleId)

            val records = if (publishedSchedule != null) {
                publicationRecordRepository.findByPublishedScheduleIdOrderByTimestampDesc(publishedSchedule.id!!)
            } else {
                emptyList()
            }

            ResponseEntity.ok(
                mapOf(
                    "scheduleId" to scheduleId,
                    "sourceScheduleData" to (scheduleData ?: mapOf(
                        "error" to "Schedule not found in scheduling service",
                        "fallbackUsed" to true
                    )),
                    "publishedSchedule" to (publishedSchedule?.let { ps ->
                        mapOf(
                            "id" to ps.id,
                            "title" to ps.title,
                            "publicationStatus" to ps.publicationStatus,
                            "publishedAt" to ps.publishedAt,
                            "isPublic" to ps.isPublic
                        )
                    } ?: mapOf("status" to "NOT_PUBLISHED")),
                    "publicationHistory" to records.map { record ->
                        mapOf(
                            "recordType" to record.recordType,
                            "actionBy" to record.actionBy,
                            "timestamp" to record.timestamp,
                            "description" to record.actionDescription
                        )
                    },
                    "integrationStatus" to mapOf(
                        "schedulingServiceConnected" to (scheduleData != null),
                        "scheduleExists" to (scheduleData != null),
                        "isPublished" to (publishedSchedule != null),
                        "publicationRecordsCount" to records.size
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Published schedule integration test failed", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Published schedule integration test failed",
                    "message" to when (e) {
                        is CompletionException -> e.cause?.message ?: e.message
                        else -> e.message
                    },
                    "scheduleId" to scheduleId
                )
            )
        }
    }

    @GetMapping("/test-finalized-schedules-ready-for-publication")
    fun testFinalizedSchedulesReadyForPublication(): ResponseEntity<Map<String, Any?>> {
        return try {
            logger.info("Testing which finalized schedules are ready for publication")

            val finalizedSchedules = schedulingServiceClient.getFinalizedSchedules().get()
            val schedulesFromService = finalizedSchedules["schedules"] as? List<Map<String, Any>> ?: emptyList()

            val publishedSchedules = publishedScheduleRepository.findAll()
            val publishedScheduleIds = publishedSchedules.map { it.scheduleId }.toSet()

            val readyForPublication = schedulesFromService.filter { schedule ->
                val scheduleId = UUID.fromString(schedule["id"] as String)
                !publishedScheduleIds.contains(scheduleId)
            }

            val alreadyPublished = schedulesFromService.filter { schedule ->
                val scheduleId = UUID.fromString(schedule["id"] as String)
                publishedScheduleIds.contains(scheduleId)
            }

            ResponseEntity.ok(
                mapOf(
                    "finalizedSchedulesFromSchedulingService" to mapOf(
                        "total" to schedulesFromService.size,
                        "schedules" to schedulesFromService.map { schedule ->
                            mapOf(
                                "id" to schedule["id"],
                                "examSessionPeriodId" to schedule["examSessionPeriodId"],
                                "status" to schedule["status"],
                                "finalizedAt" to schedule["finalizedAt"]
                            )
                        }
                    ),
                    "publicationStatus" to mapOf(
                        "readyForPublication" to readyForPublication.map { schedule ->
                            mapOf(
                                "id" to schedule["id"],
                                "examSessionPeriodId" to schedule["examSessionPeriodId"],
                                "canPublish" to true
                            )
                        },
                        "alreadyPublished" to alreadyPublished.map { schedule ->
                            mapOf(
                                "id" to schedule["id"],
                                "examSessionPeriodId" to schedule["examSessionPeriodId"],
                                "publishedStatus" to "ALREADY_PUBLISHED"
                            )
                        }
                    ),
                    "summary" to mapOf(
                        "totalFinalized" to schedulesFromService.size,
                        "readyForPublicationCount" to readyForPublication.size,
                        "alreadyPublishedCount" to alreadyPublished.size,
                        "publicationOpportunities" to readyForPublication.size
                    ),
                    "serviceIntegration" to mapOf(
                        "schedulingServiceConnected" to true,
                        "dataConsistency" to "VERIFIED"
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to check schedules ready for publication", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to check schedules ready for publication",
                    "message" to when (e) {
                        is CompletionException -> e.cause?.message ?: e.message
                        else -> e.message
                    }
                )
            )
        }
    }

    @PostMapping("/seed-test-data")
    fun seedTestData(): ResponseEntity<Map<String, Any?>> {
        return try {
            val schedule1 = PublishedSchedule(
                scheduleId = UUID.randomUUID(),
                examSessionPeriodId = "WINTER_2025_MIDTERM",
                academicYear = "2024-2025",
                examSession = "WINTER_MIDTERM",
                title = "Winter 2025 Midterm Exam Schedule",
                description = "Official exam schedule for winter semester midterm examinations",
                publicationStatus = PublicationStatus.PUBLISHED,
                publishedAt = Instant.now().minusSeconds(3600), // 1 hour ago
                publishedBy = "ADMIN_001",
                isPublic = true
            )

            val schedule2 = PublishedSchedule(
                scheduleId = UUID.randomUUID(),
                examSessionPeriodId = "SPRING_2025_FINAL",
                academicYear = "2024-2025",
                examSession = "SPRING_FINAL",
                title = "Spring 2025 Final Exam Schedule",
                description = "Draft schedule for spring semester final examinations",
                publicationStatus = PublicationStatus.DRAFT,
                publishedBy = "ADMIN_002",
                isPublic = false
            )

            val schedule3 = PublishedSchedule(
                scheduleId = UUID.randomUUID(),
                examSessionPeriodId = "FALL_2024_FINAL",
                academicYear = "2023-2024",
                examSession = "FALL_FINAL",
                title = "Fall 2024 Final Exam Schedule",
                description = "Archived schedule from previous academic year",
                publicationStatus = PublicationStatus.ARCHIVED,
                publishedAt = Instant.now().minusSeconds(7200 * 24), // Many days ago
                publishedBy = "ADMIN_001",
                isPublic = false
            )

            val savedSchedules = publishedScheduleRepository.saveAll(listOf(schedule1, schedule2, schedule3))

            val record1 = PublicationRecord(
                publishedScheduleId = schedule1.id!!,
                recordType = "PUBLISHED",
                actionBy = "ADMIN_001",
                actionDescription = "Initial publication of winter midterm schedule",
                metadata = """{"notificationsSent": 150, "publicAccess": true}"""
            )

            val record2 = PublicationRecord(
                publishedScheduleId = schedule1.id!!,
                recordType = "UPDATED",
                actionBy = "ADMIN_001",
                actionDescription = "Updated room allocation for Computer Science exams",
                metadata = """{"changedExams": 3, "affectedStudents": 45}"""
            )

            val record3 = PublicationRecord(
                publishedScheduleId = schedule2.id!!,
                recordType = "CREATED",
                actionBy = "ADMIN_002",
                actionDescription = "Created draft for spring final schedule",
                metadata = """{"totalExams": 28, "estimatedStudents": 800}"""
            )

            val savedRecords = publicationRecordRepository.saveAll(listOf(record1, record2, record3))

            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "message" to "Test data seeded successfully",
                    "publishedSchedules" to savedSchedules.size,
                    "publicationRecords" to savedRecords.size,
                    "data" to mapOf(
                        "scheduleIds" to savedSchedules.map { it.id },
                        "recordIds" to savedRecords.map { it.id }
                    )
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

    @GetMapping("/published-schedules")
    fun getAllPublishedSchedules(): ResponseEntity<Map<String, Any?>> {
        return try {
            val schedules = publishedScheduleRepository.findAll()
            val statistics = publishedScheduleRepository.getPublicationStatistics()

            ResponseEntity.ok(
                mapOf(
                    "schedules" to schedules.map { schedule ->
                        mapOf(
                            "id" to schedule.id,
                            "scheduleId" to schedule.scheduleId,
                            "examSessionPeriodId" to schedule.examSessionPeriodId,
                            "academicYear" to schedule.academicYear,
                            "examSession" to schedule.examSession,
                            "title" to schedule.title,
                            "description" to schedule.description,
                            "publicationStatus" to schedule.publicationStatus,
                            "publishedAt" to schedule.publishedAt,
                            "publishedBy" to schedule.publishedBy,
                            "isPublic" to schedule.isPublic,
                            "createdAt" to schedule.createdAt,
                            "updatedAt" to schedule.updatedAt
                        )
                    },
                    "statistics" to statistics,
                    "count" to schedules.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch published schedules",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/published-schedules/{id}")
    fun getPublishedScheduleById(@PathVariable id: UUID): ResponseEntity<Map<String, Any?>> {
        return try {
            val schedule = publishedScheduleRepository.findById(id)
            if (schedule.isPresent) {
                val s = schedule.get()
                val records = publicationRecordRepository.findByPublishedScheduleIdOrderByTimestampDesc(s.id!!)

                ResponseEntity.ok(
                    mapOf(
                        "schedule" to mapOf(
                            "id" to s.id,
                            "scheduleId" to s.scheduleId,
                            "examSessionPeriodId" to s.examSessionPeriodId,
                            "academicYear" to s.academicYear,
                            "examSession" to s.examSession,
                            "title" to s.title,
                            "description" to s.description,
                            "publicationStatus" to s.publicationStatus,
                            "publishedAt" to s.publishedAt,
                            "publishedBy" to s.publishedBy,
                            "isPublic" to s.isPublic,
                            "createdAt" to s.createdAt,
                            "updatedAt" to s.updatedAt
                        ),
                        "publicationRecords" to records.map { record ->
                            mapOf(
                                "id" to record.id,
                                "recordType" to record.recordType,
                                "actionBy" to record.actionBy,
                                "actionDescription" to record.actionDescription,
                                "metadata" to record.metadata,
                                "timestamp" to record.timestamp
                            )
                        },
                        "recordsCount" to records.size
                    )
                )
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch published schedule",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/published-schedules/status/{status}")
    fun getSchedulesByStatus(@PathVariable status: PublicationStatus): ResponseEntity<Map<String, Any?>> {
        return try {
            val schedules = publishedScheduleRepository.findByPublicationStatus(status)

            ResponseEntity.ok(
                mapOf(
                    "status" to status,
                    "schedules" to schedules.map { s ->
                        mapOf(
                            "id" to s.id,
                            "title" to s.title,
                            "examSessionPeriodId" to s.examSessionPeriodId,
                            "academicYear" to s.academicYear,
                            "examSession" to s.examSession,
                            "publishedAt" to s.publishedAt,
                            "publishedBy" to s.publishedBy,
                            "isPublic" to s.isPublic
                        )
                    },
                    "count" to schedules.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch schedules by status",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/published-schedules/academic-year/{year}")
    fun getSchedulesByAcademicYear(@PathVariable year: String): ResponseEntity<Map<String, Any?>> {
        return try {
            val schedules = publishedScheduleRepository.findByAcademicYear(year)

            ResponseEntity.ok(
                mapOf(
                    "academicYear" to year,
                    "schedules" to schedules.map { s ->
                        mapOf(
                            "id" to s.id,
                            "title" to s.title,
                            "examSessionPeriodId" to s.examSessionPeriodId,
                            "examSession" to s.examSession,
                            "publicationStatus" to s.publicationStatus,
                            "publishedAt" to s.publishedAt,
                            "isPublic" to s.isPublic
                        )
                    },
                    "count" to schedules.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch schedules by academic year",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/published-schedules/public")
    fun getPublicSchedules(): ResponseEntity<Map<String, Any?>> {
        return try {
            val publicSchedules = publishedScheduleRepository.findPublicSchedules()

            ResponseEntity.ok(
                mapOf(
                    "publicSchedules" to publicSchedules.map { s ->
                        mapOf(
                            "id" to s.id,
                            "title" to s.title,
                            "examSessionPeriodId" to s.examSessionPeriodId,
                            "academicYear" to s.academicYear,
                            "examSession" to s.examSession,
                            "description" to s.description,
                            "publishedAt" to s.publishedAt
                        )
                    },
                    "count" to publicSchedules.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch public schedules",
                    "message" to e.message
                )
            )
        }
    }

    @PostMapping("/published-schedules/create")
    fun createPublishedSchedule(@RequestBody request: Map<String, Any>): ResponseEntity<Map<String, Any?>> {
        return try {
            val newSchedule = PublishedSchedule(
                scheduleId = UUID.fromString(request["scheduleId"] as String),
                examSessionPeriodId = request["examSessionPeriodId"] as String,
                academicYear = request["academicYear"] as String,
                examSession = request["examSession"] as String,
                title = request["title"] as String,
                description = request["description"] as String?,
                publicationStatus = PublicationStatus.valueOf(
                    request.getOrDefault("publicationStatus", "DRAFT") as String
                ),
                publishedBy = request["publishedBy"] as String?,
                isPublic = request.getOrDefault("isPublic", false) as Boolean
            )

            val savedSchedule = publishedScheduleRepository.save(newSchedule)

            val record = PublicationRecord(
                publishedScheduleId = savedSchedule.id!!,
                recordType = "CREATED",
                actionBy = request["publishedBy"] as String? ?: "SYSTEM",
                actionDescription = "Created new published schedule: ${savedSchedule.title}"
            )
            publicationRecordRepository.save(record)

            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "message" to "Published schedule created successfully",
                    "id" to savedSchedule.id,
                    "examSessionPeriodId" to savedSchedule.examSessionPeriodId,
                    "title" to savedSchedule.title,
                    "publicationStatus" to savedSchedule.publicationStatus
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to create published schedule",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/publication-records")
    fun getAllPublicationRecords(): ResponseEntity<Map<String, Any?>> {
        return try {
            val records = publicationRecordRepository.findAllOrderByTimestampDesc()
            val statistics = publicationRecordRepository.getRecordStatistics()

            ResponseEntity.ok(
                mapOf(
                    "records" to records.map { record ->
                        mapOf(
                            "id" to record.id,
                            "publishedScheduleId" to record.publishedScheduleId,
                            "recordType" to record.recordType,
                            "actionBy" to record.actionBy,
                            "actionDescription" to record.actionDescription,
                            "metadata" to record.metadata,
                            "timestamp" to record.timestamp
                        )
                    },
                    "statistics" to statistics,
                    "count" to records.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch publication records",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/publication-records/action-by/{actionBy}")
    fun getRecordsByActionBy(@PathVariable actionBy: String): ResponseEntity<Map<String, Any?>> {
        return try {
            val records = publicationRecordRepository.findByActionByOrderByTimestampDesc(actionBy)

            ResponseEntity.ok(
                mapOf(
                    "actionBy" to actionBy,
                    "records" to records.map { record ->
                        mapOf(
                            "id" to record.id,
                            "publishedScheduleId" to record.publishedScheduleId,
                            "recordType" to record.recordType,
                            "actionDescription" to record.actionDescription,
                            "timestamp" to record.timestamp
                        )
                    },
                    "count" to records.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch records by action by",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/statistics/comprehensive")
    fun getComprehensiveStatistics(): ResponseEntity<Map<String, Any?>> {
        return try {
            val publicationStats = publishedScheduleRepository.getPublicationStatistics()
            val recordStats = publicationRecordRepository.getRecordStatistics()

            ResponseEntity.ok(
                mapOf(
                    "publicationStatistics" to publicationStats,
                    "recordStatistics" to recordStats,
                    "overview" to mapOf(
                        "totalPublishedSchedules" to publishedScheduleRepository.count(),
                        "totalPublicationRecords" to publicationRecordRepository.count(),
                        "publishedSchedulesCount" to publishedScheduleRepository.countByPublicationStatus(
                            PublicationStatus.PUBLISHED
                        ),
                        "draftSchedulesCount" to publishedScheduleRepository.countByPublicationStatus(PublicationStatus.DRAFT),
                        "publicSchedulesCount" to publishedScheduleRepository.findByIsPublic(true).size
                    )
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch comprehensive statistics",
                    "message" to e.message
                )
            )
        }
    }

    @PostMapping("/mock-publication")
    fun mockPublication(): ResponseEntity<Map<String, Any?>> {
        return try {
            val scheduleId = UUID.randomUUID()
            val sessionPeriodId = "MOCK_${UUID.randomUUID().toString().take(8)}_EXAM"

            val schedule = PublishedSchedule(
                scheduleId = scheduleId,
                examSessionPeriodId = sessionPeriodId,
                academicYear = "2024-2025",
                examSession = "MOCK_EXAM",
                title = "Mock Exam Schedule - ${Instant.now()}",
                description = "This is a mock publication created for testing purposes",
                publicationStatus = PublicationStatus.PUBLISHED,
                publishedAt = Instant.now(),
                publishedBy = "TEST_ADMIN",
                isPublic = true
            )

            val savedSchedule = publishedScheduleRepository.save(schedule)

            val record = PublicationRecord(
                publishedScheduleId = savedSchedule.id!!,
                recordType = "PUBLISHED",
                actionBy = "TEST_ADMIN",
                actionDescription = "Mock publication created for testing",
                metadata = """{"testData": true, "automated": true}"""
            )
            val savedRecord = publicationRecordRepository.save(record)

            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "message" to "Mock publication created successfully",
                    "scheduleId" to savedSchedule.id,
                    "examSessionPeriodId" to savedSchedule.examSessionPeriodId,
                    "title" to savedSchedule.title,
                    "publicationStatus" to savedSchedule.publicationStatus,
                    "recordId" to savedRecord.id,
                    "publishedAt" to savedSchedule.publishedAt
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to create mock publication",
                    "message" to e.message
                )
            )
        }
    }

    @DeleteMapping("/clear-test-data")
    fun clearTestData(): ResponseEntity<Map<String, Any?>> {
        return try {
            val recordCount = publicationRecordRepository.count()
            val scheduleCount = publishedScheduleRepository.count()

            publicationRecordRepository.deleteAll()
            publishedScheduleRepository.deleteAll()

            ResponseEntity.ok(
                mapOf(
                    "message" to "Test data cleared successfully",
                    "deletedPublicationRecords" to recordCount,
                    "deletedPublishedSchedules" to scheduleCount
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