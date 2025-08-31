package mk.ukim.finki.examscheduling.schedulingservice.handlers

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.AdjustmentStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.AdjustmentType
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.CommentType
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ScheduleStatus
import mk.ukim.finki.examscheduling.schedulingservice.repository.AdjustmentLogRepository
import mk.ukim.finki.examscheduling.schedulingservice.repository.ExamSessionScheduleRepository
import mk.ukim.finki.examscheduling.schedulingservice.repository.ProfessorCommentRepository
import mk.ukim.finki.examscheduling.schedulingservice.repository.ScheduledExamRepository
import mk.ukim.finki.examscheduling.schedulingservice.service.EventPublisher
import mk.ukim.finki.examscheduling.schedulingservice.service.QualityScoringService
import org.axonframework.eventhandling.EventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Component
@Transactional
class SchedulingProjectionHandler(
    private val examSessionScheduleRepository: ExamSessionScheduleRepository,
    private val scheduledExamRepository: ScheduledExamRepository,
    private val professorCommentRepository: ProfessorCommentRepository,
    private val adjustmentLogRepository: AdjustmentLogRepository,
    private val eventPublisher: EventPublisher,
    private val qualityScoringService: QualityScoringService
) {

    private val logger = LoggerFactory.getLogger(SchedulingProjectionHandler::class.java)

    // === ExamSessionSchedule Events ===

    @EventHandler
    fun on(event: ExamSessionScheduleCreatedEvent) {
        logger.info("Creating JPA projection for schedule: {}", event.scheduleId)

        val schedule = ExamSessionSchedule(
            id = event.scheduleId,
            examSessionPeriodId = event.examSessionPeriodId,
            academicYear = event.academicYear,
            examSession = event.examSession,
            startDate = event.startDate,
            endDate = event.endDate,
            status = event.status,
            createdAt = event.createdAt
        )

        examSessionScheduleRepository.save(schedule)

        val notification = mapOf(
            "eventType" to "ScheduleCreated",
            "scheduleId" to event.scheduleId.toString(),
            "examSessionPeriodId" to event.examSessionPeriodId,
            "message" to "New exam session schedule created for ${event.academicYear} ${event.examSession}",
            "timestamp" to event.createdAt.toString()
        )
        eventPublisher.publishSystemNotification(notification, "schedule-created-${event.scheduleId}")
    }

    @EventHandler
    fun on(event: DraftScheduleGenerationTriggeredEvent) {
        logger.info("Updating schedule status to GENERATING: {}", event.scheduleId)

        examSessionScheduleRepository.findById(event.scheduleId).ifPresent { schedule ->
            val updatedSchedule = schedule.copy(
                status = ScheduleStatus.GENERATING,
                updatedAt = event.triggeredAt
            )
            examSessionScheduleRepository.save(updatedSchedule)
        }
    }

    @EventHandler
    fun on(event: DraftScheduleGeneratedEvent) {
        logger.info("Updating schedule status to GENERATED with {} exams: {}", event.totalExams, event.scheduleId)

        examSessionScheduleRepository.findById(event.scheduleId).ifPresent { schedule ->
            val updatedSchedule = schedule.copy(
                status = ScheduleStatus.GENERATED,
                updatedAt = event.generatedAt
            )
            examSessionScheduleRepository.save(updatedSchedule)

            qualityScoringService.recordSchedulingMetrics(event.scheduleId, event.schedulingMetrics, event.qualityScore)

            val notification = mapOf(
                "eventType" to "ScheduleGenerated",
                "scheduleId" to event.scheduleId.toString(),
                "examSessionPeriodId" to event.examSessionPeriodId,
                "totalExams" to event.totalExams,
                "qualityScore" to event.qualityScore,
                "metrics" to mapOf(
                    "preferenceSatisfactionRate" to event.schedulingMetrics.preferenceSatisfactionRate,
                    "totalConflicts" to event.schedulingMetrics.totalConflicts,
                    "roomUtilizationRate" to event.schedulingMetrics.roomUtilizationRate
                ),
                "violations" to event.constraintViolations.map { violation ->
                    mapOf(
                        "type" to violation.violationType,
                        "severity" to violation.severity.name,
                        "description" to violation.description,
                        "affectedExams" to violation.affectedExams.size
                    )
                },
                "timestamp" to event.generatedAt.toString()
            )
            eventPublisher.publishSystemNotification(notification, "schedule-generated-${event.scheduleId}")
        }
    }

    @EventHandler
    fun on(event: DraftSchedulePublishedForReviewEvent) {
        logger.info("Publishing schedule for review: {}", event.scheduleId)

        examSessionScheduleRepository.findById(event.scheduleId).ifPresent { schedule ->
            val updatedSchedule = schedule.copy(
                status = ScheduleStatus.PUBLISHED_FOR_REVIEW,
                updatedAt = event.publishedAt
            )
            examSessionScheduleRepository.save(updatedSchedule)

            val notification = mapOf(
                "eventType" to "SchedulePublishedForReview",
                "scheduleId" to event.scheduleId.toString(),
                "examSessionPeriodId" to event.examSessionPeriodId,
                "message" to "Schedule is now available for professor review",
                "reviewDeadline" to event.reviewDeadline?.toString(),
                "publishNotes" to event.publishNotes,
                "timestamp" to event.publishedAt.toString()
            )
            eventPublisher.publishSystemNotification(notification, "review-published-${event.scheduleId}")
        }
    }

    @EventHandler
    fun on(event: ScheduleFinalizedEvent) {
        logger.info("Finalizing schedule: {}", event.scheduleId)

        examSessionScheduleRepository.findById(event.scheduleId).ifPresent { schedule ->
            val updatedSchedule = schedule.copy(
                status = ScheduleStatus.FINALIZED,
                finalizedAt = event.finalizedAt,
                updatedAt = event.finalizedAt
            )
            examSessionScheduleRepository.save(updatedSchedule)

            qualityScoringService.recordFinalQualityScore(event.scheduleId, event.finalQualityScore)
        }
    }

    @EventHandler
    fun on(event: FinalSchedulePublishedEvent) {
        logger.info("Publishing final schedule: {}", event.scheduleId)

        examSessionScheduleRepository.findById(event.scheduleId).ifPresent { schedule ->
            val updatedSchedule = schedule.copy(
                status = ScheduleStatus.PUBLISHED,
                publishedAt = event.publishedAt,
                updatedAt = event.publishedAt
            )
            examSessionScheduleRepository.save(updatedSchedule)

            val notification = mapOf(
                "eventType" to "FinalSchedulePublished",
                "scheduleId" to event.scheduleId.toString(),
                "examSessionPeriodId" to event.examSessionPeriodId,
                "message" to "Final exam schedule has been published",
                "distributionChannels" to event.distributionChannels,
                "publishNotes" to event.publishNotes,
                "timestamp" to event.publishedAt.toString()
            )
            eventPublisher.publishSystemNotification(notification, "final-published-${event.scheduleId}")
        }
    }

    // === ScheduledExam Events ===

    @EventHandler
    fun on(event: ScheduledExamAddedEvent) {
        logger.info("Adding scheduled exam: {} for course: {}", event.scheduledExamId, event.courseId)

        examSessionScheduleRepository.findById(event.scheduleId).ifPresent { schedule ->
            val scheduledExam = ScheduledExam(
                id = java.util.UUID.randomUUID(),
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
                createdAt = event.addedAt,
                examSessionSchedule = schedule
            )

            scheduledExamRepository.save(scheduledExam)

            examSessionScheduleRepository.save(schedule.copy(updatedAt = event.addedAt))
        }
    }

    @EventHandler
    fun on(event: ScheduledExamTimeChangedEvent) {
        logger.info("Updating exam time for: {}", event.scheduledExamId)

        scheduledExamRepository.findByScheduledExamId(event.scheduledExamId)?.let { exam ->
            val updatedExam = exam.copy(
                examDate = event.newExamDate,
                startTime = event.newStartTime,
                endTime = event.newEndTime,
                updatedAt = event.updatedAt
            )
            scheduledExamRepository.save(updatedExam)

            logAdjustment(
                scheduleId = event.scheduleId,
                examId = event.scheduledExamId,
                adjustmentType = AdjustmentType.TIME_CHANGE,
                description = "Exam time changed: ${event.oldExamDate} ${event.oldStartTime}-${event.oldEndTime} -> ${event.newExamDate} ${event.newStartTime}-${event.newEndTime}",
                reason = event.reason,
                adminId = event.updatedBy,
                timestamp = event.updatedAt,
                oldValues = "${event.oldExamDate},${event.oldStartTime},${event.oldEndTime}",
                newValues = "${event.newExamDate},${event.newStartTime},${event.newEndTime}"
            )

            if (event.conflictsCreated.isNotEmpty() || event.conflictsResolved.isNotEmpty()) {
                val notification = mapOf(
                    "eventType" to "ExamTimeChangeImpact",
                    "scheduleId" to event.scheduleId.toString(),
                    "scheduledExamId" to event.scheduledExamId,
                    "courseId" to event.courseId,
                    "impactedStudents" to event.impactedStudents,
                    "conflictsResolved" to event.conflictsResolved,
                    "conflictsCreated" to event.conflictsCreated,
                    "timestamp" to event.updatedAt.toString()
                )
                eventPublisher.publishSystemNotification(notification, "time-change-impact-${event.scheduledExamId}")
            }
        }
    }

    @EventHandler
    fun on(event: ScheduledExamSpaceChangedEvent) {
        logger.info("Updating exam space for: {}", event.scheduledExamId)

        scheduledExamRepository.findByScheduledExamId(event.scheduledExamId)?.let { exam ->
            val updatedExam = exam.copy(
                roomId = event.newRoomId,
                roomName = event.newRoomName,
                roomCapacity = event.newRoomCapacity,
                updatedAt = event.updatedAt
            )
            scheduledExamRepository.save(updatedExam)

            logAdjustment(
                scheduleId = event.scheduleId,
                examId = event.scheduledExamId,
                adjustmentType = AdjustmentType.ROOM_CHANGE,
                description = "Exam room changed: ${event.oldRoomName ?: event.oldRoomId} -> ${event.newRoomName ?: event.newRoomId}",
                reason = event.reason,
                adminId = event.updatedBy,
                timestamp = event.updatedAt,
                oldValues = "${event.oldRoomId},${event.oldRoomName},${event.oldRoomCapacity}",
                newValues = "${event.newRoomId},${event.newRoomName},${event.newRoomCapacity}"
            )
        }
    }

    @EventHandler
    fun on(event: ScheduledExamRemovedEvent) {
        logger.info("Removing scheduled exam: {}", event.scheduledExamId)

        scheduledExamRepository.findByScheduledExamId(event.scheduledExamId)?.let { exam ->
            scheduledExamRepository.delete(exam)

            // Log the removal
            logAdjustment(
                scheduleId = event.scheduleId,
                examId = event.scheduledExamId,
                adjustmentType = AdjustmentType.CANCELLATION,
                description = "Scheduled exam removed for course: ${event.courseId}",
                reason = event.reason,
                adminId = event.removedBy,
                timestamp = event.removedAt
            )
        }
    }

    // === Professor Comment Events ===

    @EventHandler
    fun on(event: ProfessorFeedbackSubmittedEvent) {
        logger.info("Adding professor feedback: {} from professor: {}", event.commentId, event.professorId)

        examSessionScheduleRepository.findById(event.scheduleId).ifPresent { schedule ->
            val comment = ProfessorComment(
                id = java.util.UUID.randomUUID(),
                commentId = event.commentId,
                professorId = event.professorId,
                scheduledExamId = event.scheduledExamId,
                commentText = event.commentText,
                commentType = event.commentType,
                status = event.status,
                submittedAt = event.submittedAt,
                examSessionSchedule = schedule
            )

            professorCommentRepository.save(comment)

            if (schedule.status == ScheduleStatus.PUBLISHED_FOR_REVIEW) {
                examSessionScheduleRepository.save(schedule.copy(
                    status = ScheduleStatus.UNDER_REVIEW,
                    updatedAt = event.submittedAt
                ))
            }

            val notification = mapOf(
                "eventType" to "ProfessorFeedbackReceived",
                "scheduleId" to event.scheduleId.toString(),
                "commentId" to event.commentId,
                "professorId" to event.professorId,
                "commentType" to event.commentType.name,
                "scheduledExamId" to event.scheduledExamId,
                "urgent" to (event.commentType == CommentType.URGENT_CHANGE),
                "timestamp" to event.submittedAt.toString()
            )
            eventPublisher.publishSystemNotification(notification, "feedback-received-${event.commentId}")
        }
    }

    @EventHandler
    fun on(event: ProfessorFeedbackReviewedEvent) {
        logger.info("Reviewing professor feedback: {}", event.commentId)

        professorCommentRepository.findByCommentId(event.commentId)?.let { comment ->
            val updatedComment = comment.copy(
                status = event.newStatus,
                reviewedAt = event.reviewedAt,
                reviewedBy = event.reviewedBy
            )
            professorCommentRepository.save(updatedComment)
        }
    }

    // === Schedule Adjustment Events ===

    @EventHandler
    fun on(event: ScheduleAdjustmentRequestedEvent) {
        logger.info("Recording adjustment request: {}", event.adjustmentId)

        examSessionScheduleRepository.findById(event.scheduleId).ifPresent { schedule ->
            val adjustmentLog = AdjustmentLog(
                id = java.util.UUID.randomUUID(),
                adjustmentId = event.adjustmentId,
                adminId = event.requestedBy,
                commentId = event.commentId,
                scheduledExamId = event.scheduledExamId,
                adjustmentType = event.adjustmentType,
                description = event.description,
                reason = event.reason,
                timestamp = event.requestedAt,
                status = event.status,
                examSessionSchedule = schedule
            )

            adjustmentLogRepository.save(adjustmentLog)
        }
    }

    @EventHandler
    fun on(event: ScheduleAdjustmentApprovedEvent) {
        logger.info("Approving adjustment request: {}", event.adjustmentId)

        adjustmentLogRepository.findByAdjustmentId(event.adjustmentId)?.let { adjustment ->
            val updatedAdjustment = adjustment.copy(
                status = AdjustmentStatus.APPROVED,
                oldValues = event.oldValues,
                newValues = event.newValues
            )
            adjustmentLogRepository.save(updatedAdjustment)

            val notification = mapOf(
                "eventType" to "AdjustmentApproved",
                "scheduleId" to event.scheduleId.toString(),
                "adjustmentId" to event.adjustmentId,
                "adjustmentType" to event.adjustmentType.name,
                "approvedBy" to event.approvedBy,
                "scheduledExamId" to event.scheduledExamId,
                "approvalNotes" to event.approvalNotes,
                "timestamp" to event.approvedAt.toString()
            )
            eventPublisher.publishSystemNotification(notification, "adjustment-approved-${event.adjustmentId}")
        }
    }

    @EventHandler
    fun on(event: ScheduleAdjustmentRejectedEvent) {
        logger.info("Rejecting adjustment request: {}", event.adjustmentId)

        adjustmentLogRepository.findByAdjustmentId(event.adjustmentId)?.let { adjustment ->
            val updatedAdjustment = adjustment.copy(
                status = AdjustmentStatus.REJECTED,
                reason = event.rejectionReason
            )
            adjustmentLogRepository.save(updatedAdjustment)
        }
    }

    private fun logAdjustment(
        scheduleId: java.util.UUID,
        examId: String? = null,
        adjustmentType: AdjustmentType,
        description: String,
        reason: String,
        adminId: String,
        timestamp: java.time.Instant,
        oldValues: String? = null,
        newValues: String? = null
    ) {
        examSessionScheduleRepository.findById(scheduleId).ifPresent { schedule ->
            val adjustmentLog = AdjustmentLog(
                id = java.util.UUID.randomUUID(),
                adjustmentId = "auto-${java.util.UUID.randomUUID()}",
                adminId = adminId,
                scheduledExamId = examId,
                adjustmentType = adjustmentType,
                description = description,
                reason = reason,
                timestamp = timestamp,
                status = AdjustmentStatus.APPLIED,
                oldValues = oldValues,
                newValues = newValues,
                examSessionSchedule = schedule
            )

            adjustmentLogRepository.save(adjustmentLog)
        }
    }
}