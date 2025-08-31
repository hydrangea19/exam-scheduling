package mk.ukim.finki.examscheduling.preferencemanagement.query.repository

import mk.ukim.finki.examscheduling.preferencemanagement.query.PreferenceStatisticsView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PreferenceStatisticsViewRepository : JpaRepository<PreferenceStatisticsView, String> {
    fun findByExamSessionPeriodId(examSessionPeriodId: String): List<PreferenceStatisticsView>

    @Query("SELECT p FROM PreferenceStatisticsView p WHERE p.examSessionPeriodId = :sessionId ORDER BY p.preferenceCount DESC")
    fun findMostPopularTimeSlots(@Param("sessionId") sessionId: String): List<PreferenceStatisticsView>

    @Query("SELECT p FROM PreferenceStatisticsView p WHERE p.examSessionPeriodId = :sessionId AND p.conflictingPreferences > 0")
    fun findConflictingTimeSlots(@Param("sessionId") sessionId: String): List<PreferenceStatisticsView>
}