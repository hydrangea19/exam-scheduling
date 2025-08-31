package mk.ukim.finki.examscheduling.preferencemanagement.domain

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import mk.ukim.finki.examscheduling.preferencemanagement.domain.enums.ValidationRuleType
import mk.ukim.finki.examscheduling.preferencemanagement.domain.enums.ValidationSeverity
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.Instant

data class SubmitProfessorPreferenceCommand @JsonCreator constructor(
    @TargetAggregateIdentifier
    @JsonProperty("submissionId") val submissionId: SubmissionId,
    @JsonProperty("professorId") val professorId: ProfessorId,
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("preferences") val preferences: List<PreferenceDetails>,
    @JsonProperty("submissionTimestamp") val submissionTimestamp: Instant = Instant.now(),
    @JsonProperty("isUpdate") @JsonAlias("update") val isUpdate: Boolean = false,
    @JsonProperty("previousVersion") val previousVersion: Int = 0
) {
    init {
        require(preferences.isNotEmpty()) { "At least one preference must be specified" }
        require(previousVersion >= 0) { "Previous version cannot be negative" }
    }
}

data class OpenPreferenceSubmissionWindowCommand @JsonCreator constructor(
    @TargetAggregateIdentifier
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("academicYear") val academicYear: String,
    @JsonProperty("examSession") val examSession: String,
    @JsonProperty("openedBy") val openedBy: String,
    @JsonProperty("submissionDeadline") val submissionDeadline: Instant,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("openedAt") val openedAt: Instant = Instant.now()
) {
    init {
        require(academicYear.isNotBlank()) { "Academic year cannot be blank" }
        require(examSession.isNotBlank()) { "Exam session cannot be blank" }
        require(openedBy.isNotBlank()) { "Opened by cannot be blank" }
        require(submissionDeadline.isAfter(Instant.now())) { "Submission deadline must be in the future" }
        require(description == null || description.length <= 500) { "Description cannot exceed 500 characters" }
    }
}

data class ClosePreferenceSubmissionWindowCommand @JsonCreator constructor(
    @TargetAggregateIdentifier
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("closedBy") val closedBy: String,
    @JsonProperty("reason") val reason: String? = null,
    @JsonProperty("closedAt") val closedAt: Instant = Instant.now(),
    @JsonProperty("totalSubmissions") val totalSubmissions: Int = 0
) {
    init {
        require(closedBy.isNotBlank()) { "Closed by cannot be blank" }
        require(reason == null || reason.length <= 500) { "Reason cannot exceed 500 characters" }
        require(totalSubmissions >= 0) { "Total submissions cannot be negative" }
    }
}

data class UpdateProfessorPreferenceCommand @JsonCreator constructor(
    @TargetAggregateIdentifier
    @JsonProperty("submissionId") val submissionId: SubmissionId,
    @JsonProperty("professorId") val professorId: ProfessorId,
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("updatedPreferences") val updatedPreferences: List<PreferenceDetails>,
    @JsonProperty("updateTimestamp") val updateTimestamp: Instant = Instant.now(),
    @JsonProperty("updateReason") val updateReason: String? = null,
    @JsonProperty("expectedVersion") val expectedVersion: Int
) {
    init {
        require(updatedPreferences.isNotEmpty()) { "At least one preference must be specified" }
        require(updateReason == null || updateReason.length <= 300) { "Update reason cannot exceed 300 characters" }
        require(expectedVersion >= 0) { "Expected version cannot be negative" }
    }
}

data class ValidatePreferenceSubmissionCommand @JsonCreator constructor(
    @TargetAggregateIdentifier
    @JsonProperty("submissionId") val submissionId: SubmissionId,
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("validationRules") val validationRules: List<ValidationRule>,
    @JsonProperty("requestedBy") val requestedBy: String,
    @JsonProperty("validationTimestamp") val validationTimestamp: Instant = Instant.now()
) {
    init {
        require(validationRules.isNotEmpty()) { "At least one validation rule must be specified" }
        require(requestedBy.isNotBlank()) { "Requested by cannot be blank" }
    }
}

data class CreateExamSessionPeriodCommand @JsonCreator constructor(
    @TargetAggregateIdentifier
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("academicYear") val academicYear: String,
    @JsonProperty("examSession") val examSession: String,
    @JsonProperty("createdBy") val createdBy: String,
    @JsonProperty("plannedStartDate") val plannedStartDate: Instant,
    @JsonProperty("plannedEndDate") val plannedEndDate: Instant,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("createdAt") val createdAt: Instant = Instant.now()
) {
    init {
        require(academicYear.isNotBlank()) { "Academic year cannot be blank" }
        require(examSession.isNotBlank()) { "Exam session cannot be blank" }
        require(createdBy.isNotBlank()) { "Created by cannot be blank" }
        require(plannedStartDate.isBefore(plannedEndDate)) { "Start date must be before end date" }
        require(description == null || description.length <= 500) { "Description cannot exceed 500 characters" }
    }
}

data class WithdrawPreferenceSubmissionCommand @JsonCreator constructor(
    @TargetAggregateIdentifier
    @JsonProperty("submissionId") val submissionId: SubmissionId,
    @JsonProperty("professorId") val professorId: ProfessorId,
    @JsonProperty("examSessionPeriodId") val examSessionPeriodId: ExamSessionPeriodId,
    @JsonProperty("withdrawnBy") val withdrawnBy: String,
    @JsonProperty("withdrawalReason") val withdrawalReason: String,
    @JsonProperty("withdrawnAt") val withdrawnAt: Instant = Instant.now()
) {
    init {
        require(withdrawnBy.isNotBlank()) { "Withdrawn by cannot be blank" }
        require(withdrawalReason.isNotBlank()) { "Withdrawal reason cannot be blank" }
        require(withdrawalReason.length <= 500) { "Withdrawal reason cannot exceed 500 characters" }
    }
}

data class ValidationRule @JsonCreator constructor(
    @JsonProperty("ruleType") val ruleType: ValidationRuleType,
    @JsonProperty("parameters") val parameters: Map<String, Any> = emptyMap(),
    @JsonProperty("severity") val severity: ValidationSeverity = ValidationSeverity.ERROR
)



