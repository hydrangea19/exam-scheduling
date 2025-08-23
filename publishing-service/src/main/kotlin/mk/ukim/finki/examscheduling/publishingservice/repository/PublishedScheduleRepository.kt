package mk.ukim.finki.examscheduling.publishingservice.repository

import mk.ukim.finki.examscheduling.publishingservice.domain.PublishedSchedule
import mk.ukim.finki.examscheduling.publishingservice.domain.enums.PublicationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PublishedScheduleRepository : JpaRepository<PublishedSchedule, UUID> {

    fun findByAcademicYear(academicYear: String): List<PublishedSchedule>

    fun findByPublicationStatus(status: PublicationStatus): List<PublishedSchedule>

    fun findByIsPublic(isPublic: Boolean): List<PublishedSchedule>

    @Query("SELECT ps FROM PublishedSchedule ps WHERE ps.isPublic = true AND ps.publicationStatus = 'PUBLISHED'")
    fun findPublicSchedules(): List<PublishedSchedule>

    fun countByPublicationStatus(status: PublicationStatus): Long

    @Query(
        """
        SELECT 
            COUNT(*) as total_schedules,
            COUNT(CASE WHEN publication_status = 'DRAFT' THEN 1 END) as draft_schedules,
            COUNT(CASE WHEN publication_status = 'PUBLISHED' THEN 1 END) as published_schedules,
            COUNT(CASE WHEN publication_status = 'ARCHIVED' THEN 1 END) as archived_schedules,
            COUNT(CASE WHEN publication_status = 'WITHDRAWN' THEN 1 END) as withdrawn_schedules,
            COUNT(CASE WHEN is_public = true THEN 1 END) as public_schedules,
            COUNT(DISTINCT academic_year) as unique_years
        FROM published_schedules
    """, nativeQuery = true
    )
    fun getPublicationStatistics(): Map<String, Any>
}