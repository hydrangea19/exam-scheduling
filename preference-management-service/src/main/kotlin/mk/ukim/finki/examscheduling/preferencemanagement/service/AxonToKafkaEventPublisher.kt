package mk.ukim.finki.examscheduling.preferencemanagement.service

import mk.ukim.finki.examscheduling.preferencemanagement.domain.*
import org.axonframework.eventhandling.EventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AxonToKafkaEventPublisher(
    private val eventPublisher: EventPublisher
) {
    private val logger = LoggerFactory.getLogger(AxonToKafkaEventPublisher::class.java)

    @EventHandler
    fun handle(event: ExamSessionPeriodCreatedEvent) {
        logger.info("Publishing ExamSessionPeriodCreatedEvent to Kafka: {}", event.examSessionPeriodId)

        val kafkaEvent = mapOf(
            "eventType" to "ExamSessionPeriodCreated",
            "examSessionPeriodId" to event.examSessionPeriodId.value,
            "academicYear" to event.academicYear,
            "examSession" to event.examSession,
            "createdBy" to event.createdBy,
            "plannedStartDate" to event.plannedStartDate,
            "plannedEndDate" to event.plannedEndDate,
            "description" to event.description,
            "timestamp" to event.timestamp,
            "serviceName" to event.serviceName
        )

        eventPublisher.publishPreferenceEvent(kafkaEvent, event.examSessionPeriodId.value)

        val notification = mapOf(
            "notificationType" to "SESSION_PERIOD_CREATED",
            "message" to "New exam session period created: ${event.academicYear} ${event.examSession}",
            "targetService" to "scheduling-service",
            "metadata" to mapOf(
                "examSessionPeriodId" to event.examSessionPeriodId.value,
                "academicYear" to event.academicYear,
                "examSession" to event.examSession
            )
        )

        eventPublisher.publishSystemNotification(notification, event.examSessionPeriodId.value)
    }

    @EventHandler
    fun handle(event: PreferenceSubmissionWindowOpenedEvent) {
        logger.info("Publishing PreferenceSubmissionWindowOpenedEvent to Kafka: {}", event.examSessionPeriodId)

        val kafkaEvent = mapOf(
            "eventType" to "PreferenceSubmissionWindowOpened",
            "examSessionPeriodId" to event.examSessionPeriodId.value,
            "academicYear" to event.academicYear,
            "examSession" to event.examSession,
            "openedBy" to event.openedBy,
            "submissionDeadline" to event.submissionDeadline,
            "description" to event.description,
            "timestamp" to event.timestamp,
            "serviceName" to event.serviceName
        )

        eventPublisher.publishPreferenceEvent(kafkaEvent, event.examSessionPeriodId.value)

        val notification = mapOf(
            "notificationType" to "PREFERENCE_WINDOW_OPENED",
            "message" to "Preference submission window opened for ${event.academicYear} ${event.examSession}",
            "targetService" to "user-management-service",
            "metadata" to mapOf(
                "examSessionPeriodId" to event.examSessionPeriodId.value,
                "submissionDeadline" to event.submissionDeadline,
                "academicYear" to event.academicYear
            )
        )

        eventPublisher.publishSystemNotification(notification, event.examSessionPeriodId.value)
    }

    @EventHandler
    fun handle(event: PreferenceSubmissionWindowClosedEvent) {
        logger.info("Publishing PreferenceSubmissionWindowClosedEvent to Kafka: {}", event.examSessionPeriodId)

        val kafkaEvent = mapOf(
            "eventType" to "PreferenceSubmissionWindowClosed",
            "examSessionPeriodId" to event.examSessionPeriodId.value,
            "closedBy" to event.closedBy,
            "reason" to event.reason,
            "totalSubmissions" to event.totalSubmissions,
            "timestamp" to event.timestamp,
            "serviceName" to event.serviceName
        )

        eventPublisher.publishPreferenceEvent(kafkaEvent, event.examSessionPeriodId.value)

        val notification = mapOf(
            "notificationType" to "PREFERENCE_WINDOW_CLOSED",
            "message" to "Preference submission window closed. Ready for schedule generation.",
            "targetService" to "scheduling-service",
            "metadata" to mapOf(
                "examSessionPeriodId" to event.examSessionPeriodId.value,
                "totalSubmissions" to event.totalSubmissions,
                "reason" to event.reason
            )
        )

        eventPublisher.publishSystemNotification(notification, event.examSessionPeriodId.value)
    }

    @EventHandler
    fun handle(event: ProfessorPreferenceSubmittedEvent) {
        logger.info("Publishing ProfessorPreferenceSubmittedEvent to Kafka: {}", event.submissionId)

        val kafkaEvent = mapOf(
            "eventType" to "ProfessorPreferenceSubmitted",
            "submissionId" to event.submissionId.value.toString(),
            "professorId" to event.professorId.value.toString(),
            "examSessionPeriodId" to event.examSessionPeriodId.value,
            "courseIds" to event.courseIds.map { it.value },
            "submissionVersion" to event.submissionVersion,
            "totalTimePreferences" to event.getTotalTimePreferences(),
            "totalRoomPreferences" to event.getTotalRoomPreferences(),
            "isUpdate" to event.isUpdate,
            "timestamp" to event.timestamp,
            "serviceName" to event.serviceName
        )

        eventPublisher.publishPreferenceEvent(kafkaEvent, event.examSessionPeriodId.value)

        val auditEvent = mapOf(
            "action" to "PREFERENCE_SUBMITTED",
            "performedBy" to event.professorId.value.toString(),
            "entityType" to "ProfessorPreferenceSubmission",
            "entityId" to event.submissionId.value.toString(),
            "changeDetails" to mapOf(
                "examSessionPeriodId" to event.examSessionPeriodId.value,
                "courseCount" to event.courseIds.size,
                "preferenceVersion" to event.submissionVersion,
                "isUpdate" to event.isUpdate
            ),
            "performedAt" to event.submittedAt
        )

        eventPublisher.publishEvent("audit-events", auditEvent, event.submissionId.value.toString())
    }

    @EventHandler
    fun handle(event: ProfessorPreferenceUpdatedEvent) {
        logger.info("Publishing ProfessorPreferenceUpdatedEvent to Kafka: {}", event.submissionId)

        val kafkaEvent = mapOf(
            "eventType" to "ProfessorPreferenceUpdated",
            "submissionId" to event.submissionId.value.toString(),
            "professorId" to event.professorId.value.toString(),
            "examSessionPeriodId" to event.examSessionPeriodId.value,
            "previousVersion" to event.previousVersion,
            "newVersion" to event.newVersion,
            "updateReason" to event.updateReason,
            "timestamp" to event.timestamp,
            "serviceName" to event.serviceName
        )

        eventPublisher.publishPreferenceEvent(kafkaEvent, event.examSessionPeriodId.value)

        val auditEvent = mapOf(
            "action" to "PREFERENCE_UPDATED",
            "performedBy" to event.professorId.value.toString(),
            "entityType" to "ProfessorPreferenceSubmission",
            "entityId" to event.submissionId.value.toString(),
            "changeDetails" to mapOf(
                "previousVersion" to event.previousVersion,
                "newVersion" to event.newVersion,
                "updateReason" to event.updateReason
            ),
            "performedAt" to event.updatedAt
        )

        eventPublisher.publishEvent("audit-events", auditEvent, event.submissionId.value.toString())
    }

    @EventHandler
    fun handle(event: ProfessorPreferenceWithdrawnEvent) {
        logger.info("Publishing ProfessorPreferenceWithdrawnEvent to Kafka: {}", event.submissionId)

        val kafkaEvent = mapOf(
            "eventType" to "ProfessorPreferenceWithdrawn",
            "submissionId" to event.submissionId.value.toString(),
            "professorId" to event.professorId.value.toString(),
            "examSessionPeriodId" to event.examSessionPeriodId.value,
            "withdrawnBy" to event.withdrawnBy,
            "withdrawalReason" to event.withdrawalReason,
            "finalVersion" to event.finalVersion,
            "timestamp" to event.timestamp,
            "serviceName" to event.serviceName
        )

        eventPublisher.publishPreferenceEvent(kafkaEvent, event.examSessionPeriodId.value)

        val auditEvent = mapOf(
            "action" to "PREFERENCE_WITHDRAWN",
            "performedBy" to event.withdrawnBy,
            "entityType" to "ProfessorPreferenceSubmission",
            "entityId" to event.submissionId.value.toString(),
            "changeDetails" to mapOf(
                "withdrawalReason" to event.withdrawalReason,
                "finalVersion" to event.finalVersion
            ),
            "performedAt" to event.withdrawnAt
        )

        eventPublisher.publishEvent("audit-events", auditEvent, event.submissionId.value.toString())
    }

    @EventHandler
    fun handle(event: PreferenceValidationFailedEvent) {
        logger.warn("Publishing PreferenceValidationFailedEvent to Kafka: {}", event.submissionId)

        val kafkaEvent = mapOf(
            "eventType" to "PreferenceValidationFailed",
            "submissionId" to event.submissionId.value.toString(),
            "professorId" to event.professorId.value.toString(),
            "examSessionPeriodId" to event.examSessionPeriodId.value,
            "validationErrors" to event.validationErrors.map { error ->
                mapOf(
                    "ruleType" to error.ruleType.name,
                    "severity" to error.severity.name,
                    "message" to error.message,
                    "affectedCourseId" to error.affectedCourseId?.value,
                    "suggestedFix" to error.suggestedFix
                )
            },
            "errorCount" to event.validationErrors.size,
            "timestamp" to event.timestamp,
            "serviceName" to event.serviceName
        )

        eventPublisher.publishPreferenceEvent(kafkaEvent, event.examSessionPeriodId.value)

        val notification = mapOf(
            "notificationType" to "PREFERENCE_VALIDATION_FAILED",
            "message" to "Preference validation failed with ${event.validationErrors.size} errors",
            "targetService" to "user-management-service",
            "metadata" to mapOf(
                "professorId" to event.professorId.value.toString(),
                "submissionId" to event.submissionId.value.toString(),
                "errorCount" to event.validationErrors.size,
                "errors" to event.validationErrors.map { it.message }
            )
        )

        eventPublisher.publishSystemNotification(notification, event.professorId.value.toString())
    }

    @EventHandler
    fun handle(event: PreferenceValidationSucceededEvent) {
        logger.info("Publishing PreferenceValidationSucceededEvent to Kafka: {}", event.submissionId)

        val kafkaEvent = mapOf(
            "eventType" to "PreferenceValidationSucceeded",
            "submissionId" to event.submissionId.value.toString(),
            "professorId" to event.professorId.value.toString(),
            "examSessionPeriodId" to event.examSessionPeriodId.value,
            "validationWarnings" to event.validationWarnings.map { warning ->
                mapOf(
                    "ruleType" to warning.ruleType.name,
                    "severity" to warning.severity.name,
                    "message" to warning.message,
                    "suggestedFix" to warning.suggestedFix
                )
            },
            "warningCount" to event.validationWarnings.size,
            "timestamp" to event.timestamp,
            "serviceName" to event.serviceName
        )

        eventPublisher.publishPreferenceEvent(kafkaEvent, event.examSessionPeriodId.value)

        if (event.validationWarnings.isNotEmpty()) {
            val notification = mapOf(
                "notificationType" to "PREFERENCE_VALIDATION_WARNINGS",
                "message" to "Preference validation passed with ${event.validationWarnings.size} warnings",
                "targetService" to "user-management-service",
                "metadata" to mapOf(
                    "professorId" to event.professorId.value.toString(),
                    "submissionId" to event.submissionId.value.toString(),
                    "warningCount" to event.validationWarnings.size,
                    "warnings" to event.validationWarnings.map { it.message }
                )
            )

            eventPublisher.publishSystemNotification(notification, event.professorId.value.toString())
        }
    }
}