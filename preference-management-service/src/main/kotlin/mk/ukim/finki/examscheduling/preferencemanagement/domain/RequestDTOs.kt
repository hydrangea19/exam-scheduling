package mk.ukim.finki.examscheduling.preferencemanagement.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

data class CreateExamSessionPeriodRequest @JsonCreator constructor(
    @JsonProperty("academicYear") val academicYear: String,
    @JsonProperty("examSession") val examSession: String,
    @JsonProperty("createdBy") val createdBy: String,
    @JsonProperty("plannedStartDate") val plannedStartDate: Instant,
    @JsonProperty("plannedEndDate") val plannedEndDate: Instant,
    @JsonProperty("description") val description: String? = null
)

data class OpenSubmissionWindowRequest @JsonCreator constructor(
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: String,
    @JsonProperty("academicYear") val academicYear: String,
    @JsonProperty("examSession") val examSession: String,
    @JsonProperty("openedBy") val openedBy: String,
    @JsonProperty("submissionDeadline") val submissionDeadline: Instant,
    @JsonProperty("description") val description: String? = null
)

data class CloseSubmissionWindowRequest @JsonCreator constructor(
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: String,
    @JsonProperty("closedBy") val closedBy: String,
    @JsonProperty("reason") val reason: String? = null,
    @JsonProperty("totalSubmissions") val totalSubmissions: Int = 0
)

data class SubmitPreferencesRequest @JsonCreator constructor(
    @JsonProperty("professorId") val professorId: UUID,
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: String,
    @JsonProperty("preferences") val preferences: List<PreferenceDetailsRequest>,
    @JsonProperty("isUpdate") val isUpdate: Boolean = false,
    @JsonProperty("previousVersion") val previousVersion: Int = 0
)

data class UpdatePreferencesRequest @JsonCreator constructor(
    @JsonProperty("submissionId") val submissionId: String,
    @JsonProperty("professorId") val professorId: UUID,
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: String,
    @JsonProperty("updatedPreferences") val updatedPreferences: List<PreferenceDetailsRequest>,
    @JsonProperty("updateReason") val updateReason: String? = null,
    @JsonProperty("expectedVersion") val expectedVersion: Int
)

data class WithdrawPreferencesRequest @JsonCreator constructor(
    @JsonProperty("submissionId") val submissionId: String,
    @JsonProperty("professorId") val professorId: UUID,
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: String,
    @JsonProperty("withdrawnBy") val withdrawnBy: String,
    @JsonProperty("withdrawalReason") val withdrawalReason: String
)

data class PreferenceDetailsRequest @JsonCreator constructor(
    @JsonProperty("courseId") val courseId: String,
    @JsonProperty("timePreferences") val timePreferences: List<TimeSlotPreferenceRequest>,
    @JsonProperty("roomPreferences") val roomPreferences: List<RoomPreferenceRequest> = emptyList(),
    @JsonProperty("durationPreference") val durationPreference: DurationPreferenceRequest? = null,
    @JsonProperty("specialRequirements") val specialRequirements: String? = null
)

data class TimeSlotPreferenceRequest @JsonCreator constructor(
    @JsonProperty("dayOfWeek") val dayOfWeek: Int,
    @JsonProperty("startTime") val startTime: String,
    @JsonProperty("endTime") val endTime: String,
    @JsonProperty("preferenceLevel") val preferenceLevel: String,
    @JsonProperty("reason") val reason: String? = null
)

data class RoomPreferenceRequest @JsonCreator constructor(
    @JsonProperty("roomId") val roomId: String,
    @JsonProperty("preferenceLevel") val preferenceLevel: String,
    @JsonProperty("reason") val reason: String? = null
)

data class DurationPreferenceRequest @JsonCreator constructor(
    @JsonProperty("preferredDurationMinutes") val preferredDurationMinutes: Int,
    @JsonProperty("minimumDurationMinutes") val minimumDurationMinutes: Int,
    @JsonProperty("maximumDurationMinutes") val maximumDurationMinutes: Int
)