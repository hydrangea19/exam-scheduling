package mk.ukim.finki.examscheduling.schedulingservice.repository

import mk.ukim.finki.examscheduling.schedulingservice.domain.AdjustmentLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AdjustmentLogRepository : JpaRepository<AdjustmentLog, UUID> {

    fun findByExamSessionScheduleId(examSessionScheduleId: UUID): List<AdjustmentLog>

    @Query(
        """
        SELECT 
            COUNT(*) as total_adjustments,
            COUNT(CASE WHEN status = 'APPLIED' THEN 1 END) as applied_adjustments,
            COUNT(CASE WHEN status = 'REVERTED' THEN 1 END) as reverted_adjustments,
            COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_adjustments,
            COUNT(DISTINCT admin_id) as unique_admins,
            COUNT(DISTINCT scheduled_exam_id) as affected_exams
        FROM adjustment_logs
    """, nativeQuery = true
    )
    fun getAdjustmentStatistics(): Map<String, Any>
}