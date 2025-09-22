package mk.ukim.finki.examscheduling.preferencemanagement.query.handler

import mk.ukim.finki.examscheduling.preferencemanagement.domain.*
import mk.ukim.finki.examscheduling.preferencemanagement.query.ExamSessionPeriodView
import mk.ukim.finki.examscheduling.preferencemanagement.query.PreferenceStatisticsView
import mk.ukim.finki.examscheduling.preferencemanagement.query.PreferenceSubmissionSummary
import mk.ukim.finki.examscheduling.preferencemanagement.query.repository.ExamSessionPeriodViewRepository
import mk.ukim.finki.examscheduling.preferencemanagement.query.repository.PreferenceStatisticsViewRepository
import mk.ukim.finki.examscheduling.preferencemanagement.query.repository.PreferenceSubmissionSummaryRepository
import org.axonframework.eventhandling.EventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class PreferenceSubmissionProjectionHandler(
    private val submissionRepository: PreferenceSubmissionSummaryRepository,
    private val sessionRepository: ExamSessionPeriodViewRepository,
    private val statisticsRepository: PreferenceStatisticsViewRepository
) {
    private val logger = LoggerFactory.getLogger(PreferenceSubmissionProjectionHandler::class.java)

    @EventHandler
    fun on(event: ExamSessionPeriodCreatedEvent) {
        logger.debug("Handling ExamSessionPeriodCreatedEvent: {}", event.examSessionPeriodId)

        val view = ExamSessionPeriodView(
            examSessionPeriodId = event.examSessionPeriodId.value,
            academicYear = event.academicYear,
            examSession = event.examSession,
            isWindowOpen = false,
            submissionDeadline = null,
            windowOpenedAt = null,
            windowClosedAt = null,
            createdAt = event.createdAt,
            description = event.description
        )

        sessionRepository.save(view)
    }

    @EventHandler
    fun on(event: PreferenceSubmissionWindowOpenedEvent) {
        logger.debug("Handling PreferenceSubmissionWindowOpenedEvent: {}", event.examSessionPeriodId)

        val existingView = sessionRepository.findById(event.examSessionPeriodId.value)
        if (existingView.isPresent) {
            val view = existingView.get().copy(
                isWindowOpen = true,
                submissionDeadline = event.submissionDeadline,
                windowOpenedAt = event.openedAt,
                windowClosedAt = null
            )
            sessionRepository.save(view)
        }
    }

    @EventHandler
    fun on(event: PreferenceSubmissionWindowClosedEvent) {
        logger.debug("Handling PreferenceSubmissionWindowClosedEvent: {}", event.examSessionPeriodId)

        val existingView = sessionRepository.findById(event.examSessionPeriodId.value)
        if (existingView.isPresent) {
            val view = existingView.get().copy(
                isWindowOpen = false,
                windowClosedAt = event.closedAt,
                totalSubmissions = event.totalSubmissions
            )
            sessionRepository.save(view)
        }
    }

    @EventHandler
    fun on(event: ProfessorPreferenceSubmittedEvent) {
        logger.debug("Handling ProfessorPreferenceSubmittedEvent: {}", event.submissionId)

        val summary = PreferenceSubmissionSummary(
            submissionId = event.submissionId.value.toString(),
            professorId = event.professorId.toString(),
            examSessionPeriodId = event.examSessionPeriodId.value,
            academicYear = extractAcademicYear(event.examSessionPeriodId.value),
            examSession = extractExamSession(event.examSessionPeriodId.value),
            status = "SUBMITTED",
            submissionVersion = event.submissionVersion,
            totalTimePreferences = event.getTotalTimePreferences(),
            totalRoomPreferences = event.getTotalRoomPreferences(),
            coursesCount = event.preferences.size,
            submittedAt = event.submittedAt,
            lastUpdatedAt = event.submittedAt,
            hasSpecialRequirements = event.getCoursesWithSpecialRequirements().isNotEmpty()
        )

        submissionRepository.save(summary)
        updateSessionStatistics(event.examSessionPeriodId.value)
        updatePreferenceStatistics(event.examSessionPeriodId.value, event.preferences)
    }

    @EventHandler
    fun on(event: ProfessorPreferenceUpdatedEvent) {
        logger.debug("Handling ProfessorPreferenceUpdatedEvent: {}", event.submissionId)

        val existingSummary = submissionRepository.findById(event.submissionId.value.toString())
        if (existingSummary.isPresent) {
            val summary = existingSummary.get().copy(
                submissionVersion = event.newVersion,
                totalTimePreferences = event.updatedPreferences.sumOf { it.timePreferences.size },
                totalRoomPreferences = event.updatedPreferences.sumOf { it.roomPreferences.size },
                coursesCount = event.updatedPreferences.size,
                lastUpdatedAt = event.updatedAt,
                hasSpecialRequirements = event.updatedPreferences.any { !it.specialRequirements.isNullOrBlank() }
            )
            submissionRepository.save(summary)
            updatePreferenceStatistics(event.examSessionPeriodId.value, event.updatedPreferences)
        }
    }

    @EventHandler
    fun on(event: ProfessorPreferenceWithdrawnEvent) {
        logger.debug("Handling ProfessorPreferenceWithdrawnEvent: {}", event.submissionId)

        val existingSummary = submissionRepository.findById(event.submissionId.value.toString())
        if (existingSummary.isPresent) {
            val summary = existingSummary.get().copy(
                status = "WITHDRAWN",
                lastUpdatedAt = event.withdrawnAt
            )
            submissionRepository.save(summary)
        }
    }

    @EventHandler
    fun on(event: PreferenceValidationFailedEvent) {
        logger.debug("Handling PreferenceValidationFailedEvent: {}", event.submissionId)

        val existingSummary = submissionRepository.findById(event.submissionId.value.toString())
        if (existingSummary.isPresent) {
            val summary = existingSummary.get().copy(
                hasValidationErrors = true,
                validationErrorsCount = event.validationErrors.size,
                status = "VALIDATION_FAILED"
            )
            submissionRepository.save(summary)
        }
    }

    private fun updateSessionStatistics(examSessionPeriodId: String) {
        val submissions = submissionRepository.findByExamSessionPeriodId(examSessionPeriodId)
        val uniqueProfessors = submissions.map { it.professorId }.toSet().size

        val existingView = sessionRepository.findById(examSessionPeriodId)
        if (existingView.isPresent) {
            val view = existingView.get().copy(
                totalSubmissions = submissions.size,
                uniqueProfessors = uniqueProfessors
            )
            sessionRepository.save(view)
        }
    }

    private fun updatePreferenceStatistics(examSessionPeriodId: String, preferences: List<PreferenceDetails>) {
        val existingStats = statisticsRepository.findByExamSessionPeriodId(examSessionPeriodId)
        statisticsRepository.deleteAll(existingStats)

        val timeSlotStats = mutableMapOf<String, MutableMap<String, Int>>()

        for (preference in preferences) {
            for (timePreference in preference.timePreferences) {
                val timeSlotKey =
                    "${timePreference.timeSlot.dayOfWeek}-${timePreference.timeSlot.startTime}-${timePreference.timeSlot.endTime}"
                val prefLevel = timePreference.preferenceLevel.name

                timeSlotStats
                    .computeIfAbsent(timeSlotKey) { mutableMapOf() }
                    .merge(prefLevel, 1) { old, new -> old + new }
            }
        }

        val statisticsToSave = mutableListOf<PreferenceStatisticsView>()

        for ((timeSlotKey, levelCounts) in timeSlotStats) {
            val parts = timeSlotKey.split("-")
            val dayOfWeek = parts[0].toInt()
            val startTime = parts[1]
            val endTime = parts[2]

            for ((level, count) in levelCounts) {
                val stat = PreferenceStatisticsView(
                    examSessionPeriodId = examSessionPeriodId,
                    timeSlotDay = dayOfWeek,
                    timeSlotStart = startTime,
                    timeSlotEnd = endTime,
                    preferenceLevel = level,
                    preferenceCount = count,
                    lastUpdated = Instant.now()
                )
                statisticsToSave.add(stat)
            }
        }

        statisticsRepository.saveAll(statisticsToSave)
    }

    private fun extractAcademicYear(examSessionPeriodId: String): String {
        val parts = examSessionPeriodId.split("_")
        return if (parts.isNotEmpty()) parts[0] else "Unknown"
    }

    private fun extractExamSession(examSessionPeriodId: String): String {
        val parts = examSessionPeriodId.split("_")
        return if (parts.size > 1) parts.drop(1).joinToString("_") else "Unknown"
    }
}