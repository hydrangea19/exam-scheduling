package mk.ukim.finki.examscheduling.schedulingservice.controller

import jakarta.validation.Valid
import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.CommentStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ScheduleStatus
import mk.ukim.finki.examscheduling.schedulingservice.repository.*
import mk.ukim.finki.examscheduling.schedulingservice.service.ConflictAnalysisService
import mk.ukim.finki.examscheduling.schedulingservice.service.QualityScoringService
import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.queryhandling.QueryGateway
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/scheduling")
class SchedulingController(
    private val commandGateway: CommandGateway,
    private val queryGateway: QueryGateway,
    private val examSessionScheduleRepository: ExamSessionScheduleRepository,
    private val scheduledExamRepository: ScheduledExamRepository,
    private val professorCommentRepository: ProfessorCommentRepository,
    private val adjustmentLogRepository: AdjustmentLogRepository,
    private val qualityScoringService: QualityScoringService,
    private val conflictAnalysisService: ConflictAnalysisService,
    private val scheduleVersionRepository: ScheduleVersionRepository
) {

    private val logger = LoggerFactory.getLogger(SchedulingController::class.java)

    // === Schedule Management Endpoints ===

    @PostMapping("/schedules")
    @PreAuthorize("hasRole('ADMIN')")
    fun createSchedule(@Valid @RequestBody request: CreateScheduleRequest): ResponseEntity<ScheduleResponse> {
        logger.info("Creating new schedule for session: {}", request.examSessionPeriodId)

        val scheduleId = UUID.randomUUID()

        try {
            commandGateway.sendAndWait<Void>(
                CreateExamSessionScheduleCommand(
                    scheduleId = scheduleId,
                    examSessionPeriodId = request.examSessionPeriodId,
                    academicYear = request.academicYear,
                    examSession = request.examSession,
                    startDate = request.startDate,
                    endDate = request.endDate,
                    createdBy = getCurrentUserId()
                )
            )

            val schedule = examSessionScheduleRepository.findById(scheduleId).get()
            return ResponseEntity.status(HttpStatus.CREATED).body(schedule.toResponse())

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
        logger.debug("Fetching schedules with filters: year={}, session={}, status={}", academicYear, examSession, status)

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

            // Initiate the complex generation process
            commandGateway.sendAndWait<Void>(
                InitiateScheduleGenerationCommand(
                    scheduleId = scheduleId,
                    examSessionPeriodId = schedule.examSessionPeriodId,
                    academicYear = schedule.academicYear,
                    examSession = schedule.examSession,
                    initiatedBy = getCurrentUserId()
                )
            )

            return ResponseEntity.ok(
                GenerationResponse(
                    scheduleId = scheduleId,
                    status = "INITIATED",
                    message = "Schedule generation has been initiated. This may take several minutes.",
                    estimatedCompletionTime = Instant.now().plusSeconds(300) // 5 minutes
                )
            )

        } catch (e: Exception) {
            logger.error("Failed to initiate schedule generation", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GenerationResponse(
                    scheduleId = scheduleId,
                    status = "FAILED",
                    message = "Failed to initiate generation: ${e.message}",
                    estimatedCompletionTime = null
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
            commandGateway.sendAndWait<Void>(
                PublishDraftScheduleForReviewCommand(
                    scheduleId = scheduleId,
                    publishedBy = getCurrentUserId(),
                    publishNotes = request.notes
                )
            )

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
        logger.info("Finalizing schedule: {}", scheduleId)

        try {
            commandGateway.sendAndWait<Void>(
                FinalizeScheduleCommand(
                    scheduleId = scheduleId,
                    finalizedBy = getCurrentUserId(),
                    finalizeNotes = request.notes
                )
            )

            return ResponseEntity.ok().build()

        } catch (e: Exception) {
            logger.error("Failed to finalize schedule", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
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
            commandGateway.sendAndWait<Void>(
                AddScheduledExamCommand(
                    scheduleId = scheduleId,
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
                    professorIds = request.professorIds,
                    addedBy = getCurrentUserId()
                )
            )

            val exam = scheduledExamRepository.findByScheduledExamId(
                request.scheduledExamId ?: "${request.courseId}_${scheduleId}"
            )!!

            return ResponseEntity.status(HttpStatus.CREATED).body(exam.toResponse())

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
            professorId != null -> professorCommentRepository.findByProfessorIdAndExamSessionScheduleId(professorId, scheduleId)
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