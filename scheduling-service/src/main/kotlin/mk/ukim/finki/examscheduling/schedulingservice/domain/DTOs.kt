package mk.ukim.finki.examscheduling.schedulingservice.domain

import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

data class CourseEnrollmentInfo(
    val courseId: String,
    val studentCount: Int,
    val enrollmentDetails: Map<String, Any> = emptyMap()
)

data class CourseAccreditationInfo(
    val courseId: String,
    val courseName: String,
    val mandatoryStatus: MandatoryStatus,
    val credits: Int,
    val professorIds: Set<String>,
    val prerequisites: Set<String> = emptySet(),
    val accreditationDetails: Map<String, Any> = emptyMap()
)

data class ProfessorPreferenceInfo(
    val preferenceId: String,
    val professorId: String,
    val courseId: String,
    val preferredDates: List<LocalDate> = emptyList(),
    val preferredTimeSlots: List<TimeSlotPreference> = emptyList(),
    val preferredRooms: List<String> = emptyList(),
    val unavailableDates: List<LocalDate> = emptyList(),
    val unavailableTimeSlots: List<TimeSlotPreference> = emptyList(),
    val specialRequirements: String? = null,
    val priority: Int = 1
)

data class TimeSlotPreference(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val dayOfWeek: Int? = null
)

data class RoomInfo(
    val roomId: String,
    val roomName: String,
    val capacity: Int,
    val equipment: Set<String> = emptySet(),
    val location: String? = null,
    val accessibility: Boolean = true,
    val availableTimeSlots: List<TimeSlotPreference> = emptyList()
)

data class SchedulingMetrics(
    val totalCoursesScheduled: Int,
    val totalProfessorPreferencesConsidered: Int,
    val preferencesSatisfied: Int,
    val preferenceSatisfactionRate: Double,
    val totalConflicts: Int,
    val resolvedConflicts: Int,
    val roomUtilizationRate: Double,
    val averageStudentExamsPerDay: Double,
    val processingTimeMs: Long
)

data class ConstraintViolation(
    val violationType: String,
    val severity: ViolationSeverity,
    val description: String,
    val affectedExams: List<String>,
    val affectedStudents: Int,
    val suggestedResolution: String? = null
)

data class ScheduledExamEntity(
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
    val professorIds: MutableSet<String>,
    val createdAt: Instant,
    val updatedAt: Instant? = null
)

data class ProfessorCommentEntity(
    val commentId: String,
    val professorId: String,
    val scheduledExamId: String?,
    val commentText: String,
    val commentType: CommentType,
    val status: CommentStatus,
    val submittedAt: Instant,
    val reviewedAt: Instant? = null,
    val reviewedBy: String? = null
)

data class AdjustmentLogEntity(
    val adjustmentId: String,
    val commentId: String?,
    val scheduledExamId: String?,
    val adjustmentType: AdjustmentType,
    val description: String,
    val requestedBy: String,
    val requestedAt: Instant,
    val reason: String?,
    val status: AdjustmentStatus,
    val oldValues: String? = null,
    val newValues: String? = null
)

data class TimeChangeImpact(
    val impactedStudents: Int,
    val conflictsResolved: List<String>,
    val conflictsCreated: List<String>
)

data class SchedulingResult(
    val scheduledExams: List<ScheduledExamInfo>,
    val metrics: SchedulingMetrics,
    val qualityScore: Double,
    val violations: List<ConstraintViolation>
)

data class ScheduledExamInfo(
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
    val professorIds: Set<String>
)

data class InitiateScheduleGenerationCommand(
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val initiatedBy: String
)

data class BatchUpdateScheduledExamsCommand(
    val scheduleId: UUID,
    val examUpdates: List<ExamUpdate>,
    val updatedBy: String
)

data class ExamUpdate(
    val scheduledExamId: String,
    val updateType: ExamUpdateType,
    val newExamDate: java.time.LocalDate? = null,
    val newStartTime: java.time.LocalTime? = null,
    val newEndTime: java.time.LocalTime? = null,
    val newRoomId: String? = null,
    val newRoomName: String? = null,
    val newRoomCapacity: Int? = null,
    val reason: String? = null
)

data class HandleScheduleGenerationFailureCommand(
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val failureReason: String,
    val failedAt: java.time.Instant
)

data class QualityWeights(
    val preferenceSatisfaction: Double,
    val conflictMinimization: Double,
    val resourceUtilization: Double,
    val studentWorkloadDistribution: Double,
    val institutionalPolicies: Double
)

data class QualityScoreResult(
    val overallScore: Double,
    val preferenceSatisfactionScore: Double,
    val conflictMinimizationScore: Double,
    val resourceUtilizationScore: Double,
    val studentWorkloadScore: Double,
    val policyComplianceScore: Double,
    val breakdown: QualityScoreBreakdown
)

data class QualityScoreBreakdown(
    val totalExams: Int,
    val totalPreferences: Int,
    val satisfiedPreferences: Int,
    val totalConflicts: Int,
    val criticalViolations: Int,
    val recommendations: List<String>
)

data class ConflictAnalysisResult(
    val totalExamSlots: Int,
    val totalConflicts: Int,
    val criticalViolations: Int,
    val timeConflicts: List<TimeConflict>,
    val spaceConflicts: List<SpaceConflict>,
    val professorConflicts: List<ProfessorConflict>
)

data class TimeConflict(
    val examId1: String,
    val examId2: String,
    val conflictType: String,
    val severity: ConflictSeverity,
    val affectedStudents: Int
)

data class SpaceConflict(
    val examId: String,
    val roomId: String,
    val requiredCapacity: Int,
    val availableCapacity: Int,
    val overflowCount: Int
)

data class ProfessorConflict(
    val professorId: String,
    val conflictingExamIds: List<String>,
    val conflictTime: String,
    val severity: ConflictSeverity
)

data class ScheduleComparisonResult(
    val scheduleId: UUID,
    val version1: Int,
    val version2: Int,
    val differences: List<ScheduleDifference>,
    val comparedAt: Instant
)

data class ScheduleDifference(
    val type: DifferenceType,
    val examId: String? = null,
    val courseId: String? = null,
    val property: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null,
    val description: String
)

data class ExamChangeProposal(
    val newExamDate: LocalDate? = null,
    val newStartTime: LocalTime? = null,
    val newEndTime: LocalTime? = null,
    val newRoomId: String? = null,
    val newRoomName: String? = null,
    val newRoomCapacity: Int? = null,
    val reason: String
)

data class ChangeImpactAnalysis(
    val examId: String,
    val proposedChanges: ExamChangeProposal,
    val conflictsResolved: List<String>,
    val conflictsCreated: List<String>,
    val impactedStudents: Int,
    val recommendationScore: Double,
    val additionalImpacts: List<String>
)

data class CreateScheduleRequest(
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class ScheduleResponse(
    val id: UUID,
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: ScheduleStatus,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val finalizedAt: Instant?,
    val publishedAt: Instant?,
    val totalExams: Int = 0
)

data class DetailedScheduleResponse(
    val schedule: ScheduleResponse,
    val exams: List<ExamResponse>,
    val comments: List<CommentResponse>,
    val adjustments: List<AdjustmentResponse>,
    val metrics: QualityMetricsResponse?,
    val conflicts: List<ConflictResponse>
)

data class AddExamRequest(
    val scheduledExamId: String? = null,
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
    val professorIds: Set<String>
)

data class ExamResponse(
    val id: UUID,
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
    val professorIds: Set<String>
)

data class UpdateExamTimeRequest(
    val examDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val reason: String
)

data class UpdateExamSpaceRequest(
    val roomId: String?,
    val roomName: String?,
    val roomCapacity: Int?,
    val reason: String
)

data class SubmitFeedbackRequest(
    val scheduledExamId: String?,
    val commentText: String,
    val commentType: CommentType
)

data class CommentResponse(
    val id: UUID,
    val commentId: String,
    val professorId: String,
    val scheduledExamId: String?,
    val commentText: String,
    val commentType: CommentType,
    val status: CommentStatus,
    val submittedAt: Instant,
    val reviewedAt: Instant?,
    val reviewedBy: String?
)

data class AdjustmentResponse(
    val id: UUID,
    val adjustmentId: String,
    val adminId: String,
    val commentId: String?,
    val scheduledExamId: String?,
    val adjustmentType: AdjustmentType,
    val description: String,
    val reason: String?,
    val timestamp: Instant,
    val status: AdjustmentStatus,
    val oldValues: String?,
    val newValues: String?
)

data class QualityMetricsResponse(
    val scheduleId: UUID,
    val qualityScore: Double,
    val finalQualityScore: Double?,
    val preferenceSatisfactionRate: Double,
    val totalConflicts: Int,
    val resolvedConflicts: Int,
    val roomUtilizationRate: Double,
    val recommendations: List<String> = emptyList()
)

data class ConflictResponse(
    val id: UUID,
    val conflictId: String,
    val conflictType: ConflictType,
    val severity: ConflictSeverity,
    val description: String,
    val affectedExamIds: Set<String>,
    val affectedStudents: Int,
    val suggestedResolution: String?,
    val status: ConflictStatus
)

data class GenerationResponse(
    val scheduleId: UUID,
    val status: String,
    val message: String,
    val estimatedCompletionTime: Instant?
)

data class PublishForReviewRequest(val notes: String?)
data class FinalizeScheduleRequest(val notes: String?)
data class ResolveConflictRequest(val resolutionNotes: String?)
data class CreateVersionRequest(val versionType: ScheduleVersionType, val notes: String?)

data class VersionResponse(
    val id: UUID,
    val scheduleId: UUID,
    val versionNumber: Int,
    val versionType: ScheduleVersionType,
    val versionNotes: String?,
    val scheduleStatus: ScheduleStatus,
    val createdBy: String,
    val createdAt: Instant
)

fun ExamSessionSchedule.toResponse() = ScheduleResponse(
    id = id,
    examSessionPeriodId = examSessionPeriodId,
    academicYear = academicYear,
    examSession = examSession,
    startDate = startDate,
    endDate = endDate,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    finalizedAt = finalizedAt,
    publishedAt = publishedAt,
)

fun ScheduledExam.toResponse() = ExamResponse(
    id = id,
    scheduledExamId = scheduledExamId,
    courseId = courseId,
    courseName = courseName,
    examDate = examDate,
    startTime = startTime,
    endTime = endTime,
    roomId = roomId,
    roomName = roomName,
    roomCapacity = roomCapacity,
    studentCount = studentCount,
    mandatoryStatus = mandatoryStatus,
    professorIds = professorIds
)

fun ProfessorComment.toResponse() = CommentResponse(
    id = id,
    commentId = commentId,
    professorId = professorId,
    scheduledExamId = scheduledExamId,
    commentText = commentText,
    commentType = commentType,
    status = status,
    submittedAt = submittedAt,
    reviewedAt = reviewedAt,
    reviewedBy = reviewedBy
)

fun AdjustmentLog.toResponse() = AdjustmentResponse(
    id = id,
    adjustmentId = adjustmentId,
    adminId = adminId,
    commentId = commentId,
    scheduledExamId = scheduledExamId,
    adjustmentType = adjustmentType,
    description = description,
    reason = reason,
    timestamp = timestamp,
    status = status,
    oldValues = oldValues,
    newValues = newValues
)

fun ScheduleMetricsEntity.toResponse() = QualityMetricsResponse(
    scheduleId = scheduleId,
    qualityScore = qualityScore,
    finalQualityScore = finalQualityScore,
    preferenceSatisfactionRate = preferenceSatisfactionRate,
    totalConflicts = totalConflicts,
    resolvedConflicts = resolvedConflicts,
    roomUtilizationRate = roomUtilizationRate
)

fun ScheduleConflictEntity.toResponse() = ConflictResponse(
    id = id,
    conflictId = conflictId,
    conflictType = conflictType,
    severity = severity,
    description = description,
    affectedExamIds = affectedExamIds,
    affectedStudents = affectedStudents,
    suggestedResolution = suggestedResolution,
    status = status
)

fun ScheduleVersionEntity.toResponse() = VersionResponse(
    id = id,
    scheduleId = scheduleId,
    versionNumber = versionNumber,
    versionType = versionType,
    versionNotes = versionNotes,
    scheduleStatus = scheduleStatus,
    createdBy = createdBy,
    createdAt = createdAt
)

data class QualityTrendsResult(
    val scheduleId: UUID,
    val dataPoints: Int,
    val overallTrend: TrendDirection,
    val qualityScoreTrend: Double,
    val preferenceSatisfactionTrend: Double,
    val conflictResolutionTrend: Double,
    val recommendations: List<String>
)

enum class TrendDirection {
    IMPROVING, DECLINING, STABLE
}

data class SessionComparisonResult(
    val academicYear: String,
    val sessionStats: List<SessionQualityStats>,
    val bestOverallSession: SessionQualityStats?,
    val recommendations: List<String>
)

data class SessionQualityStats(
    val examSessionPeriodId: String,
    val averageQualityScore: Double,
    val bestQualityScore: Double,
    val averagePreferenceSatisfaction: Double,
    val averageRoomUtilization: Double,
    val totalGenerations: Int,
    val latestUpdate: Instant?
)


