package mk.ukim.finki.examscheduling.preferencemanagement.repository

import mk.ukim.finki.examscheduling.preferencemanagement.domain.ProfessorPreference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ProfessorPreferenceRepository : JpaRepository<ProfessorPreference, UUID> {

    fun findByProfessorId(professorId: UUID): List<ProfessorPreference>

    @Query(
        """
        SELECT 
            COUNT(*) as total_preferences,
            COUNT(CASE WHEN status = 'SUBMITTED' THEN 1 END) as submitted_preferences,
            COUNT(CASE WHEN status = 'DRAFT' THEN 1 END) as draft_preferences,
            COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) as approved_preferences,
            COUNT(DISTINCT professor_id) as unique_professors,
            COUNT(DISTINCT academic_year) as unique_years
        FROM professor_preferences
    """, nativeQuery = true
    )
    fun getPreferenceStatistics(): Map<String, Any>
}