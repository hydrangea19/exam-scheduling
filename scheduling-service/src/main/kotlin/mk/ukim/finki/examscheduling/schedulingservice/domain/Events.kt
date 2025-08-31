package mk.ukim.finki.examscheduling.schedulingservice.domain

import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

data class ExamSessionScheduleCreatedEvent(
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: ScheduleStatus,
    val createdBy: String,
    val createdAt: Instant
)

data class DraftScheduleGenerationTriggeredEvent(
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val triggeredBy: String,
    val triggeredAt: Instant,
    val externalDataRequired: Boolean
)

data class DraftScheduleGeneratedEvent(
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val totalExams: Int,
    val schedulingMetrics: SchedulingMetrics,
    val generatedBy: String,
    val generatedAt: Instant,
    val qualityScore: Double,
    val constraintViolations: List<ConstraintViolation>
)

data class DraftSchedulePublishedForReviewEvent(
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val publishedBy: String,
    val publishedAt: Instant,
    val publishNotes: String?,
    val reviewDeadline: Instant?
)

data class ScheduleFinalizedEvent(
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val finalizedBy: String,
    val finalizedAt: Instant,
    val finalizeNotes: String?,
    val totalExams: Int,
    val finalQualityScore: Double
)

data class FinalSchedulePublishedEvent(
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val publishedBy: String,
    val publishedAt: Instant,
    val publishNotes: String?,
    val distributionChannels: List<String>
)

// Scheduled Exam Management Events
data class ScheduledExamAddedEvent(
    val scheduleId: UUID,
    val scheduledExamId: String,
    val courseId: String,
    val courseName: String,
    val examDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val roomId: String?,
    val roomName: String?,
    val roomCapacity: Int?,
    val studentCount: Int,
    val mandatoryStatus: MandatoryStatus,
    val professorIds: Set<String>,
    val addedBy: String,
    val addedAt: Instant
)

data class ScheduledExamTimeChangedEvent(
    val scheduleId: UUID,
    val scheduledExamId: String,
    val courseId: String,
    val oldExamDate: LocalDate,
    val oldStartTime: LocalTime,
    val oldEndTime: LocalTime,
    val newExamDate: LocalDate,
    val newStartTime: LocalTime,
    val newEndTime: LocalTime,
    val reason: String,
    val updatedBy: String,
    val updatedAt: Instant,
    val impactedStudents: Int,
    val conflictsResolved: List<String>,
    val conflictsCreated: List<String>
)

data class ScheduledExamSpaceChangedEvent(
    val scheduleId: UUID,
    val scheduledExamId: String,
    val courseId: String,
    val oldRoomId: String?,
    val oldRoomName: String?,
    val oldRoomCapacity: Int?,
    val newRoomId: String?,
    val newRoomName: String?,
    val newRoomCapacity: Int?,
    val reason: String,
    val updatedBy: String,
    val updatedAt: Instant,
    val capacityImpact: Int
)

data class ScheduledExamDetailsUpdatedEvent(
    val scheduleId: UUID,
    val scheduledExamId: String,
    val courseId: String,
    val updates: Map<String, Any>,
    val updatedBy: String,
    val updatedAt: Instant,
    val reason: String
)

data class ScheduledExamRemovedEvent(
    val scheduleId: UUID,
    val scheduledExamId: String,
    val courseId: String,
    val reason: String,
    val removedBy: String,
    val removedAt: Instant
)

data class ProfessorFeedbackSubmittedEvent(
    val scheduleId: UUID,
    val commentId: String,
    val professorId: String,
    val scheduledExamId: String?,
    val commentText: String,
    val commentType: CommentType,
    val status: CommentStatus,
    val submittedAt: Instant
)

data class ProfessorFeedbackReviewedEvent(
    val scheduleId: UUID,
    val commentId: String,
    val professorId: String,
    val reviewedBy: String,
    val reviewedAt: Instant,
    val reviewNotes: String?,
    val newStatus: CommentStatus
)

data class ScheduleAdjustmentRequestedEvent(
    val scheduleId: UUID,
    val adjustmentId: String,
    val commentId: String?,
    val scheduledExamId: String?,
    val adjustmentType: AdjustmentType,
    val description: String,
    val requestedBy: String,
    val requestedAt: Instant,
    val reason: String?,
    val status: AdjustmentStatus
)

data class ScheduleAdjustmentApprovedEvent(
    val scheduleId: UUID,
    val adjustmentId: String,
    val commentId: String?,
    val scheduledExamId: String?,
    val adjustmentType: AdjustmentType,
    val description: String,
    val approvedBy: String,
    val approvedAt: Instant,
    val approvalNotes: String?,
    val oldValues: String?,
    val newValues: String?
)

data class ScheduleAdjustmentRejectedEvent(
    val scheduleId: UUID,
    val adjustmentId: String,
    val commentId: String?,
    val adjustmentType: AdjustmentType,
    val description: String,
    val rejectedBy: String,
    val rejectedAt: Instant,
    val rejectionReason: String
)

data class ScheduleOptimizationCompletedEvent(
    val scheduleId: UUID,
    val optimizationStrategy: String,
    val previousQualityScore: Double,
    val newQualityScore: Double,
    val improvementRate: Double,
    val optimizationTime: Long,
    val optimizedBy: String,
    val optimizedAt: Instant,
    val constraintViolationsResolved: Int,
    val optimizationMetrics: Map<String, Any>
)

data class ScheduleOptimizationFailedEvent(
    val scheduleId: UUID,
    val failureReason: String,
    val failedAt: Instant,
    val attemptedBy: String
)