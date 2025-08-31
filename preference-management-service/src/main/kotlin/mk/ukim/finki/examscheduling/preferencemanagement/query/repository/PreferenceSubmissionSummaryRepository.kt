package mk.ukim.finki.examscheduling.preferencemanagement.query.repository

import mk.ukim.finki.examscheduling.preferencemanagement.query.PreferenceSubmissionSummary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PreferenceSubmissionSummaryRepository : JpaRepository<PreferenceSubmissionSummary, String> {
    fun findByProfessorId(professorId: String): List<PreferenceSubmissionSummary>
    fun findByExamSessionPeriodId(examSessionPeriodId: String): List<PreferenceSubmissionSummary>
    fun findByProfessorIdAndExamSessionPeriodId(
        professorId: String,
        examSessionPeriodId: String
    ): PreferenceSubmissionSummary?

    @Query("SELECT COUNT(*) FROM PreferenceSubmissionSummary p WHERE p.examSessionPeriodId = :sessionId AND p.hasValidationErrors = true")
    fun countSubmissionsWithErrors(@Param("sessionId") sessionId: String): Long
}