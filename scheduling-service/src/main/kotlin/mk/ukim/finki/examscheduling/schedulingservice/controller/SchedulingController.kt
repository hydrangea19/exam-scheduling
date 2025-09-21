package mk.ukim.finki.examscheduling.schedulingservice.controller

import jakarta.validation.Valid
import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.CommentStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.MandatoryStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ScheduleStatus
import mk.ukim.finki.examscheduling.schedulingservice.repository.*
import mk.ukim.finki.examscheduling.schedulingservice.service.AdvancedSchedulingService
import mk.ukim.finki.examscheduling.schedulingservice.service.ConflictAnalysisService
import mk.ukim.finki.examscheduling.schedulingservice.service.QualityScoringService
import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.axonframework.commandhandling.gateway.CommandGateway
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@RestController
@RequestMapping("/api/scheduling")
class SchedulingController(
    private val commandGateway: CommandGateway,
    private val examSessionScheduleRepository: ExamSessionScheduleRepository,
    private val scheduledExamRepository: ScheduledExamRepository,
    private val professorCommentRepository: ProfessorCommentRepository,
    private val adjustmentLogRepository: AdjustmentLogRepository,
    private val qualityScoringService: QualityScoringService,
    private val conflictAnalysisService: ConflictAnalysisService,
    private val scheduleVersionRepository: ScheduleVersionRepository,
    private val advancedSchedulingService: AdvancedSchedulingService
) {

    private val logger = LoggerFactory.getLogger(SchedulingController::class.java)

    @PostMapping("/schedules")
    @PreAuthorize("hasRole('ADMIN')")
    fun createSchedule(@Valid @RequestBody request: CreateScheduleRequest): ResponseEntity<ScheduleResponse> {
        logger.info("Creating new schedule for session: {}", request.examSessionPeriodId)

        val scheduleId = UUID.randomUUID()

        try {
            val schedule = ExamSessionSchedule(
                id = scheduleId,
                examSessionPeriodId = request.examSessionPeriodId,
                academicYear = request.academicYear,
                examSession = request.examSession,
                startDate = request.startDate,
                endDate = request.endDate,
                status = ScheduleStatus.DRAFT,
                createdAt = Instant.now()
            )

            val savedSchedule = examSessionScheduleRepository.save(schedule)
            return ResponseEntity.status(HttpStatus.CREATED).body(savedSchedule.toResponse())

        } catch (e: Exception) {
            logger.error("Failed to create schedule", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/schedules")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    fun getSchedules(
        @RequestParam(required = false) academicYear: String?,
        @RequestParam(required = false) examSession: String?,
        @RequestParam(required = false) status: ScheduleStatus?,
        pageable: Pageable
    ): ResponseEntity<Page<ScheduleResponse>> {
        logger.debug(
            "Fetching schedules with filters: year={}, session={}, status={}",
            academicYear,
            examSession,
            status
        )

        val schedules = when {
            academicYear != null && examSession != null ->
                examSessionScheduleRepository.findByAcademicYearAndExamSession(academicYear, examSession)

            status != null ->
                examSessionScheduleRepository.findByStatus(status)

            else ->
                examSessionScheduleRepository.findAll()
        }

        val responses = schedules.map { it.toResponse() }
        return ResponseEntity.ok(org.springframework.data.domain.PageImpl(responses, pageable, responses.size.toLong()))
    }

    @GetMapping("/schedules/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    fun getSchedule(@PathVariable scheduleId: UUID): ResponseEntity<DetailedScheduleResponse> {
        logger.debug("Fetching detailed schedule: {}", scheduleId)

        val schedule = examSessionScheduleRepository.findById(scheduleId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val exams = scheduledExamRepository.findByExamSessionScheduleId(scheduleId)
        val comments = professorCommentRepository.findByExamSessionScheduleId(scheduleId)
        val adjustments = adjustmentLogRepository.findByExamSessionScheduleId(scheduleId)
        val metrics = qualityScoringService.getLatestMetrics(scheduleId)
        val conflicts = conflictAnalysisService.getCurrentConflicts(scheduleId)

        val response = DetailedScheduleResponse(
            schedule = schedule.toResponse(),
            exams = exams.map { it.toResponse() },
            comments = comments.map { it.toResponse() },
            adjustments = adjustments.map { it.toResponse() },
            metrics = metrics?.toResponse(),
            conflicts = conflicts.map { it.toResponse() }
        )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/schedules/{scheduleId}/generate")
    @PreAuthorize("hasRole('ADMIN')")
    fun generateSchedule(@PathVariable scheduleId: UUID): ResponseEntity<GenerationResponse> {
        logger.info("Triggering schedule generation for: {}", scheduleId)

        try {
            val schedule = examSessionScheduleRepository.findById(scheduleId).orElse(null)
                ?: return ResponseEntity.notFound().build()

            val examPeriod = ExamPeriod(
                examSessionPeriodId = schedule.examSessionPeriodId,
                academicYear = schedule.academicYear,
                examSession = schedule.examSession,
                startDate = schedule.startDate,
                endDate = schedule.endDate
            )

            val courseEnrollmentData = createMockEnrollmentData()
            val courseAccreditationData = createMockAccreditationData()
            val professorPreferences = createMockPreferences()
            val availableRooms = createMockRooms()

            val result = advancedSchedulingService.generateOptimalSchedule(
                courseEnrollmentData = courseEnrollmentData,
                courseAccreditationData = courseAccreditationData,
                professorPreferences = professorPreferences,
                availableRooms = availableRooms,
                examPeriod = examPeriod
            )

            logger.info("Schedule generated: {} exams with quality {}",
                result.scheduledExams.size, result.qualityScore)

            return ResponseEntity.ok(
                GenerationResponse(
                    scheduleId = scheduleId,
                    status = "COMPLETED",
                    message = "Generated ${result.scheduledExams.size} exams with quality score ${result.qualityScore}",
                    estimatedCompletionTime = Instant.now()
                )
            )

        } catch (e: Exception) {
            logger.error("Failed to generate schedule", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GenerationResponse(
                    scheduleId = scheduleId,
                    status = "FAILED",
                    message = "Generation failed: ${e.message}",
                    estimatedCompletionTime = null
                ))
        }
    }

    private fun createMockEnrollmentData() = mapOf(
        "CS101" to CourseEnrollmentInfo("CS101", 50, mapOf()),
        "MATH201" to CourseEnrollmentInfo("MATH201", 30, mapOf()),
        "ENG301" to CourseEnrollmentInfo("ENG301", 25, mapOf())
    )

    private fun createMockAccreditationData() = mapOf(
        "CS101" to CourseAccreditationInfo(
            courseId = "CS101",
            courseName = "Computer Science",
            mandatoryStatus = MandatoryStatus.MANDATORY,
            credits = 6,
            professorIds = setOf("PROF001"),
            prerequisites = emptySet(),
            accreditationDetails = mapOf()
        ),
        "MATH201" to CourseAccreditationInfo(
            courseId = "MATH201",
            courseName = "Mathematics",
            mandatoryStatus = MandatoryStatus.MANDATORY,
            credits = 5,
            professorIds = setOf("PROF002"),
            prerequisites = emptySet(),
            accreditationDetails = mapOf()
        ),
        "ENG301" to CourseAccreditationInfo(
            courseId = "ENG301",
            courseName = "English",
            mandatoryStatus = MandatoryStatus.ELECTIVE,
            credits = 3,
            professorIds = setOf("PROF003"),
            prerequisites = emptySet(),
            accreditationDetails = mapOf()
        )
    )

    private fun createMockPreferences() = listOf(
        ProfessorPreferenceInfo(
            preferenceId = "PREF001",
            professorId = "PROF001",
            courseId = "CS101",
            preferredDates = listOf(LocalDate.of(2025, 6, 16)),
            preferredTimeSlots = listOf(
                TimeSlotPreference(LocalTime.of(9, 0), LocalTime.of(11, 0))
            ),
            preferredRooms = listOf("ROOM_A101"),
            unavailableDates = emptyList(),
            unavailableTimeSlots = emptyList(),
            specialRequirements = "Morning preference",
            priority = 1
        )
    )

    private fun createMockRooms() = listOf(
        RoomInfo("ROOM_A101", "Amphitheater A101", 80, setOf("projector"), "Building A", true),
        RoomInfo("ROOM_B205", "Classroom B205", 40, setOf("whiteboard"), "Building B", true),
        RoomInfo("ROOM_C302", "Lab C302", 30, setOf("computers"), "Building C", true)
    )

    @PostMapping("/schedules/{scheduleId}/generate-direct")
    fun generateScheduleDirect(@PathVariable scheduleId: UUID): ResponseEntity<Map<String, Any?>> {
        logger.info("Direct schedule generation test for: {}", scheduleId)

        try {
            val schedule = examSessionScheduleRepository.findById(scheduleId).orElse(null)
                ?: return ResponseEntity.badRequest().body(mapOf(
                    "scheduleId" to scheduleId,
                    "status" to "FAILED",
                    "error" to "Schedule not found"
                ))

            val webClient = WebClient.builder()
                .baseUrl("http://localhost:8009")
                .build()

            val requestData = mapOf(
                "examPeriod" to mapOf(
                    "examSessionPeriodId" to "TEST_SESSION_2025",
                    "academicYear" to "2024-2025",
                    "examSession" to "Test Session",
                    "startDate" to "2025-06-15",
                    "endDate" to "2025-06-20"
                ),
                "courses" to listOf(
                    mapOf(
                        "courseId" to "CS101",
                        "courseName" to "Computer Science",
                        "studentCount" to 50,
                        "professorIds" to listOf("PROF001"),
                        "mandatoryStatus" to "MANDATORY",
                        "estimatedDuration" to 120,
                        "requiredEquipment" to emptyList<String>(),
                        "accessibilityRequired" to false
                    ),
                    mapOf(
                        "courseId" to "MATH201",
                        "courseName" to "Mathematics",
                        "studentCount" to 30,
                        "professorIds" to listOf("PROF002"),
                        "mandatoryStatus" to "MANDATORY",
                        "estimatedDuration" to 120,
                        "requiredEquipment" to emptyList<String>(),
                        "accessibilityRequired" to false
                    )
                ),
                "availableRooms" to listOf(
                    mapOf(
                        "roomId" to "ROOM_A101",
                        "roomName" to "Amphitheater A101",
                        "capacity" to 80,
                        "equipment" to listOf("projector"),
                        "location" to "Building A",
                        "accessibility" to true
                    ),
                    mapOf(
                        "roomId" to "ROOM_B205",
                        "roomName" to "Classroom B205",
                        "capacity" to 40,
                        "equipment" to listOf("whiteboard"),
                        "location" to "Building B",
                        "accessibility" to true
                    )
                ),
                "professorPreferences" to listOf(
                    mapOf(
                        "preferenceId" to "PREF001",
                        "professorId" to "PROF001",
                        "courseId" to "CS101",
                        "preferredDates" to listOf("2025-06-16"),
                        "preferredTimeSlots" to listOf(
                            mapOf("startTime" to "09:00:00", "endTime" to "11:00:00")
                        ),
                        "preferredRooms" to listOf("ROOM_A101"),
                        "unavailableDates" to emptyList<String>(),
                        "unavailableTimeSlots" to emptyList<String>(),
                        "specialRequirements" to "Morning preference",
                        "priority" to 1
                    )
                ),
                "institutionalConstraints" to mapOf(
                    "workingHours" to mapOf(
                        "startTime" to "08:00:00",
                        "endTime" to "18:00:00"
                    ),
                    "minimumExamDuration" to 90,
                    "minimumGapMinutes" to 30,
                    "maxExamsPerDay" to 6,
                    "maxExamsPerRoom" to 8,
                    "allowWeekendExams" to false
                )
            )

            val response = webClient
                .post()
                .uri("/api/schedule/generate")
                .bodyValue(requestData)
                .retrieve()
                .bodyToMono(object : org.springframework.core.ParameterizedTypeReference<Map<String, Any>>() {})
                .block()

           logger.info("Python service response: {}", response)

            try {
                val generatedExams = response?.get("scheduledExams") as? List<Map<String, Any>>

                generatedExams?.forEach { examData ->
                    val scheduledExam = ScheduledExam(
                        scheduledExamId = examData["scheduledExamId"] as String,
                        courseId = examData["courseId"] as String,
                        courseName = examData["courseName"] as String,
                        examDate = LocalDate.parse(examData["examDate"] as String),
                        startTime = LocalTime.parse(examData["startTime"] as String),
                        endTime = LocalTime.parse(examData["endTime"] as String),
                        roomId = examData["roomId"] as? String,
                        roomName = examData["roomName"] as? String,
                        roomCapacity = examData["roomCapacity"] as? Int,
                        studentCount = examData["studentCount"] as? Int ?: 0,
                        mandatoryStatus = MandatoryStatus.valueOf(examData["mandatoryStatus"] as String),
                        examSessionSchedule = schedule
                    )
                    scheduledExamRepository.save(scheduledExam)
                }

                logger.info("Saved {} exams to database", generatedExams?.size ?: 0)
            } catch (e: Exception) {
                logger.warn("Failed to parse Python response, but generation call succeeded", e)
            }

            schedule.status = ScheduleStatus.GENERATED
            examSessionScheduleRepository.save(schedule)
            logger.info("Updated schedule status to GENERATED")

            logger.info("Python service response: {}", response)

            return ResponseEntity.ok(mapOf(
                "scheduleId" to scheduleId,
                "status" to "SUCCESS",
                "pythonResponse" to response,
                "message" to "Direct Python integration working!"
            ))

        } catch (e: Exception) {
            logger.error("Direct generation failed", e)
            return ResponseEntity.ok(mapOf(
                "scheduleId" to scheduleId,
                "status" to "FAILED",
                "error" to e.message,
                "message" to "Check if Python service is running on port 8009"
            ))
        }
    }

    @PostMapping("/schedules/{scheduleId}/publish-for-review")
    @PreAuthorize("hasRole('ADMIN')")
    fun publishForReview(
        @PathVariable scheduleId: UUID,
        @RequestBody request: PublishForReviewRequest
    ): ResponseEntity<Void> {
        logger.info("Publishing schedule for review: {}", scheduleId)

        try {
            val schedule = examSessionScheduleRepository.findById(scheduleId).orElse(null)
                ?: return ResponseEntity.notFound().build()

            schedule.status = ScheduleStatus.PUBLISHED_FOR_REVIEW
            schedule.publishedAt = Instant.now()
            examSessionScheduleRepository.save(schedule)

            return ResponseEntity.ok().build()

        } catch (e: Exception) {
            logger.error("Failed to publish schedule for review", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/schedules/{scheduleId}/finalize")
    @PreAuthorize("hasRole('ADMIN')")
    fun finalizeSchedule(
        @PathVariable scheduleId: UUID,
        @RequestBody request: FinalizeScheduleRequest
    ): ResponseEntity<Void> {
        val schedule = examSessionScheduleRepository.findById(scheduleId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        // Update schedule status
        schedule.status = ScheduleStatus.FINALIZED
        schedule.finalizedAt = Instant.now()
        examSessionScheduleRepository.save(schedule)

        val webClient = WebClient.builder()
            .baseUrl("http://localhost:8005")
            .build()

        val publishRequest = mapOf(
            "scheduleId" to scheduleId,
            "examSessionPeriodId" to schedule.examSessionPeriodId,
            "academicYear" to schedule.academicYear,
            "examSession" to schedule.examSession,
            "title" to "${schedule.academicYear} ${schedule.examSession} Exam Schedule",
            "description" to "Finalized exam schedule for ${schedule.examSession} ${schedule.academicYear}",
            "publishedBy" to getCurrentUserId(),
            "isPublic" to true
        )

        webClient.post()
            .uri("/api/publishing/schedules")
            .bodyValue(publishRequest)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        logger.info("Published schedule record created successfully")
        return ResponseEntity.ok().build()
    }

    // === Exam Management Endpoints ===

    @PostMapping("/schedules/{scheduleId}/exams")
    @PreAuthorize("hasRole('ADMIN')")
    fun addExam(
        @PathVariable scheduleId: UUID,
        @Valid @RequestBody request: AddExamRequest
    ): ResponseEntity<ExamResponse> {
        logger.info("Adding exam to schedule: {}, course: {}", scheduleId, request.courseId)

        try {
            val schedule = examSessionScheduleRepository.findById(scheduleId).orElse(null)
                ?: return ResponseEntity.notFound().build()

            val scheduledExam = ScheduledExam(
                scheduledExamId = request.scheduledExamId ?: "${request.courseId}_${scheduleId}",
                courseId = request.courseId,
                courseName = request.courseName,
                examDate = request.examDate,
                startTime = request.startTime,
                endTime = request.endTime,
                roomId = request.roomId,
                roomName = request.roomName,
                roomCapacity = request.roomCapacity,
                studentCount = request.studentCount,
                mandatoryStatus = request.mandatoryStatus,
                examSessionSchedule = schedule
            )

            val savedExam = scheduledExamRepository.save(scheduledExam)
            return ResponseEntity.status(HttpStatus.CREATED).body(savedExam.toResponse())

        } catch (e: Exception) {
            logger.error("Failed to add exam", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PutMapping("/schedules/{scheduleId}/exams/{examId}/time")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateExamTime(
        @PathVariable scheduleId: UUID,
        @PathVariable examId: String,
        @Valid @RequestBody request: UpdateExamTimeRequest
    ): ResponseEntity<Void> {
        logger.info("Updating exam time: {}", examId)

        try {
            commandGateway.sendAndWait<Void>(
                UpdateScheduledExamTimeCommand(
                    scheduleId = scheduleId,
                    scheduledExamId = examId,
                    newExamDate = request.examDate,
                    newStartTime = request.startTime,
                    newEndTime = request.endTime,
                    reason = request.reason,
                    updatedBy = getCurrentUserId()
                )
            )

            return ResponseEntity.ok().build()

        } catch (e: Exception) {
            logger.error("Failed to update exam time", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @PutMapping("/schedules/{scheduleId}/exams/{examId}/space")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateExamSpace(
        @PathVariable scheduleId: UUID,
        @PathVariable examId: String,
        @Valid @RequestBody request: UpdateExamSpaceRequest
    ): ResponseEntity<Void> {
        logger.info("Updating exam space: {}", examId)

        try {
            commandGateway.sendAndWait<Void>(
                UpdateScheduledExamSpaceCommand(
                    scheduleId = scheduleId,
                    scheduledExamId = examId,
                    newRoomId = request.roomId,
                    newRoomName = request.roomName,
                    newRoomCapacity = request.roomCapacity,
                    reason = request.reason,
                    updatedBy = getCurrentUserId()
                )
            )

            return ResponseEntity.ok().build()

        } catch (e: Exception) {
            logger.error("Failed to update exam space", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    // === Professor Feedback Endpoints ===

    @PostMapping("/schedules/{scheduleId}/feedback")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    fun submitFeedback(
        @PathVariable scheduleId: UUID,
        @Valid @RequestBody request: SubmitFeedbackRequest
    ): ResponseEntity<CommentResponse> {
        logger.info("Submitting feedback for schedule: {}", scheduleId)

        try {
            val commentId = UUID.randomUUID().toString()

            commandGateway.sendAndWait<Void>(
                SubmitProfessorFeedbackCommand(
                    scheduleId = scheduleId,
                    commentId = commentId,
                    professorId = getCurrentUserId(),
                    scheduledExamId = request.scheduledExamId,
                    commentText = request.commentText,
                    commentType = request.commentType
                )
            )

            val comment = professorCommentRepository.findByCommentId(commentId)!!
            return ResponseEntity.status(HttpStatus.CREATED).body(comment.toResponse())

        } catch (e: Exception) {
            logger.error("Failed to submit feedback", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/schedules/{scheduleId}/feedback")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    fun getFeedback(
        @PathVariable scheduleId: UUID,
        @RequestParam(required = false) professorId: String?,
        @RequestParam(required = false) status: CommentStatus?
    ): ResponseEntity<List<CommentResponse>> {
        logger.debug("Fetching feedback for schedule: {}", scheduleId)

        val comments = when {
            professorId != null -> professorCommentRepository.findByProfessorIdAndExamSessionScheduleId(
                professorId,
                scheduleId
            )

            else -> professorCommentRepository.findByExamSessionScheduleId(scheduleId)
        }.filter { status == null || it.status == status }

        return ResponseEntity.ok(comments.map { it.toResponse() })
    }

    // === Analytics and Quality Endpoints ===

    @GetMapping("/schedules/{scheduleId}/quality")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    fun getQualityMetrics(@PathVariable scheduleId: UUID): ResponseEntity<QualityMetricsResponse> {
        logger.debug("Fetching quality metrics for schedule: {}", scheduleId)

        val metrics = qualityScoringService.getLatestMetrics(scheduleId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(metrics.toResponse())
    }

    @GetMapping("/schedules/{scheduleId}/conflicts")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    fun getConflicts(@PathVariable scheduleId: UUID): ResponseEntity<List<ConflictResponse>> {
        logger.debug("Fetching conflicts for schedule: {}", scheduleId)

        val conflicts = conflictAnalysisService.getCurrentConflicts(scheduleId)
        return ResponseEntity.ok(conflicts.map { it.toResponse() })
    }

    @PostMapping("/schedules/{scheduleId}/conflicts/{conflictId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    fun resolveConflict(
        @PathVariable scheduleId: UUID,
        @PathVariable conflictId: String,
        @RequestBody request: ResolveConflictRequest
    ): ResponseEntity<Void> {
        logger.info("Resolving conflict: {}", conflictId)

        try {
            conflictAnalysisService.resolveConflict(conflictId, getCurrentUserId(), request.resolutionNotes)
            return ResponseEntity.ok().build()

        } catch (e: Exception) {
            logger.error("Failed to resolve conflict", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    // === Version Management Endpoints ===

    @PostMapping("/schedules/{scheduleId}/versions")
    @PreAuthorize("hasRole('ADMIN')")
    fun createVersion(
        @PathVariable scheduleId: UUID,
        @RequestBody request: CreateVersionRequest
    ): ResponseEntity<VersionResponse> {
        logger.info("Creating version for schedule: {}", scheduleId)

        try {
            val version = qualityScoringService.createScheduleVersion(
                scheduleId = scheduleId,
                versionType = request.versionType,
                versionNotes = request.notes,
                createdBy = getCurrentUserId()
            )

            return ResponseEntity.status(HttpStatus.CREATED).body(version.toResponse())

        } catch (e: Exception) {
            logger.error("Failed to create version", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/schedules/{scheduleId}/versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    fun getVersions(@PathVariable scheduleId: UUID): ResponseEntity<List<VersionResponse>> {
        logger.debug("Fetching versions for schedule: {}", scheduleId)

        val versions = scheduleVersionRepository.findByScheduleIdOrderByVersionNumberDesc(scheduleId)
        return ResponseEntity.ok(versions.map { it.toResponse() })
    }

    @GetMapping("/schedules/{scheduleId}/versions/compare")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    fun compareVersions(
        @PathVariable scheduleId: UUID,
        @RequestParam version1: Int,
        @RequestParam version2: Int
    ): ResponseEntity<ScheduleComparisonResult> {
        logger.debug("Comparing versions {} and {} for schedule: {}", version1, version2, scheduleId)

        try {
            val comparison = qualityScoringService.compareScheduleVersions(scheduleId, version1, version2)
            return ResponseEntity.ok(comparison)

        } catch (e: Exception) {
            logger.error("Failed to compare versions", e)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    private fun getCurrentUserId(): String {
        return SecurityUtils.getCurrentUser()?.id ?: "system"
    }
}