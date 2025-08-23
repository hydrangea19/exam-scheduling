package mk.ukim.finki.examscheduling.preferencemanagement.repository

import mk.ukim.finki.examscheduling.preferencemanagement.domain.TimePreference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TimePreferenceRepository : JpaRepository<TimePreference, UUID> {

    fun findByPreferenceSubmissionId(preferenceSubmissionId: UUID): List<TimePreference>

    @Query(
        """
        SELECT 
            day_of_week,
            preference_level,
            COUNT(*) as preference_count,
            MIN(start_time) as earliest_start,
            MAX(end_time) as latest_end
        FROM time_preferences
        GROUP BY day_of_week, preference_level
        ORDER BY day_of_week, preference_level
    """, nativeQuery = true
    )
    fun getTimePreferenceStatistics(): List<Map<String, Any>>

    @Query(
        """
        SELECT 
            day_of_week,
            start_time,
            end_time,
            COUNT(*) as preference_count
        FROM time_preferences
        WHERE preference_level = 'PREFERRED'
        GROUP BY day_of_week, start_time, end_time
        HAVING COUNT(*) > 1
        ORDER BY preference_count DESC, day_of_week, start_time
    """, nativeQuery = true
    )
    fun findMostPreferredTimeSlots(): List<Map<String, Any>>
}