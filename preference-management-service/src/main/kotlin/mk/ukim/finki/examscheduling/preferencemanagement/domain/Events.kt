package mk.ukim.finki.examscheduling.preferencemanagement.domain

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import mk.ukim.finki.examscheduling.preferencemanagement.domain.enums.ValidationRuleType
import mk.ukim.finki.examscheduling.preferencemanagement.domain.enums.ValidationSeverity
import java.time.Instant
import java.util.*

abstract class PreferenceManagementEvent {
    val eventId: String = UUID.randomUUID().toString()
    val timestamp: Instant = Instant.now()
    val serviceName: String = "preference-management-service"
}

data class ExamSessionPeriodCreatedEvent @JsonCreator constructor(
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("academicYear") val academicYear: String,
    @JsonProperty("examSession") val examSession: String,
    @JsonProperty("createdBy") val createdBy: String,
    @JsonProperty("plannedStartDate") val plannedStartDate: Instant,
    @JsonProperty("plannedEndDate") val plannedEndDate: Instant,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("createdAt") val createdAt: Instant = Instant.now()
) : PreferenceManagementEvent()

data class PreferenceSubmissionWindowOpenedEvent @JsonCreator constructor(
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("academicYear") val academicYear: String,
    @JsonProperty("examSession") val examSession: String,
    @JsonProperty("openedBy") val openedBy: String,
    @JsonProperty("submissionDeadline") val submissionDeadline: Instant,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("openedAt") val openedAt: Instant = Instant.now()
) : PreferenceManagementEvent()

data class PreferenceSubmissionWindowClosedEvent @JsonCreator constructor(
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("closedBy") val closedBy: String,
    @JsonProperty("reason") val reason: String? = null,
    @JsonProperty("closedAt") val closedAt: Instant = Instant.now(),
    @JsonProperty("totalSubmissions") val totalSubmissions: Int = 0
) : PreferenceManagementEvent()

data class ProfessorPreferenceSubmittedEvent @JsonCreator constructor(
    @JsonProperty("submissionId") val submissionId: SubmissionId,
    @JsonProperty("professorId") val professorId: ProfessorId,
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("courseIds") val courseIds: List<CourseId>,
    @JsonProperty("preferences") val preferences: List<PreferenceDetails>,
    @JsonProperty("submissionVersion") val submissionVersion: Int = 1,
    @JsonProperty("submittedAt") val submittedAt: Instant = Instant.now(),
    @JsonProperty("isUpdate") @JsonAlias("update") val isUpdate: Boolean = false
) : PreferenceManagementEvent() {


    @JsonIgnore
    fun getTotalTimePreferences(): Int = preferences.sumOf { it.timePreferences.size }

    @JsonIgnore
    fun getTotalRoomPreferences(): Int = preferences.sumOf { it.roomPreferences.size }

    @JsonIgnore
    fun getCoursesWithSpecialRequirements(): List<CourseId> =
        preferences.filter { !it.specialRequirements.isNullOrBlank() }.map { it.courseId }
}

data class ProfessorPreferenceUpdatedEvent @JsonCreator constructor(
    @JsonProperty("submissionId") val submissionId: SubmissionId,
    @JsonProperty("professorId") val professorId: ProfessorId,
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("updatedPreferences") val updatedPreferences: List<PreferenceDetails>,
    @JsonProperty("previousVersion") val previousVersion: Int,
    @JsonProperty("newVersion") val newVersion: Int,
    @JsonProperty("updateReason") val updateReason: String? = null,
    @JsonProperty("updatedAt") val updatedAt: Instant = Instant.now()
) : PreferenceManagementEvent()

data class ProfessorPreferenceWithdrawnEvent @JsonCreator constructor(
    @JsonProperty("submissionId") val submissionId: SubmissionId,
    @JsonProperty("professorId") val professorId: ProfessorId,
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("withdrawnBy") val withdrawnBy: String,
    @JsonProperty("withdrawalReason") val withdrawalReason: String,
    @JsonProperty("withdrawnAt") val withdrawnAt: Instant = Instant.now(),
    @JsonProperty("finalVersion") val finalVersion: Int
) : PreferenceManagementEvent()

data class PreferenceValidationFailedEvent @JsonCreator constructor(
    @JsonProperty("submissionId") val submissionId: SubmissionId,
    @JsonProperty("professorId") val professorId: ProfessorId,
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("validationErrors") val validationErrors: List<ValidationError>,
    @JsonProperty("validatedAt") val validatedAt: Instant = Instant.now(),
    @JsonProperty("attemptedSubmission") val attemptedSubmission: String? = null
) : PreferenceManagementEvent() {
    @JsonIgnore
    fun hasErrorsOfSeverity(severity: ValidationSeverity): Boolean =
        validationErrors.any { it.severity == severity }

    @JsonIgnore
    fun getErrorsOfType(type: ValidationRuleType): List<ValidationError> =
        validationErrors.filter { it.ruleType == type }
}

data class PreferenceValidationSucceededEvent @JsonCreator constructor(
    @JsonProperty("submissionId") val submissionId: SubmissionId,
    @JsonProperty("professorId") val professorId: ProfessorId,
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("validationWarnings") val validationWarnings: List<ValidationError> = emptyList(),
    @JsonProperty("validatedAt") val validatedAt: Instant = Instant.now()
) : PreferenceManagementEvent()

data class PreferenceTemplateCreatedEvent @JsonCreator constructor(
    @JsonProperty("templateId") val templateId: UUID,
    @JsonProperty("templateName") val templateName: String,
    @JsonProperty("createdBy") val createdBy: String,
    @JsonProperty("templatePreferences") val templatePreferences: List<PreferenceDetails>,
    @JsonProperty("applicableToSessions") val applicableToSessions: Set<String> = emptySet(),
    @JsonProperty("createdAt") val createdAt: Instant = Instant.now()
) : PreferenceManagementEvent()

data class PreferencesImportedFromTemplateEvent @JsonCreator constructor(
    @JsonProperty("submissionId") val submissionId: SubmissionId,
    @JsonProperty("professorId") val professorId: ProfessorId,
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("templateId") val templateId: UUID,
    @JsonProperty("importedPreferences") val importedPreferences: List<PreferenceDetails>,
    @JsonProperty("importedAt") val importedAt: Instant = Instant.now()
) : PreferenceManagementEvent()

data class ValidationError @JsonCreator constructor(
    @JsonProperty("ruleType") val ruleType: ValidationRuleType,
    @JsonProperty("severity") val severity: ValidationSeverity,
    @JsonProperty("message") val message: String,
    @JsonProperty("affectedCourseId") val affectedCourseId: CourseId? = null,
    @JsonProperty("affectedTimeSlot") val affectedTimeSlot: TimeSlot? = null,
    @JsonProperty("suggestedFix") val suggestedFix: String? = null
) {
    init {
        require(message.isNotBlank()) { "Validation error message cannot be blank" }
        require(message.length <= 500) { "Validation error message cannot exceed 500 characters" }
    }
}

data class PreferenceAutoProcessedEvent @JsonCreator constructor(
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("processedSubmissions") val processedSubmissions: List<SubmissionId>,
    @JsonProperty("processingRules") val processingRules: List<String>,
    @JsonProperty("processedAt") val processedAt: Instant = Instant.now(),
    @JsonProperty("processedBy") val processedBy: String = "SYSTEM"
) : PreferenceManagementEvent()

data class PreferenceStatisticsGeneratedEvent @JsonCreator constructor(
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("totalSubmissions") val totalSubmissions: Int,
    @JsonProperty("submissionsByProfessor") val submissionsByProfessor: Map<ProfessorId, Int>,
    @JsonProperty("preferencesByTimeSlot") val preferencesByTimeSlot: Map<TimeSlot, Int>,
    @JsonProperty("mostRequestedRooms") val mostRequestedRooms: List<String>,
    @JsonProperty("conflictingPreferences") val conflictingPreferences: Int,
    @JsonProperty("generatedAt") val generatedAt: Instant = Instant.now()
) : PreferenceManagementEvent()

data class PreferenceSystemNotificationEvent @JsonCreator constructor(
    @JsonProperty("notificationType") val notificationType: String,
    @JsonProperty("message") val message: String,
    @JsonProperty("targetService") val targetService: String? = null,
    @JsonProperty("relatedEntityId") val relatedEntityId: String? = null,
    @JsonProperty("metadata") val metadata: Map<String, Any> = emptyMap()
) : PreferenceManagementEvent()

data class PreferenceAuditEvent @JsonCreator constructor(
    @JsonProperty("action") val action: String,
    @JsonProperty("performedBy") val performedBy: String,
    @JsonProperty("entityType") val entityType: String,
    @JsonProperty("entityId") val entityId: String,
    @JsonProperty("changeDetails") val changeDetails: Map<String, Any> = emptyMap(),
    @JsonProperty("performedAt") val performedAt: Instant = Instant.now()
) : PreferenceManagementEvent()