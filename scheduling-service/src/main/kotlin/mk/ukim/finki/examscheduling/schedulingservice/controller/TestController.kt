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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@RestController
@RequestMapping("/api/test")
class TestController @Autowired constructor(
    private val examSessionScheduleRepository: ExamSessionScheduleRepository,
    private val scheduledExamRepository: ScheduledExamRepository,
    private val professorCommentRepository: ProfessorCommentRepository,
    private val adjustmentLogRepository: AdjustmentLogRepository
) {

    @GetMapping("/ping")
    fun ping(): Map<String, Any> {
        return mapOf(
            "message" to "Scheduling Service is running",
            "timestamp" to Instant.now(),
            "service" to "scheduling-service",
            "version" to "1.0.0-SNAPSHOT"
        )
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
                commentType = CommentType.TIME_CHANGE_REQUEST,
                status = CommentStatus.SUBMITTED,
                examSessionSchedule = savedSession
            )

            val comment2 = ProfessorComment(
                commentId = "COMMENT_002",
                professorId = "PROF_003",
                scheduledExamId = exam2.scheduledExamId,
                commentText = "Lab 101 might be too small for 45 students. Can we get a larger room?",
                commentType = CommentType.ROOM_CHANGE_REQUEST,
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