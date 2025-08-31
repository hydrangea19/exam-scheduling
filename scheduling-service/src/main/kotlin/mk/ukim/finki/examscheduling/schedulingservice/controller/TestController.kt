package mk.ukim.finki.examscheduling.schedulingservice.controller

import mk.ukim.finki.examscheduling.schedulingservice.domain.AdjustmentLog
import mk.ukim.finki.examscheduling.schedulingservice.domain.ExamSessionSchedule
import mk.ukim.finki.examscheduling.schedulingservice.domain.ProfessorComment
import mk.ukim.finki.examscheduling.schedulingservice.domain.ScheduledExam
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.*
import mk.ukim.finki.examscheduling.schedulingservice.repository.AdjustmentLogRepository
import mk.ukim.finki.examscheduling.schedulingservice.repository.ExamSessionScheduleRepository
import mk.ukim.finki.examscheduling.schedulingservice.repository.ProfessorCommentRepository
import mk.ukim.finki.examscheduling.schedulingservice.repository.ScheduledExamRepository
import mk.ukim.finki.examscheduling.schedulingservice.service.EventPublisher
import mk.ukim.finki.examscheduling.schedulingservice.service.ExternalIntegrationClient
import mk.ukim.finki.examscheduling.schedulingservice.service.PreferenceManagementClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import java.util.concurrent.CompletionException


@RestController
@RequestMapping("/api/test")
class TestController @Autowired constructor(
    private val examSessionScheduleRepository: ExamSessionScheduleRepository,
    private val scheduledExamRepository: ScheduledExamRepository,
    private val professorCommentRepository: ProfessorCommentRepository,
    private val adjustmentLogRepository: AdjustmentLogRepository,
    private val preferenceManagementClient: PreferenceManagementClient,
    private val externalIntegrationClient: ExternalIntegrationClient,
    private val eventPublisher: EventPublisher
) {
    private val logger = LoggerFactory.getLogger(TestController::class.java)

    @GetMapping("/ping")
    fun ping(): Map<String, Any> {
        return mapOf(
            "message" to "Scheduling Service is running",
            "timestamp" to Instant.now(),
            "service" to "scheduling-service",
            "version" to "1.0.0-SNAPSHOT"
        )
    }

    // === Kafka Testing Endpoints ===

    @GetMapping("/kafka/health")
    fun kafkaHealth(): ResponseEntity<Map<String, Any?>> {
        return try {
            val healthEvent = mapOf(
                "eventType" to "HealthCheck",
                "service" to "scheduling-service",
                "timestamp" to Instant.now().toString(),
                "message" to "Kafka health check from scheduling service"
            )

            eventPublisher.publishSystemNotification(healthEvent, "health-check")

            ResponseEntity.ok(
                mapOf(
                    "status" to "HEALTHY",
                    "message" to "Scheduling service Kafka is working",
                    "kafkaProducer" to "CONNECTED",
                    "eventPublished" to healthEvent
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                mapOf(
                    "status" to "UNHEALTHY",
                    "error" to e.message
                )
            )
        }
    }

    @PostMapping("/kafka/test-event-consumption")
    fun testEventConsumption(): ResponseEntity<Map<String, Any?>> {
        return try {
            val testSessionId = "EVENT_TEST_${System.currentTimeMillis()}"

            logger.info("Testing event consumption workflow with session: {}", testSessionId)

            val schedulesBefore = examSessionScheduleRepository.count()

            ResponseEntity.ok(
                mapOf(
                    "status" to "TEST_INITIATED",
                    "message" to "Event consumption test ready - now publish events from preference-management",
                    "testSessionId" to testSessionId,
                    "instructions" to listOf(
                        "1. Call preference-management: POST /api/test/kafka/publish-preference-workflow-events",
                        "2. Watch scheduling service logs for event consumption",
                        "3. Check if new schedule was created"
                    ),
                    "scheduleCountBefore" to schedulesBefore
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to e.message
                )
            )
        }
    }

    @GetMapping("/kafka/consumed-events-status")
    fun getConsumedEventsStatus(): ResponseEntity<Map<String, Any?>> {
        return try {
            val recentSchedules = examSessionScheduleRepository.findAll()
                .sortedByDescending { it.createdAt }
                .take(5)

            val kafkaCreatedSchedules = recentSchedules.filter {
                it.examSessionPeriodId.startsWith("KAFKA_TEST") || it.examSessionPeriodId.startsWith("EVENT_TEST")
            }

            ResponseEntity.ok(
                mapOf(
                    "status" to "CONSUMPTION_STATUS",
                    "totalSchedules" to recentSchedules.size,
                    "kafkaCreatedSchedules" to kafkaCreatedSchedules.map { schedule ->
                        mapOf(
                            "id" to schedule.id,
                            "examSessionPeriodId" to schedule.examSessionPeriodId,
                            "status" to schedule.status,
                            "createdAt" to schedule.createdAt
                        )
                    },
                    "eventConsumptionWorking" to kafkaCreatedSchedules.isNotEmpty(),
                    "message" to if (kafkaCreatedSchedules.isNotEmpty()) {
                        "Event consumption is working - schedules created from Kafka events"
                    } else {
                        "No schedules created from Kafka events yet"
                    }
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to e.message
                )
            )
        }
    }

    @PostMapping("/kafka/publish-scheduling-event")
    fun publishSchedulingEvent(@RequestBody request: Map<String, Any>): ResponseEntity<Map<String, Any?>> {
        return try {
            val eventType = request["eventType"] as? String ?: "TestSchedulingEvent"
            val message = request["message"] as? String ?: "Test event from scheduling service"

            val event = mapOf(
                "eventType" to eventType,
                "service" to "scheduling-service",
                "message" to message,
                "timestamp" to Instant.now().toString(),
                "scheduleId" to UUID.randomUUID().toString(),
                "metadata" to request
            )

            eventPublisher.publishSchedulingEvent(event)

            ResponseEntity.ok(
                mapOf(
                    "status" to "SUCCESS",
                    "message" to "Scheduling event published",
                    "eventType" to eventType,
                    "eventPublished" to event
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to e.message
                )
            )
        }
    }

    // === Service Communication Testing Endpoints ===

    @GetMapping("/test-preference-management-service")
    fun testPreferenceManagementService(): Map<String, Any?> {
        return try {
            logger.info("Testing communication with preference management service")

            val pingResponse = preferenceManagementClient.ping().get()
            val preferencesResponse = preferenceManagementClient.getAllPreferences().get()

            mapOf(
                "status" to "SUCCESS",
                "message" to "Preference management service communication working",
                "preferenceManagementService" to mapOf(
                    "ping" to pingResponse,
                    "preferences" to mapOf(
                        "count" to preferencesResponse["count"],
                        "available" to ((preferencesResponse["count"]?.toString()?.toIntOrNull() ?: 0) > 0)
                    ),
                    "reachable" to true
                )
            )
        } catch (e: Exception) {
            logger.error("Preference management service communication failed", e)
            mapOf(
                "status" to "ERROR",
                "message" to "Preference management service communication failed",
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

    @GetMapping("/test-full-scheduling-integration")
    fun testFullSchedulingIntegration(): Map<String, Any?> {
        return try {
            logger.info("Testing full scheduling integration: Scheduling + Preference Management + External Integration")

            val preferenceServicePing = preferenceManagementClient.ping().get()
            val externalServicePing = externalIntegrationClient.ping().get()

            val preferencesData = preferenceManagementClient.getAllPreferences().get()
            val coursesData = externalIntegrationClient.getAllCourses().get()

            val mockSchedule = ExamSessionSchedule(
                examSessionPeriodId = "INTEGRATION_TEST_${System.currentTimeMillis()}",
                academicYear = "2024-2025",
                examSession = "INTEGRATION_TEST",
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 3, 15),
                status = ScheduleStatus.DRAFT
            )

            val savedSchedule = examSessionScheduleRepository.save(mockSchedule)

            val mockExam = ScheduledExam(
                scheduledExamId = "INTEGRATION_TEST_EXAM",
                courseId = "TEST_COURSE_001",
                courseName = "Integration Test Course",
                examDate = LocalDate.of(2025, 3, 5),
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(11, 0),
                roomId = "ROOM_TEST",
                roomName = "Test Room",
                roomCapacity = 50,
                studentCount = 30,
                mandatoryStatus = MandatoryStatus.MANDATORY,
                examSessionSchedule = savedSchedule
            )

            val savedExam = scheduledExamRepository.save(mockExam)

            mapOf(
                "status" to "SUCCESS",
                "message" to "Full scheduling integration test completed",
                "results" to mapOf(
                    "preferenceManagementService" to mapOf(
                        "status" to "CONNECTED",
                        "service" to preferenceServicePing["service"],
                        "preferencesCount" to preferencesData["count"]
                    ),
                    "externalIntegrationService" to mapOf(
                        "status" to "CONNECTED",
                        "service" to externalServicePing["service"],
                        "coursesCount" to coursesData["count"]
                    ),
                    "scheduleCreated" to mapOf(
                        "scheduleId" to savedSchedule.id,
                        "examSessionPeriodId" to savedSchedule.examSessionPeriodId,
                        "status" to savedSchedule.status,
                        "examsCount" to 1
                    ),
                    "mockExamCreated" to mapOf(
                        "examId" to savedExam.id,
                        "scheduledExamId" to savedExam.scheduledExamId,
                        "courseId" to savedExam.courseId,
                        "examDate" to savedExam.examDate
                    ),
                    "integrationWorking" to true
                )
            )
        } catch (e: Exception) {
            logger.error("Full scheduling integration test failed", e)
            mapOf(
                "status" to "ERROR",
                "message" to "Full scheduling integration test failed",
                "error" to when (e) {
                    is CompletionException -> e.cause?.message ?: e.message
                    else -> e.message
                },
                "integrationWorking" to false
            )
        }
    }

    @GetMapping("/test-schedule-generation-simulation")
    fun testScheduleGenerationSimulation(): ResponseEntity<Map<String, Any?>> {
        return try {
            logger.info("Simulating schedule generation with integrated data from multiple services")

            val academicYear = "2024-2025"
            val examSession = "WINTER_MIDTERM"
            val preferencesForSession =
                preferenceManagementClient.getPreferencesBySession(academicYear, examSession).get()

            val coursesData = externalIntegrationClient.getAllCourses().get()

            val schedule = ExamSessionSchedule(
                examSessionPeriodId = "GENERATED_${academicYear}_${examSession}_${System.currentTimeMillis()}",
                academicYear = academicYear,
                examSession = examSession,
                startDate = LocalDate.of(2025, 1, 15),
                endDate = LocalDate.of(2025, 1, 30),
                status = ScheduleStatus.GENERATED
            )

            val savedSchedule = examSessionScheduleRepository.save(schedule)

            val examCount = minOf(3, (coursesData["count"] as? Int) ?: 1)
            val generatedExams = (1..examCount).map { i ->
                ScheduledExam(
                    scheduledExamId = "SIM_EXAM_${i}_${savedSchedule.examSessionPeriodId}",
                    courseId = "COURSE_${String.format("%03d", i)}",
                    courseName = "Simulated Course $i",
                    examDate = LocalDate.of(2025, 1, 15 + (i * 2)),
                    startTime = LocalTime.of(8 + (i % 3) * 2, 0),
                    endTime = LocalTime.of(10 + (i % 3) * 2, 0),
                    roomId = "ROOM_${100 + i}",
                    roomName = "Simulated Room ${100 + i}",
                    roomCapacity = 40 + (i * 10),
                    studentCount = 25 + (i * 5),
                    mandatoryStatus = if (i % 2 == 0) MandatoryStatus.MANDATORY else MandatoryStatus.ELECTIVE,
                    examSessionSchedule = savedSchedule
                ).apply {
                    professorIds.add("PROF_${String.format("%03d", i)}")
                }
            }

            val savedExams = scheduledExamRepository.saveAll(generatedExams)

            ResponseEntity.ok(
                mapOf(
                    "message" to "Schedule generation simulation completed",
                    "simulationResults" to mapOf(
                        "schedule" to mapOf(
                            "id" to savedSchedule.id,
                            "examSessionPeriodId" to savedSchedule.examSessionPeriodId,
                            "academicYear" to savedSchedule.academicYear,
                            "examSession" to savedSchedule.examSession,
                            "status" to savedSchedule.status
                        ),
                        "generatedExams" to savedExams.map { exam ->
                            mapOf(
                                "id" to exam.id,
                                "scheduledExamId" to exam.scheduledExamId,
                                "courseId" to exam.courseId,
                                "courseName" to exam.courseName,
                                "examDate" to exam.examDate,
                                "timeSlot" to "${exam.startTime} - ${exam.endTime}",
                                "roomId" to exam.roomId,
                                "studentCount" to exam.studentCount
                            )
                        },
                        "dataIntegration" to mapOf(
                            "preferencesUsed" to preferencesForSession["professorsCount"],
                            "coursesAvailable" to coursesData["count"],
                            "servicesIntegrated" to 2
                        )
                    ),
                    "servicesCommunicated" to mapOf(
                        "preferenceManagementConnected" to true,
                        "externalIntegrationConnected" to true
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Schedule generation simulation failed", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Schedule generation simulation failed",
                    "message" to when (e) {
                        is CompletionException -> e.cause?.message ?: e.message
                        else -> e.message
                    }
                )
            )
        }
    }

    @GetMapping("/test-professor-preferences-integration/{professorId}")
    fun testProfessorPreferencesIntegration(@PathVariable professorId: UUID): ResponseEntity<Map<String, Any?>> {
        return try {
            logger.info("Testing professor preferences integration for professor: {}", professorId)

            val preferencesData = preferenceManagementClient.getPreferencesByProfessor(professorId).get()

            val coursesData = externalIntegrationClient.getAllCourses().get()

            val existingExams = scheduledExamRepository.findByProfessorId(professorId.toString())

            ResponseEntity.ok(
                mapOf(
                    "professorId" to professorId,
                    "professorPreferences" to preferencesData,
                    "existingScheduledExams" to existingExams.map { exam ->
                        mapOf(
                            "id" to exam.id,
                            "scheduledExamId" to exam.scheduledExamId,
                            "courseId" to exam.courseId,
                            "courseName" to exam.courseName,
                            "examDate" to exam.examDate,
                            "timeSlot" to "${exam.startTime} - ${exam.endTime}",
                            "roomId" to exam.roomId
                        )
                    },
                    "coursesContext" to mapOf(
                        "totalCourses" to coursesData["count"],
                        "serviceWorking" to true
                    ),
                    "integrationStatus" to mapOf(
                        "preferencesServiceConnected" to (preferencesData["count"] != null),
                        "coursesServiceConnected" to true,
                        "preferencesFound" to (((preferencesData["count"] as? Number)?.toInt()
                            ?: (preferencesData["count"] as? String)?.toIntOrNull() ?: 0) > 0),
                        "existingExamsFound" to existingExams.isNotEmpty()
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Professor preferences integration test failed", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Professor preferences integration test failed",
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
            val examSession = ExamSessionSchedule(
                examSessionPeriodId = "WINTER_2025_MIDTERM",
                academicYear = "2024-2025",
                examSession = "WINTER_MIDTERM",
                startDate = LocalDate.of(2025, 1, 15),
                endDate = LocalDate.of(2025, 1, 30),
                status = ScheduleStatus.DRAFT
            )

            val savedSession = examSessionScheduleRepository.save(examSession)

            val exam1 = ScheduledExam(
                scheduledExamId = "SOA_2025_WINTER_MIDTERM",
                courseId = "F18L3S155",
                courseName = "Service-Oriented Architecture",
                examDate = LocalDate.of(2025, 1, 20),
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(11, 0),
                roomId = "AMPH_A",
                roomName = "Amphitheater A",
                roomCapacity = 200,
                studentCount = 85,
                mandatoryStatus = MandatoryStatus.MANDATORY,
                examSessionSchedule = savedSession
            ).apply {
                professorIds.add("PROF_001")
                professorIds.add("PROF_002")
            }

            val exam2 = ScheduledExam(
                scheduledExamId = "WP_2025_WINTER_MIDTERM",
                courseId = "F18L3S142",
                courseName = "Web Programming",
                examDate = LocalDate.of(2025, 1, 22),
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(16, 0),
                roomId = "LAB_101",
                roomName = "Computer Lab 101",
                roomCapacity = 30,
                studentCount = 45,
                mandatoryStatus = MandatoryStatus.MANDATORY,
                examSessionSchedule = savedSession
            ).apply {
                professorIds.add("PROF_003")
            }

            val savedExams = scheduledExamRepository.saveAll(listOf(exam1, exam2))

            val comment1 = ProfessorComment(
                commentId = "COMMENT_001",
                professorId = "PROF_001",
                scheduledExamId = exam1.scheduledExamId,
                commentText = "The 9 AM time slot might be too early for students. Could we move it to 10 AM?",
                commentType = CommentType.TIME_CONFLICT,
                status = CommentStatus.SUBMITTED,
                examSessionSchedule = savedSession
            )

            val comment2 = ProfessorComment(
                commentId = "COMMENT_002",
                professorId = "PROF_003",
                scheduledExamId = exam2.scheduledExamId,
                commentText = "Lab 101 might be too small for 45 students. Can we get a larger room?",
                commentType = CommentType.TIME_CONFLICT,
                status = CommentStatus.UNDER_REVIEW,
                examSessionSchedule = savedSession
            )

            val savedComments = professorCommentRepository.saveAll(listOf(comment1, comment2))

            val adjustment = AdjustmentLog(
                adjustmentId = "ADJ_001",
                adminId = "ADMIN_001",
                commentId = comment1.commentId,
                scheduledExamId = exam1.scheduledExamId,
                adjustmentType = AdjustmentType.TIME_CHANGE,
                description = "Changed exam time from 9:00 AM to 10:00 AM based on professor feedback",
                oldValues = "startTime=09:00, endTime=11:00",
                newValues = "startTime=10:00, endTime=12:00",
                reason = "Professor requested later start time for student convenience",
                examSessionSchedule = savedSession
            )

            val savedAdjustment = adjustmentLogRepository.save(adjustment)

            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "message" to "Test data seeded successfully",
                    "examSessionSchedule" to savedSession.id,
                    "scheduledExams" to savedExams.size,
                    "professorComments" to savedComments.size,
                    "adjustmentLogs" to 1,
                    "data" to mapOf(
                        "sessionId" to savedSession.id,
                        "examIds" to savedExams.map { it.id },
                        "commentIds" to savedComments.map { it.id },
                        "adjustmentId" to savedAdjustment.id
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

    @GetMapping("/schedules")
    fun getAllSchedules(): ResponseEntity<Map<String, Any?>> {
        return try {
            val schedules = examSessionScheduleRepository.findAll()
            val statistics = examSessionScheduleRepository.getScheduleStatistics()

            ResponseEntity.ok(
                mapOf(
                    "schedules" to schedules.map { schedule ->
                        mapOf(
                            "id" to schedule.id,
                            "examSessionPeriodId" to schedule.examSessionPeriodId,
                            "academicYear" to schedule.academicYear,
                            "examSession" to schedule.examSession,
                            "startDate" to schedule.startDate,
                            "endDate" to schedule.endDate,
                            "status" to schedule.status,
                            "createdAt" to schedule.createdAt,
                            "finalizedAt" to schedule.finalizedAt,
                            "publishedAt" to schedule.publishedAt,
                            "examsCount" to schedule.scheduledExams.size,
                            "commentsCount" to schedule.professorComments.size,
                            "adjustmentsCount" to schedule.adjustmentLogs.size
                        )
                    },
                    "statistics" to statistics,
                    "count" to schedules.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch schedules",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/schedules/{id}")
    fun getScheduleById(@PathVariable id: UUID): ResponseEntity<Map<String, Any?>> {
        return try {
            val schedule = examSessionScheduleRepository.findById(id)
            if (schedule.isPresent) {
                val s = schedule.get()
                val exams = scheduledExamRepository.findByExamSessionScheduleId(s.id!!)
                val comments = professorCommentRepository.findByExamSessionScheduleId(s.id!!)
                val adjustments = adjustmentLogRepository.findByExamSessionScheduleId(s.id!!)

                ResponseEntity.ok(
                    mapOf(
                        "schedule" to mapOf(
                            "id" to s.id,
                            "examSessionPeriodId" to s.examSessionPeriodId,
                            "academicYear" to s.academicYear,
                            "examSession" to s.examSession,
                            "startDate" to s.startDate,
                            "endDate" to s.endDate,
                            "status" to s.status,
                            "createdAt" to s.createdAt,
                            "finalizedAt" to s.finalizedAt,
                            "publishedAt" to s.publishedAt
                        ),
                        "exams" to exams.map { exam ->
                            mapOf(
                                "id" to exam.id,
                                "scheduledExamId" to exam.scheduledExamId,
                                "courseId" to exam.courseId,
                                "courseName" to exam.courseName,
                                "examDate" to exam.examDate,
                                "startTime" to exam.startTime,
                                "endTime" to exam.endTime,
                                "roomId" to exam.roomId,
                                "roomName" to exam.roomName,
                                "studentCount" to exam.studentCount,
                                "mandatoryStatus" to exam.mandatoryStatus,
                                "professorIds" to exam.professorIds
                            )
                        },
                        "comments" to comments.map { comment ->
                            mapOf(
                                "id" to comment.id,
                                "commentId" to comment.commentId,
                                "professorId" to comment.professorId,
                                "scheduledExamId" to comment.scheduledExamId,
                                "commentText" to comment.commentText,
                                "commentType" to comment.commentType,
                                "status" to comment.status,
                                "submittedAt" to comment.submittedAt
                            )
                        },
                        "adjustments" to adjustments.map { adj ->
                            mapOf(
                                "id" to adj.id,
                                "adjustmentId" to adj.adjustmentId,
                                "adminId" to adj.adminId,
                                "adjustmentType" to adj.adjustmentType,
                                "description" to adj.description,
                                "timestamp" to adj.timestamp,
                                "status" to adj.status
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
                    "error" to "Failed to fetch schedule",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/exams/daily/{date}")
    fun getExamsByDate(@PathVariable date: LocalDate): ResponseEntity<Map<String, Any?>> {
        return try {
            val exams = scheduledExamRepository.findByExamDate(date)

            ResponseEntity.ok(
                mapOf(
                    "date" to date,
                    "exams" to exams.map { exam ->
                        mapOf(
                            "id" to exam.id,
                            "courseId" to exam.courseId,
                            "courseName" to exam.courseName,
                            "startTime" to exam.startTime,
                            "endTime" to exam.endTime,
                            "roomId" to exam.roomId,
                            "roomName" to exam.roomName,
                            "studentCount" to exam.studentCount,
                            "professorIds" to exam.professorIds
                        )
                    },
                    "count" to exams.size,
                    "totalStudents" to exams.sumOf { it.studentCount }
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch exams for date",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/comments/pending")
    fun getPendingComments(): ResponseEntity<Map<String, Any?>> {
        return try {
            val pendingComments = professorCommentRepository.findPendingComments()
            val statistics = professorCommentRepository.getCommentStatistics()

            ResponseEntity.ok(
                mapOf(
                    "pendingComments" to pendingComments.map { comment ->
                        mapOf(
                            "id" to comment.id,
                            "commentId" to comment.commentId,
                            "professorId" to comment.professorId,
                            "scheduledExamId" to comment.scheduledExamId,
                            "commentText" to comment.commentText,
                            "commentType" to comment.commentType,
                            "submittedAt" to comment.submittedAt
                        )
                    },
                    "statistics" to statistics,
                    "count" to pendingComments.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch pending comments",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/statistics/comprehensive")
    fun getComprehensiveStatistics(): ResponseEntity<Map<String, Any?>> {
        return try {
            val scheduleStats = examSessionScheduleRepository.getScheduleStatistics()
            val commentStats = professorCommentRepository.getCommentStatistics()
            val adjustmentStats = adjustmentLogRepository.getAdjustmentStatistics()
            val dailyExamStats = scheduledExamRepository.getDailyExamStatistics()

            ResponseEntity.ok(
                mapOf(
                    "scheduleStatistics" to scheduleStats,
                    "commentStatistics" to commentStats,
                    "adjustmentStatistics" to adjustmentStats,
                    "dailyExamStatistics" to dailyExamStats,
                    "overview" to mapOf(
                        "totalSchedules" to examSessionScheduleRepository.count(),
                        "totalExams" to scheduledExamRepository.count(),
                        "totalComments" to professorCommentRepository.count(),
                        "totalAdjustments" to adjustmentLogRepository.count()
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

    @PostMapping("/mock-schedule-generation")
    fun mockScheduleGeneration(): ResponseEntity<Map<String, Any?>> {
        return try {
            val examSession = ExamSessionSchedule(
                examSessionPeriodId = "SPRING_2025_FINAL_${UUID.randomUUID().toString().take(8)}",
                academicYear = "2024-2025",
                examSession = "SPRING_FINAL",
                startDate = LocalDate.of(2025, 6, 1),
                endDate = LocalDate.of(2025, 6, 20),
                status = ScheduleStatus.GENERATED
            )

            val savedSession = examSessionScheduleRepository.save(examSession)

            val mockExams = (1..5).map { i ->
                ScheduledExam(
                    scheduledExamId = "MOCK_EXAM_${i}_${savedSession.examSessionPeriodId}",
                    courseId = "F18L3S${150 + i}",
                    courseName = "Mock Course $i",
                    examDate = LocalDate.of(2025, 6, 2 + (i * 2)),
                    startTime = LocalTime.of(8 + (i % 4) * 2, 0),
                    endTime = LocalTime.of(10 + (i % 4) * 2, 0),
                    roomId = "ROOM_${100 + i}",
                    roomName = "Room ${100 + i}",
                    roomCapacity = 50 + (i * 10),
                    studentCount = 20 + (i * 5),
                    mandatoryStatus = if (i % 2 == 0) MandatoryStatus.MANDATORY else MandatoryStatus.ELECTIVE,
                    examSessionSchedule = savedSession
                ).apply {
                    professorIds.add("PROF_${String.format("%03d", i)}")
                }
            }

            val savedExams = scheduledExamRepository.saveAll(mockExams)

            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "message" to "Mock schedule generated successfully",
                    "sessionId" to savedSession.id,
                    "examSessionPeriodId" to savedSession.examSessionPeriodId,
                    "generatedExams" to savedExams.size,
                    "status" to savedSession.status,
                    "dateRange" to "${savedSession.startDate} to ${savedSession.endDate}"
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to generate mock schedule",
                    "message" to e.message
                )
            )
        }
    }

    @DeleteMapping("/clear-test-data")
    fun clearTestData(): ResponseEntity<Map<String, Any?>> {
        return try {
            val adjustmentCount = adjustmentLogRepository.count()
            val commentCount = professorCommentRepository.count()
            val examCount = scheduledExamRepository.count()
            val scheduleCount = examSessionScheduleRepository.count()

            adjustmentLogRepository.deleteAll()
            professorCommentRepository.deleteAll()
            scheduledExamRepository.deleteAll()
            examSessionScheduleRepository.deleteAll()

            ResponseEntity.ok(
                mapOf(
                    "message" to "Test data cleared successfully",
                    "deletedAdjustments" to adjustmentCount,
                    "deletedComments" to commentCount,
                    "deletedExams" to examCount,
                    "deletedSchedules" to scheduleCount
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