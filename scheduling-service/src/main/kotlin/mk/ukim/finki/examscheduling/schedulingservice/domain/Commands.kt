package mk.ukim.finki.examscheduling.schedulingservice.domain

import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.AdjustmentType
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.CommentType
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.MandatoryStatus
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

data class CreateExamSessionScheduleCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdBy: String
)

data class TriggerDraftScheduleGenerationCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val triggeredBy: String,
    val externalDataRequired: Boolean = true
)

data class GenerateDraftScheduleCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val courseEnrollmentData: Map<String, CourseEnrollmentInfo>,
    val courseAccreditationData: Map<String, CourseAccreditationInfo>,
    val professorPreferences: List<ProfessorPreferenceInfo>,
    val availableRooms: List<RoomInfo>,
    val generatedBy: String
)

data class AddScheduledExamCommand(
    @TargetAggregateIdentifier
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
    val addedBy: String
)

data class UpdateScheduledExamTimeCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val scheduledExamId: String,
    val newExamDate: LocalDate,
    val newStartTime: LocalTime,
    val newEndTime: LocalTime,
    val reason: String,
    val updatedBy: String
)

data class UpdateScheduledExamSpaceCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val scheduledExamId: String,
    val newRoomId: String?,
    val newRoomName: String?,
    val newRoomCapacity: Int?,
    val reason: String,
    val updatedBy: String
)

data class RemoveScheduledExamCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val scheduledExamId: String,
    val reason: String,
    val removedBy: String
)

data class SubmitProfessorFeedbackCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val commentId: String,
    val professorId: String,
    val scheduledExamId: String?,
    val commentText: String,
    val commentType: CommentType
)

data class ReviewProfessorFeedbackCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val commentId: String,
    val reviewedBy: String,
    val reviewNotes: String?
)

// Adjustment Management Commands
data class RequestScheduleAdjustmentCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val adjustmentId: String,
    val commentId: String?,
    val scheduledExamId: String?,
    val adjustmentType: AdjustmentType,
    val description: String,
    val requestedBy: String,
    val reason: String?
)

data class ApproveScheduleAdjustmentCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val adjustmentId: String,
    val approvedBy: String,
    val approvalNotes: String?
)

data class RejectScheduleAdjustmentCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val adjustmentId: String,
    val rejectedBy: String,
    val rejectionReason: String
)

data class PublishDraftScheduleForReviewCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val publishedBy: String,
    val publishNotes: String?
)

data class FinalizeScheduleCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val finalizedBy: String,
    val finalizeNotes: String?
)

data class PublishFinalScheduleCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val publishedBy: String,
    val publishNotes: String?
)

data class OptimizeScheduleCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val optimizationStrategy: SolvingStrategy? = null,
    val availableRooms: List<RoomInfo>,
    val professorPreferences: List<ProfessorPreferenceInfo>,
    val previousQualityScore: Double? = null,
    val optimizedBy: String
)