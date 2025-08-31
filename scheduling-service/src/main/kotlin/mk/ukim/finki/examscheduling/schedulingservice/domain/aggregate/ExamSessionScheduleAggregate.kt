package mk.ukim.finki.examscheduling.schedulingservice.domain.aggregate

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.AdjustmentStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.CommentStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ScheduleStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ViolationSeverity
import mk.ukim.finki.examscheduling.schedulingservice.service.AdvancedSchedulingService
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@Aggregate
class ExamSessionScheduleAggregate {

    @AggregateIdentifier
    private lateinit var scheduleId: UUID
    private lateinit var examSessionPeriodId: String
    private lateinit var academicYear: String
    private lateinit var examSession: String
    private lateinit var startDate: LocalDate
    private lateinit var endDate: LocalDate
    private lateinit var status: ScheduleStatus
    private lateinit var createdAt: Instant
    private var updatedAt: Instant? = null
    private var finalizedAt: Instant? = null
    private var publishedAt: Instant? = null

    private val scheduledExams = mutableMapOf<String, ScheduledExamEntity>()
    private val professorComments = mutableMapOf<String, ProfessorCommentEntity>()
    private val adjustmentLogs = mutableMapOf<String, AdjustmentLogEntity>()

    private var version: Long = 0
    private var totalExams: Int = 0
    private var lastGenerationMetrics: SchedulingMetrics? = null

    constructor()

    private val logger = LoggerFactory.getLogger(AdvancedSchedulingService::class.java)

    // === COMMAND HANDLERS ===

    @CommandHandler
    constructor(command: CreateExamSessionScheduleCommand) {
        require(command.startDate.isBefore(command.endDate)) {
            "Start date must be before end date"
        }
        require(command.examSessionPeriodId.isNotBlank()) {
            "Exam session period ID cannot be blank"
        }

        AggregateLifecycle.apply(
            ExamSessionScheduleCreatedEvent(
                scheduleId = command.scheduleId,
                examSessionPeriodId = command.examSessionPeriodId,
                academicYear = command.academicYear,
                examSession = command.examSession,
                startDate = command.startDate,
                endDate = command.endDate,
                status = ScheduleStatus.DRAFT,
                createdBy = command.createdBy,
                createdAt = Instant.now()
            )
        )
    }

    @CommandHandler
    fun handle(command: TriggerDraftScheduleGenerationCommand) {
        validateCommandForCurrentStatus(ScheduleStatus.DRAFT, ScheduleStatus.PREFERENCES_COLLECTED)

        AggregateLifecycle.apply(
            DraftScheduleGenerationTriggeredEvent(
                scheduleId = command.scheduleId,
                examSessionPeriodId = this.examSessionPeriodId,
                triggeredBy = command.triggeredBy,
                triggeredAt = Instant.now(),
                externalDataRequired = command.externalDataRequired
            )
        )
    }

    @CommandHandler
    fun handle(command: GenerateDraftScheduleCommand) {
        validateCommandForCurrentStatus(ScheduleStatus.GENERATING)

        val schedulingResult = generateSchedule(
            courseEnrollmentData = command.courseEnrollmentData,
            courseAccreditationData = command.courseAccreditationData,
            professorPreferences = command.professorPreferences,
            availableRooms = command.availableRooms
        )

        scheduledExams.clear()

        schedulingResult.scheduledExams.forEach { examInfo ->
            AggregateLifecycle.apply(
                ScheduledExamAddedEvent(
                    scheduleId = scheduleId,
                    scheduledExamId = examInfo.scheduledExamId,
                    courseId = examInfo.courseId,
                    courseName = examInfo.courseName,
                    examDate = examInfo.examDate,
                    startTime = examInfo.startTime,
                    endTime = examInfo.endTime,
                    roomId = examInfo.roomId,
                    roomName = examInfo.roomName,
                    roomCapacity = examInfo.roomCapacity,
                    studentCount = examInfo.studentCount,
                    mandatoryStatus = examInfo.mandatoryStatus,
                    professorIds = examInfo.professorIds,
                    addedBy = command.generatedBy,
                    addedAt = Instant.now()
                )
            )
        }

        AggregateLifecycle.apply(
            DraftScheduleGeneratedEvent(
                scheduleId = scheduleId,
                examSessionPeriodId = this.examSessionPeriodId,
                totalExams = schedulingResult.scheduledExams.size,
                schedulingMetrics = schedulingResult.metrics,
                generatedBy = command.generatedBy,
                generatedAt = Instant.now(),
                qualityScore = schedulingResult.qualityScore,
                constraintViolations = schedulingResult.violations
            )
        )
    }

    @CommandHandler
    fun handle(command: AddScheduledExamCommand) {
        validateCommandForCurrentStatus(ScheduleStatus.DRAFT, ScheduleStatus.GENERATED, ScheduleStatus.UNDER_REVIEW)

        require(!scheduledExams.containsKey(command.scheduledExamId)) {
            "Scheduled exam with ID ${command.scheduledExamId} already exists"
        }

        validateExamTimeSlot(command.examDate, command.startTime, command.endTime, command.scheduledExamId)

        AggregateLifecycle.apply(
            ScheduledExamAddedEvent(
                scheduleId = scheduleId,
                scheduledExamId = command.scheduledExamId,
                courseId = command.courseId,
                courseName = command.courseName,
                examDate = command.examDate,
                startTime = command.startTime,
                endTime = command.endTime,
                roomId = command.roomId,
                roomName = command.roomName,
                roomCapacity = command.roomCapacity,
                studentCount = command.studentCount,
                mandatoryStatus = command.mandatoryStatus,
                professorIds = command.professorIds,
                addedBy = command.addedBy,
                addedAt = Instant.now()
            )
        )
    }

    @CommandHandler
    fun handle(command: UpdateScheduledExamTimeCommand) {
        validateCommandForCurrentStatus(ScheduleStatus.GENERATED, ScheduleStatus.UNDER_REVIEW)

        val existingExam = scheduledExams[command.scheduledExamId]
            ?: throw IllegalArgumentException("Scheduled exam with ID ${command.scheduledExamId} not found")

        validateExamTimeSlot(command.newExamDate, command.newStartTime, command.newEndTime, command.scheduledExamId)

        val impactAnalysis = analyzeTimeChangeImpact(command.scheduledExamId, command.newExamDate, command.newStartTime, command.newEndTime)

        AggregateLifecycle.apply(
            ScheduledExamTimeChangedEvent(
                scheduleId = scheduleId,
                scheduledExamId = command.scheduledExamId,
                courseId = existingExam.courseId,
                oldExamDate = existingExam.examDate,
                oldStartTime = existingExam.startTime,
                oldEndTime = existingExam.endTime,
                newExamDate = command.newExamDate,
                newStartTime = command.newStartTime,
                newEndTime = command.newEndTime,
                reason = command.reason,
                updatedBy = command.updatedBy,
                updatedAt = Instant.now(),
                impactedStudents = impactAnalysis.impactedStudents,
                conflictsResolved = impactAnalysis.conflictsResolved,
                conflictsCreated = impactAnalysis.conflictsCreated
            )
        )
    }

    @CommandHandler
    fun handle(command: UpdateScheduledExamSpaceCommand) {
        validateCommandForCurrentStatus(ScheduleStatus.GENERATED, ScheduleStatus.UNDER_REVIEW)

        val existingExam = scheduledExams[command.scheduledExamId]
            ?: throw IllegalArgumentException("Scheduled exam with ID ${command.scheduledExamId} not found")

        if (command.newRoomCapacity != null && command.newRoomCapacity < existingExam.studentCount) {
            throw IllegalArgumentException("New room capacity (${command.newRoomCapacity}) is less than student count (${existingExam.studentCount})")
        }

        AggregateLifecycle.apply(
            ScheduledExamSpaceChangedEvent(
                scheduleId = scheduleId,
                scheduledExamId = command.scheduledExamId,
                courseId = existingExam.courseId,
                oldRoomId = existingExam.roomId,
                oldRoomName = existingExam.roomName,
                oldRoomCapacity = existingExam.roomCapacity,
                newRoomId = command.newRoomId,
                newRoomName = command.newRoomName,
                newRoomCapacity = command.newRoomCapacity,
                reason = command.reason,
                updatedBy = command.updatedBy,
                updatedAt = Instant.now(),
                capacityImpact = (command.newRoomCapacity ?: 0) - (existingExam.roomCapacity ?: 0)
            )
        )
    }

    @CommandHandler
    fun handle(command: SubmitProfessorFeedbackCommand) {
        validateCommandForCurrentStatus(ScheduleStatus.PUBLISHED_FOR_REVIEW, ScheduleStatus.UNDER_REVIEW)

        require(!professorComments.containsKey(command.commentId)) {
            "Comment with ID ${command.commentId} already exists"
        }

        if (command.scheduledExamId != null) {
            require(scheduledExams.containsKey(command.scheduledExamId)) {
                "Scheduled exam with ID ${command.scheduledExamId} not found"
            }
        }

        AggregateLifecycle.apply(
            ProfessorFeedbackSubmittedEvent(
                scheduleId = scheduleId,
                commentId = command.commentId,
                professorId = command.professorId,
                scheduledExamId = command.scheduledExamId,
                commentText = command.commentText,
                commentType = command.commentType,
                status = CommentStatus.SUBMITTED,
                submittedAt = Instant.now()
            )
        )
    }

    @CommandHandler
    fun handle(command: RequestScheduleAdjustmentCommand) {
        validateCommandForCurrentStatus(ScheduleStatus.UNDER_REVIEW)

        require(!adjustmentLogs.containsKey(command.adjustmentId)) {
            "Adjustment with ID ${command.adjustmentId} already exists"
        }

        if (command.commentId != null) {
            require(professorComments.containsKey(command.commentId)) {
                "Comment with ID ${command.commentId} not found"
            }
        }

        if (command.scheduledExamId != null) {
            require(scheduledExams.containsKey(command.scheduledExamId)) {
                "Scheduled exam with ID ${command.scheduledExamId} not found"
            }
        }

        AggregateLifecycle.apply(
            ScheduleAdjustmentRequestedEvent(
                scheduleId = scheduleId,
                adjustmentId = command.adjustmentId,
                commentId = command.commentId,
                scheduledExamId = command.scheduledExamId,
                adjustmentType = command.adjustmentType,
                description = command.description,
                requestedBy = command.requestedBy,
                requestedAt = Instant.now(),
                reason = command.reason,
                status = AdjustmentStatus.REQUESTED
            )
        )
    }

    @CommandHandler
    fun handle(command: ApproveScheduleAdjustmentCommand) {
        val adjustment = adjustmentLogs[command.adjustmentId]
            ?: throw IllegalArgumentException("Adjustment with ID ${command.adjustmentId} not found")

        require(adjustment.status == AdjustmentStatus.REQUESTED || adjustment.status == AdjustmentStatus.UNDER_REVIEW) {
            "Adjustment must be in REQUESTED or UNDER_REVIEW status to be approved"
        }

        AggregateLifecycle.apply(
            ScheduleAdjustmentApprovedEvent(
                scheduleId = scheduleId,
                adjustmentId = command.adjustmentId,
                commentId = adjustment.commentId,
                scheduledExamId = adjustment.scheduledExamId,
                adjustmentType = adjustment.adjustmentType,
                description = adjustment.description,
                approvedBy = command.approvedBy,
                approvedAt = Instant.now(),
                approvalNotes = command.approvalNotes,
                oldValues = adjustment.oldValues,
                newValues = adjustment.newValues
            )
        )
    }

    @CommandHandler
    fun handle(command: PublishDraftScheduleForReviewCommand) {
        validateCommandForCurrentStatus(ScheduleStatus.GENERATED)

        require(scheduledExams.isNotEmpty()) {
            "Cannot publish empty schedule for review"
        }

        AggregateLifecycle.apply(
            DraftSchedulePublishedForReviewEvent(
                scheduleId = scheduleId,
                examSessionPeriodId = this.examSessionPeriodId,
                publishedBy = command.publishedBy,
                publishedAt = Instant.now(),
                publishNotes = command.publishNotes,
                reviewDeadline = Instant.now().plusSeconds(7 * 24 * 3600)
            )
        )
    }

    @CommandHandler
    fun handle(command: FinalizeScheduleCommand) {
        validateCommandForCurrentStatus(ScheduleStatus.UNDER_REVIEW)

        AggregateLifecycle.apply(
            ScheduleFinalizedEvent(
                scheduleId = scheduleId,
                examSessionPeriodId = this.examSessionPeriodId,
                finalizedBy = command.finalizedBy,
                finalizedAt = Instant.now(),
                finalizeNotes = command.finalizeNotes,
                totalExams = scheduledExams.size,
                finalQualityScore = calculateCurrentQualityScore()
            )
        )
    }

    @CommandHandler
    fun handle(command: PublishFinalScheduleCommand) {
        validateCommandForCurrentStatus(ScheduleStatus.FINALIZED)

        AggregateLifecycle.apply(
            FinalSchedulePublishedEvent(
                scheduleId = scheduleId,
                examSessionPeriodId = this.examSessionPeriodId,
                publishedBy = command.publishedBy,
                publishedAt = Instant.now(),
                publishNotes = command.publishNotes,
                distributionChannels = listOf("web", "email", "pdf", "api")
            )
        )
    }

    // === EVENT SOURCING HANDLERS ===

    @EventSourcingHandler
    fun on(event: ExamSessionScheduleCreatedEvent) {
        this.scheduleId = event.scheduleId
        this.examSessionPeriodId = event.examSessionPeriodId
        this.academicYear = event.academicYear
        this.examSession = event.examSession
        this.startDate = event.startDate
        this.endDate = event.endDate
        this.status = event.status
        this.createdAt = event.createdAt
        this.version++
    }

    @EventSourcingHandler
    fun on(event: DraftScheduleGenerationTriggeredEvent) {
        this.status = ScheduleStatus.GENERATING
        this.updatedAt = event.triggeredAt
        this.version++
    }

    @EventSourcingHandler
    fun on(event: DraftScheduleGeneratedEvent) {
        this.status = ScheduleStatus.GENERATED
        this.totalExams = event.totalExams
        this.lastGenerationMetrics = event.schedulingMetrics
        this.updatedAt = event.generatedAt
        this.version++
    }

    @EventSourcingHandler
    fun on(event: ScheduledExamAddedEvent) {
        scheduledExams[event.scheduledExamId] = ScheduledExamEntity(
            scheduledExamId = event.scheduledExamId,
            courseId = event.courseId,
            courseName = event.courseName,
            examDate = event.examDate,
            startTime = event.startTime,
            endTime = event.endTime,
            roomId = event.roomId,
            roomName = event.roomName,
            roomCapacity = event.roomCapacity,
            studentCount = event.studentCount,
            mandatoryStatus = event.mandatoryStatus,
            professorIds = event.professorIds.toMutableSet(),
            createdAt = event.addedAt
        )
        this.updatedAt = event.addedAt
        this.version++
    }

    @EventSourcingHandler
    fun on(event: ScheduledExamTimeChangedEvent) {
        scheduledExams[event.scheduledExamId]?.let { exam ->
            scheduledExams[event.scheduledExamId] = exam.copy(
                examDate = event.newExamDate,
                startTime = event.newStartTime,
                endTime = event.newEndTime,
                updatedAt = event.updatedAt
            )
        }
        this.updatedAt = event.updatedAt
        this.version++
    }

    @EventSourcingHandler
    fun on(event: ScheduledExamSpaceChangedEvent) {
        scheduledExams[event.scheduledExamId]?.let { exam ->
            scheduledExams[event.scheduledExamId] = exam.copy(
                roomId = event.newRoomId,
                roomName = event.newRoomName,
                roomCapacity = event.newRoomCapacity,
                updatedAt = event.updatedAt
            )
        }
        this.updatedAt = event.updatedAt
        this.version++
    }

    @EventSourcingHandler
    fun on(event: ProfessorFeedbackSubmittedEvent) {
        professorComments[event.commentId] = ProfessorCommentEntity(
            commentId = event.commentId,
            professorId = event.professorId,
            scheduledExamId = event.scheduledExamId,
            commentText = event.commentText,
            commentType = event.commentType,
            status = event.status,
            submittedAt = event.submittedAt
        )
        this.updatedAt = event.submittedAt
        this.version++
    }

    @EventSourcingHandler
    fun on(event: ScheduleAdjustmentRequestedEvent) {
        adjustmentLogs[event.adjustmentId] = AdjustmentLogEntity(
            adjustmentId = event.adjustmentId,
            commentId = event.commentId,
            scheduledExamId = event.scheduledExamId,
            adjustmentType = event.adjustmentType,
            description = event.description,
            requestedBy = event.requestedBy,
            requestedAt = event.requestedAt,
            reason = event.reason,
            status = event.status
        )
        this.updatedAt = event.requestedAt
        this.version++
    }

    @EventSourcingHandler
    fun on(event: DraftSchedulePublishedForReviewEvent) {
        this.status = ScheduleStatus.PUBLISHED_FOR_REVIEW
        this.updatedAt = event.publishedAt
        this.version++
    }

    @EventSourcingHandler
    fun on(event: ScheduleFinalizedEvent) {
        this.status = ScheduleStatus.FINALIZED
        this.finalizedAt = event.finalizedAt
        this.updatedAt = event.finalizedAt
        this.version++
    }

    @EventSourcingHandler
    fun on(event: FinalSchedulePublishedEvent) {
        this.status = ScheduleStatus.PUBLISHED
        this.publishedAt = event.publishedAt
        this.updatedAt = event.publishedAt
        this.version++
    }

    // === BUSINESS LOGIC HELPERS ===

    private fun validateCommandForCurrentStatus(vararg allowedStatuses: ScheduleStatus) {
        require(this.status in allowedStatuses) {
            "Command not allowed in current status: ${this.status}. Allowed statuses: ${allowedStatuses.joinToString()}"
        }
    }

    private fun validateExamTimeSlot(examDate: LocalDate, startTime: LocalTime, endTime: LocalTime, excludeExamId: String? = null) {
        require(examDate.isAfter(startDate.minusDays(1)) && examDate.isBefore(endDate.plusDays(1))) {
            "Exam date must be within the session period ($startDate to $endDate)"
        }

        require(startTime.isBefore(endTime)) {
            "Start time must be before end time"
        }

        val conflicts = scheduledExams.values
            .filter { it.scheduledExamId != excludeExamId }
            .filter { it.examDate == examDate }
            .filter { timeSlotsOverlap(startTime, endTime, it.startTime, it.endTime) }

        if (conflicts.isNotEmpty()) {
            throw IllegalArgumentException("Time slot conflicts with existing exams: ${conflicts.map { it.scheduledExamId }}")
        }
    }

    private fun timeSlotsOverlap(start1: LocalTime, end1: LocalTime, start2: LocalTime, end2: LocalTime): Boolean {
        return start1.isBefore(end2) && start2.isBefore(end1)
    }

    private fun analyzeTimeChangeImpact(examId: String, newDate: LocalDate, newStart: LocalTime, newEnd: LocalTime): TimeChangeImpact {
        return TimeChangeImpact(
            impactedStudents = scheduledExams[examId]?.studentCount ?: 0,
            conflictsResolved = emptyList(),
            conflictsCreated = emptyList()
        )
    }

    private fun calculateCurrentQualityScore(): Double {
        return lastGenerationMetrics?.preferenceSatisfactionRate ?: 0.0
    }

    @Autowired
    private lateinit var advancedSchedulingService: AdvancedSchedulingService

    private fun generateSchedule(
        courseEnrollmentData: Map<String, CourseEnrollmentInfo>,
        courseAccreditationData: Map<String, CourseAccreditationInfo>,
        professorPreferences: List<ProfessorPreferenceInfo>,
        availableRooms: List<RoomInfo>
    ): SchedulingResult {
        logger.info("Generating advanced schedule for {} courses using CSP algorithms", courseEnrollmentData.size)

        try {
            val examPeriod = ExamPeriod(
                startDate = this.startDate,
                endDate = this.endDate,
                name = this.examSessionPeriodId
            )

            val institutionalConstraints = InstitutionalConstraints(
                workingHours = WorkingHours(
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(18, 0)
                ),
                minimumExamDuration = 120, // 2 hours
                minimumGapMinutes = 30,
                maxExamsPerDay = 6,
                maxExamsPerRoom = 8,
                allowWeekendExams = false
            )

            val schedulingResult = advancedSchedulingService.generateOptimalSchedule(
                courseEnrollmentData = courseEnrollmentData,
                courseAccreditationData = courseAccreditationData,
                professorPreferences = professorPreferences,
                availableRooms = availableRooms,
                examPeriod = examPeriod,
                institutionalConstraints = institutionalConstraints
            )

            logger.info("Advanced scheduling completed with {} exams, quality score: {}, violations: {}",
                schedulingResult.scheduledExams.size,
                schedulingResult.qualityScore,
                schedulingResult.violations.size)

            return schedulingResult

        } catch (e: Exception) {
            logger.error("Advanced scheduling failed, falling back to basic scheduling", e)

            return generateBasicFallbackSchedule(
                courseEnrollmentData, courseAccreditationData, availableRooms, e
            )
        }
    }

    private fun generateBasicFallbackSchedule(
        courseEnrollmentData: Map<String, CourseEnrollmentInfo>,
        courseAccreditationData: Map<String, CourseAccreditationInfo>,
        availableRooms: List<RoomInfo>,
        originalException: Exception
    ): SchedulingResult {
        logger.warn("Generating basic fallback schedule due to advanced scheduling failure: {}", originalException.message)

        val fallbackExams = mutableListOf<ScheduledExamInfo>()
        var currentDate = this.startDate
        var currentTime = LocalTime.of(9, 0)
        var roomIndex = 0

        courseEnrollmentData.forEach { (courseId, enrollment) ->
            val accreditation = courseAccreditationData[courseId]

            if (accreditation != null) {
                val room = availableRooms.find { it.capacity >= enrollment.studentCount }
                    ?: availableRooms.getOrNull(roomIndex % availableRooms.size)
                    ?: availableRooms.firstOrNull()

                if (room != null) {
                    val examDuration = when (accreditation.credits) {
                        in 1..3 -> 90
                        in 4..6 -> 120
                        in 7..9 -> 180
                        else -> 120
                    }

                    fallbackExams.add(
                        ScheduledExamInfo(
                            scheduledExamId = "${courseId}_fallback",
                            courseId = courseId,
                            courseName = accreditation.courseName,
                            examDate = currentDate,
                            startTime = currentTime,
                            endTime = currentTime.plusMinutes(examDuration.toLong()),
                            roomId = room.roomId,
                            roomName = room.roomName,
                            roomCapacity = room.capacity,
                            studentCount = enrollment.studentCount,
                            mandatoryStatus = accreditation.mandatoryStatus,
                            professorIds = accreditation.professorIds
                        )
                    )

                    currentTime = currentTime.plusMinutes(examDuration + 30L)
                    if (currentTime.isAfter(LocalTime.of(17, 0))) {
                        currentDate = currentDate.plusDays(1)
                        currentTime = LocalTime.of(9, 0)
                        roomIndex = (roomIndex + 1) % availableRooms.size
                    }
                }
            }
        }

        val fallbackMetrics = SchedulingMetrics(
            totalCoursesScheduled = fallbackExams.size,
            totalProfessorPreferencesConsidered = 0,
            preferencesSatisfied = 0,
            preferenceSatisfactionRate = 0.3,
            totalConflicts = 0,
            resolvedConflicts = 0,
            roomUtilizationRate = calculateBasicRoomUtilization(fallbackExams),
            averageStudentExamsPerDay = calculateBasicAverageExamsPerDay(fallbackExams),
            processingTimeMs = 100L
        )

        return SchedulingResult(
            scheduledExams = fallbackExams,
            metrics = fallbackMetrics,
            qualityScore = 0.4,
            violations = listOf(
                ConstraintViolation(
                    violationType = "ADVANCED_SCHEDULING_FAILED",
                    severity = ViolationSeverity.MEDIUM,
                    description = "Advanced scheduling failed, using basic fallback: ${originalException.message}",
                    affectedExams = fallbackExams.map { it.courseId },
                    affectedStudents = fallbackExams.sumOf { it.studentCount },
                    suggestedResolution = "Review scheduling parameters and retry with advanced algorithms"
                )
            )
        )
    }

    private fun calculateBasicRoomUtilization(exams: List<ScheduledExamInfo>): Double {
        val utilizations = exams.mapNotNull { exam ->
            exam.roomCapacity?.let { capacity ->
                if (capacity > 0) exam.studentCount.toDouble() / capacity else null
            }
        }
        return if (utilizations.isNotEmpty()) utilizations.average() else 0.0
    }

    private fun calculateBasicAverageExamsPerDay(exams: List<ScheduledExamInfo>): Double {
        val examsByDate = exams.groupBy { it.examDate }
        val dailyCounts = examsByDate.values.map { it.size }
        return if (dailyCounts.isNotEmpty()) dailyCounts.average() else 0.0
    }

    @CommandHandler
    fun handle(command: OptimizeScheduleCommand) {
        validateCommandForCurrentStatus(ScheduleStatus.GENERATED, ScheduleStatus.UNDER_REVIEW)

        try {
            val currentExams = scheduledExams.values.toList()
            val currentAssignments = currentExams.associate { exam ->
                exam.courseId to TimeSlot(
                    date = exam.examDate,
                    startTime = exam.startTime,
                    endTime = exam.endTime,
                    roomId = exam.roomId ?: "",
                    roomName = exam.roomName ?: "",
                    roomCapacity = exam.roomCapacity ?: 0,
                    dayOfWeek = exam.examDate.dayOfWeek.value
                )
            }

            val examPeriod = ExamPeriod(startDate, endDate, examSessionPeriodId)
            val institutionalConstraints = InstitutionalConstraints(
                workingHours = WorkingHours(LocalTime.of(8, 0), LocalTime.of(18, 0)),
                minimumExamDuration = 120,
                minimumGapMinutes = 30,
                maxExamsPerDay = 6,
                maxExamsPerRoom = 8
            )

            val courses = currentExams.map { exam ->
                CourseSchedulingInfo(
                    courseId = exam.courseId,
                    courseName = exam.courseName,
                    studentCount = exam.studentCount,
                    professorIds = exam.professorIds,
                    mandatoryStatus = exam.mandatoryStatus,
                    estimatedDuration = java.time.Duration.between(exam.startTime, exam.endTime).toMinutes().toInt()
                )
            }

            val problem = SchedulingProblem(
                examPeriod = examPeriod,
                courses = courses,
                availableRooms = command.availableRooms,
                professorPreferences = command.professorPreferences,
                institutionalConstraints = institutionalConstraints,
                constraints = emptyList(),
                solvingStrategy = command.optimizationStrategy ?: SolvingStrategy.HYBRID_APPROACH
            )

            // Apply optimization
            val optimizedResult = advancedSchedulingService.generateOptimalSchedule(
                courseEnrollmentData = courses.associate { it.courseId to CourseEnrollmentInfo(it.courseId, it.studentCount) },
                courseAccreditationData = courses.associate { course ->
                    course.courseId to CourseAccreditationInfo(
                        courseId = course.courseId,
                        courseName = course.courseName,
                        mandatoryStatus = course.mandatoryStatus,
                        credits = 6, // Default
                        professorIds = course.professorIds
                    )
                },
                professorPreferences = command.professorPreferences,
                availableRooms = command.availableRooms,
                examPeriod = examPeriod,
                institutionalConstraints = institutionalConstraints
            )

            scheduledExams.clear()

            optimizedResult.scheduledExams.forEach { examInfo ->
                AggregateLifecycle.apply(
                    ScheduledExamAddedEvent(
                        scheduleId = scheduleId,
                        scheduledExamId = examInfo.scheduledExamId,
                        courseId = examInfo.courseId,
                        courseName = examInfo.courseName,
                        examDate = examInfo.examDate,
                        startTime = examInfo.startTime,
                        endTime = examInfo.endTime,
                        roomId = examInfo.roomId,
                        roomName = examInfo.roomName,
                        roomCapacity = examInfo.roomCapacity,
                        studentCount = examInfo.studentCount,
                        mandatoryStatus = examInfo.mandatoryStatus,
                        professorIds = examInfo.professorIds,
                        addedBy = command.optimizedBy,
                        addedAt = Instant.now()
                    )
                )
            }

            AggregateLifecycle.apply(
                ScheduleOptimizationCompletedEvent(
                    scheduleId = scheduleId,
                    optimizationStrategy = command.optimizationStrategy?.name ?: "HYBRID_APPROACH",
                    previousQualityScore = command.previousQualityScore ?: 0.0,
                    newQualityScore = optimizedResult.qualityScore,
                    improvementRate = optimizedResult.qualityScore - (command.previousQualityScore ?: 0.0),
                    optimizationTime = optimizedResult.metrics.processingTimeMs,
                    optimizedBy = command.optimizedBy,
                    optimizedAt = Instant.now(),
                    constraintViolationsResolved = optimizedResult.violations.size,
                    optimizationMetrics = mapOf(
                        "preferenceSatisfactionRate" to optimizedResult.metrics.preferenceSatisfactionRate,
                        "roomUtilizationRate" to optimizedResult.metrics.roomUtilizationRate,
                        "totalConflicts" to optimizedResult.metrics.totalConflicts
                    )
                )
            )

        } catch (e: Exception) {
            logger.error("Schedule optimization failed", e)

            AggregateLifecycle.apply(
                ScheduleOptimizationFailedEvent(
                    scheduleId = scheduleId,
                    failureReason = e.message ?: "Unknown optimization failure",
                    failedAt = Instant.now(),
                    attemptedBy = command.optimizedBy
                )
            )

            throw IllegalStateException("Schedule optimization failed: ${e.message}", e)
        }
    }

    @EventSourcingHandler
    fun on(event: ScheduleOptimizationCompletedEvent) {
        this.status = ScheduleStatus.GENERATED
        this.updatedAt = event.optimizedAt
        this.lastGenerationMetrics = this.lastGenerationMetrics?.copy(
            preferenceSatisfactionRate = event.optimizationMetrics["preferenceSatisfactionRate"] as? Double ?: 0.0,
            roomUtilizationRate = event.optimizationMetrics["roomUtilizationRate"] as? Double ?: 0.0,
            totalConflicts = (event.optimizationMetrics["totalConflicts"] as? Number)?.toInt() ?: 0
        )
        this.version++

        logger.info("Schedule optimization completed: improvement rate = {}", event.improvementRate)
    }

    @EventSourcingHandler
    fun on(event: ScheduleOptimizationFailedEvent) {
        this.updatedAt = event.failedAt
        this.version++

        logger.warn("Schedule optimization failed: {}", event.failureReason)
    }
}