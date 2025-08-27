package mk.ukim.finki.examscheduling.publishingservice.repository

import mk.ukim.finki.examscheduling.publishingservice.domain.PublicationRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PublicationRecordRepository : JpaRepository<PublicationRecord, UUID> {

    fun findByPublishedScheduleIdOrderByTimestampDesc(publishedScheduleId: UUID): List<PublicationRecord>

    @Query("SELECT pr FROM PublicationRecord pr ORDER BY pr.timestamp DESC")
    fun findAllOrderByTimestampDesc(): List<PublicationRecord>

    @Query(
        """
        SELECT pr FROM PublicationRecord pr 
        WHERE pr.actionBy = :actionBy 
        ORDER BY pr.timestamp DESC
    """
    )
    fun findByActionByOrderByTimestampDesc(@Param("actionBy") actionBy: String): List<PublicationRecord>

    @Query(
        """
        SELECT 
            COUNT(*) as total_records,
            COUNT(CASE WHEN record_type = 'PUBLISHED' THEN 1 END) as published_records,
            COUNT(CASE WHEN record_type = 'UPDATED' THEN 1 END) as updated_records,
            COUNT(CASE WHEN record_type = 'WITHDRAWN' THEN 1 END) as withdrawn_records,
            COUNT(DISTINCT action_by) as unique_actors,
            COUNT(DISTINCT published_schedule_id) as unique_schedules
        FROM publication_records
    """, nativeQuery = true
    )
    fun getRecordStatistics(): Map<String, Any>
}