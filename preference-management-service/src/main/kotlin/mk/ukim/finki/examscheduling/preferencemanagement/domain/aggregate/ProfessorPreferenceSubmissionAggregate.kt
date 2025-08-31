package mk.ukim.finki.examscheduling.preferencemanagement.domain.aggregate

import mk.ukim.finki.examscheduling.preferencemanagement.domain.*
import mk.ukim.finki.examscheduling.preferencemanagement.domain.enums.ValidationRuleType
import mk.ukim.finki.examscheduling.preferencemanagement.domain.enums.ValidationSeverity
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import java.time.Instant

@Aggregate
class ProfessorPreferenceSubmissionAggregate {

    @AggregateIdentifier
    private lateinit var submissionId: SubmissionId

    private lateinit var professorId: ProfessorId
    private lateinit var examSessionPeriodId: ExamSessionPeriodId
    private var preferences: MutableList<PreferenceDetails> = mutableListOf()
    private var submissionVersion: Int = 0
    private var status: SubmissionStatus = SubmissionStatus.DRAFT
    private var submittedAt: Instant? = null
    private var lastUpdatedAt: Instant = Instant.now()
    private var createdAt: Instant = Instant.now()

    private var isWindowOpen: Boolean = false
    private var submissionDeadline: Instant? = null
    private var windowOpenedAt: Instant? = null
    private var windowClosedAt: Instant? = null

    private var lastValidationResult: ValidationResult? = null
    private var validationErrors: MutableList<ValidationError> = mutableListOf()

    private val changeLog: MutableList<ChangeLogEntry> = mutableListOf()

    private val logger = LoggerFactory.getLogger(ProfessorPreferenceSubmissionAggregate::class.java)

    constructor()


    @CommandHandler
    constructor(command: SubmitProfessorPreferenceCommand) {
        logger.info(
            "Creating new preference submission: {} for professor: {}",
            command.submissionId, command.professorId
        )

        require(command.preferences.isNotEmpty()) { "At least one preference must be specified" }

        val validationResult = performPreferenceValidation(command.preferences)

        if (validationResult.hasErrors()) {
            AggregateLifecycle.apply(
                PreferenceValidationFailedEvent(
                    submissionId = command.submissionId,
                    professorId = command.professorId,
                    examSessionPeriodId = command.examSessionPeriodId,
                    validationErrors = validationResult.errors,
                    validatedAt = Instant.now(),
                    attemptedSubmission = command.toString()
                )
            )
            return
        }

        if (validationResult.hasWarnings()) {
            AggregateLifecycle.apply(
                PreferenceValidationSucceededEvent(
                    submissionId = command.submissionId,
                    professorId = command.professorId,
                    examSessionPeriodId = command.examSessionPeriodId,
                    validationWarnings = validationResult.warnings
                )
            )
        }

        AggregateLifecycle.apply(
            ProfessorPreferenceSubmittedEvent(
                submissionId = command.submissionId,
                professorId = command.professorId,
                examSessionPeriodId = command.examSessionPeriodId,
                courseIds = command.preferences.map { it.courseId },
                preferences = command.preferences,
                submissionVersion = if (command.isUpdate) 2 else 1,
                submittedAt = command.submissionTimestamp,
                isUpdate = command.isUpdate
            )
        )
    }

    @CommandHandler
    fun handle(command: UpdateProfessorPreferenceCommand) {
        logger.info(
            "Updating preference submission: {} for professor: {}",
            command.submissionId, command.professorId
        )

        validateUpdatePreferences(command)

        require(command.expectedVersion == submissionVersion) {
            "Version conflict: expected ${command.expectedVersion}, current $submissionVersion"
        }

        val validationResult = performPreferenceValidation(command.updatedPreferences)

        if (validationResult.hasErrors()) {
            AggregateLifecycle.apply(
                PreferenceValidationFailedEvent(
                    submissionId = command.submissionId,
                    professorId = command.professorId,
                    examSessionPeriodId = command.examSessionPeriodId,
                    validationErrors = validationResult.errors,
                    validatedAt = Instant.now()
                )
            )
            return
        }

        AggregateLifecycle.apply(
            ProfessorPreferenceUpdatedEvent(
                submissionId = command.submissionId,
                professorId = command.professorId,
                examSessionPeriodId = command.examSessionPeriodId,
                updatedPreferences = command.updatedPreferences,
                previousVersion = submissionVersion,
                newVersion = submissionVersion + 1,
                updateReason = command.updateReason,
                updatedAt = command.updateTimestamp
            )
        )
    }

    @CommandHandler
    fun handle(command: WithdrawPreferenceSubmissionCommand) {
        logger.info(
            "Withdrawing preference submission: {} for professor: {}",
            command.submissionId, command.professorId
        )

        require(status != SubmissionStatus.WITHDRAWN) { "Submission is already withdrawn" }
        require(command.professorId == professorId) { "Only the submitting professor can withdraw their submission" }

        AggregateLifecycle.apply(
            ProfessorPreferenceWithdrawnEvent(
                submissionId = command.submissionId,
                professorId = command.professorId,
                examSessionPeriodId = command.examSessionPeriodId,
                withdrawnBy = command.withdrawnBy,
                withdrawalReason = command.withdrawalReason,
                withdrawnAt = command.withdrawnAt,
                finalVersion = submissionVersion
            )
        )
    }


    @EventSourcingHandler
    fun on(event: ProfessorPreferenceSubmittedEvent) {
        this.submissionId = event.submissionId
        this.professorId = event.professorId
        this.examSessionPeriodId = event.examSessionPeriodId
        this.preferences.clear()
        this.preferences.addAll(event.preferences)
        this.submissionVersion = event.submissionVersion
        this.status = SubmissionStatus.SUBMITTED
        this.submittedAt = event.submittedAt
        this.lastUpdatedAt = event.submittedAt
        this.createdAt = event.submittedAt

        val action = if (event.isUpdate) "UPDATED" else "SUBMITTED"
        addChangeLogEntry(action, event.professorId.toString(), "Preferences ${action.lowercase()}")

        logger.debug("Professor preferences submitted: {} by {}", event.submissionId, event.professorId)
    }

    @EventSourcingHandler
    fun on(event: ProfessorPreferenceUpdatedEvent) {
        this.preferences.clear()
        this.preferences.addAll(event.updatedPreferences)
        this.submissionVersion = event.newVersion
        this.lastUpdatedAt = event.updatedAt

        addChangeLogEntry(
            "UPDATED", event.professorId.toString(),
            "Preferences updated: ${event.updateReason ?: "No reason provided"}"
        )

        logger.debug("Professor preferences updated: {} version {}", event.submissionId, event.newVersion)
    }

    @EventSourcingHandler
    fun on(event: ProfessorPreferenceWithdrawnEvent) {
        this.status = SubmissionStatus.WITHDRAWN
        this.lastUpdatedAt = event.withdrawnAt

        addChangeLogEntry("WITHDRAWN", event.withdrawnBy, event.withdrawalReason)

        logger.debug("Professor preferences withdrawn: {}", event.submissionId)
    }

    @EventSourcingHandler
    fun on(event: PreferenceValidationFailedEvent) {
        this.validationErrors.clear()
        this.validationErrors.addAll(event.validationErrors)
        this.lastValidationResult = ValidationResult(event.validationErrors, emptyList())

        addChangeLogEntry("VALIDATION_FAILED", "SYSTEM", "Validation failed with ${event.validationErrors.size} errors")

        logger.debug(
            "Preference validation failed for: {} with {} errors",
            event.submissionId,
            event.validationErrors.size
        )
    }

    @EventSourcingHandler
    fun on(event: PreferenceValidationSucceededEvent) {
        this.validationErrors.clear()
        this.lastValidationResult = ValidationResult(emptyList(), event.validationWarnings)

        val warningCount = event.validationWarnings.size
        val message = if (warningCount > 0) "Validation passed with $warningCount warnings" else "Validation passed"
        addChangeLogEntry("VALIDATION_SUCCEEDED", "SYSTEM", message)

        logger.debug("Preference validation succeeded for: {}", event.submissionId)
    }


    private fun validateSubmitPreferences(command: SubmitProfessorPreferenceCommand) {
        require(command.preferences.isNotEmpty()) { "At least one preference must be specified" }
    }

    private fun validateUpdatePreferences(command: UpdateProfessorPreferenceCommand) {
        require(this::submissionId.isInitialized) { "No existing submission found" }
        require(status != SubmissionStatus.WITHDRAWN) { "Cannot update withdrawn submission" }
        require(command.professorId == professorId) { "Only the original professor can update this submission" }
    }

    private fun performPreferenceValidation(preferences: List<PreferenceDetails>): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationError>()

        for (preference in preferences) {
            if (preference.hasConflictingTimePreferences()) {
                errors.add(
                    ValidationError(
                        ruleType = ValidationRuleType.NO_TIME_CONFLICTS,
                        severity = ValidationSeverity.ERROR,
                        message = "Time preferences for course ${preference.courseId} have conflicts",
                        affectedCourseId = preference.courseId
                    )
                )
            }

            if (preference.timePreferences.isEmpty()) {
                errors.add(
                    ValidationError(
                        ruleType = ValidationRuleType.MINIMUM_PREFERENCES_COUNT,
                        severity = ValidationSeverity.ERROR,
                        message = "At least one time preference must be specified for course ${preference.courseId}",
                        affectedCourseId = preference.courseId
                    )
                )
            }

            val nonBusinessHourPreferences = preference.timePreferences.filter {
                !isWithinBusinessHours(it.timeSlot)
            }
            if (nonBusinessHourPreferences.isNotEmpty()) {
                warnings.add(
                    ValidationError(
                        ruleType = ValidationRuleType.BUSINESS_HOURS_ONLY,
                        severity = ValidationSeverity.WARNING,
                        message = "Some preferences for course ${preference.courseId} are outside business hours",
                        affectedCourseId = preference.courseId,
                        suggestedFix = "Consider scheduling within 8:00 AM - 8:00 PM"
                    )
                )
            }
        }

        return ValidationResult(errors, warnings)
    }

    private fun isWithinBusinessHours(timeSlot: TimeSlot): Boolean {
        val businessStart = java.time.LocalTime.of(8, 0)
        val businessEnd = java.time.LocalTime.of(20, 0)
        return timeSlot.startTime.isAfter(businessStart) && timeSlot.endTime.isBefore(businessEnd)
    }

    private fun addChangeLogEntry(action: String, performedBy: String, details: String) {
        changeLog.add(
            ChangeLogEntry(
                action = action,
                performedBy = performedBy,
                details = details,
                timestamp = Instant.now()
            )
        )
    }


    data class ValidationResult(
        val errors: List<ValidationError>,
        val warnings: List<ValidationError>
    ) {
        fun hasErrors(): Boolean = errors.isNotEmpty()
        fun hasWarnings(): Boolean = warnings.isNotEmpty()
        fun isValid(): Boolean = errors.isEmpty()
    }

    data class ChangeLogEntry(
        val action: String,
        val performedBy: String,
        val details: String,
        val timestamp: Instant
    )

    enum class SubmissionStatus {
        DRAFT,
        SUBMITTED,
        VALIDATED,
        WITHDRAWN,
        PROCESSED
    }
}