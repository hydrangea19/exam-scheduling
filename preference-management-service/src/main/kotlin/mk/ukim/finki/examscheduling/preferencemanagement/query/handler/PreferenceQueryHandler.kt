package mk.ukim.finki.examscheduling.preferencemanagement.query.handler

import mk.ukim.finki.examscheduling.preferencemanagement.domain.ExamSessionPeriodId
import mk.ukim.finki.examscheduling.preferencemanagement.query.*
import mk.ukim.finki.examscheduling.preferencemanagement.query.repository.ExamSessionPeriodViewRepository
import mk.ukim.finki.examscheduling.preferencemanagement.query.repository.PreferenceStatisticsViewRepository
import mk.ukim.finki.examscheduling.preferencemanagement.query.repository.PreferenceSubmissionSummaryRepository
import org.axonframework.queryhandling.QueryHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PreferenceQueryHandler(
    private val submissionRepository: PreferenceSubmissionSummaryRepository,
    private val sessionRepository: ExamSessionPeriodViewRepository,
    private val statisticsRepository: PreferenceStatisticsViewRepository
) {
    private val logger = LoggerFactory.getLogger(PreferenceQueryHandler::class.java)

    @QueryHandler
    fun handle(query: FindPreferencesByProfessorQuery): List<PreferenceSubmissionSummary> {
        logger.debug("Handling FindPreferencesByProfessorQuery for professor: {}", query.professorId)

        return if (query.examSessionPeriodId != null) {
            val result = submissionRepository.findByProfessorIdAndExamSessionPeriodId(
                query.professorId.value.toString(),
                query.examSessionPeriodId.value
            )
            listOfNotNull(result)
        } else {
            submissionRepository.findByProfessorId(query.professorId.value.toString())
        }
    }

    @QueryHandler
    fun handle(query: FindPreferencesBySessionQuery): List<PreferenceSubmissionSummary> {
        logger.debug("Handling FindPreferencesBySessionQuery for session: {}", query.examSessionPeriodId)

        val sessionId = ExamSessionPeriodId(query.examSessionPeriodId)
        return submissionRepository.findByExamSessionPeriodId(sessionId.value)
    }

    @QueryHandler
    fun handle(query: GetPreferenceStatisticsQuery): List<PreferenceStatisticsView> {
        logger.debug("Handling GetPreferenceStatisticsQuery for session: {}", query.examSessionPeriodId)

        return statisticsRepository.findByExamSessionPeriodId(query.examSessionPeriodId.value)
    }

    @QueryHandler
    fun handle(query: GetExamSessionPeriodsQuery): List<ExamSessionPeriodView> {
        logger.debug("Handling GetExamSessionPeriodsQuery")

        return sessionRepository.findAll().sortedByDescending { it.createdAt }.toList()
    }

    @QueryHandler
    fun handle(query: FindConflictingPreferencesQuery): List<PreferenceStatisticsView> {
        logger.debug("Handling FindConflictingPreferencesQuery for session: {}", query.examSessionPeriodId)

        return statisticsRepository.findConflictingTimeSlots(query.examSessionPeriodId.value)
    }
}