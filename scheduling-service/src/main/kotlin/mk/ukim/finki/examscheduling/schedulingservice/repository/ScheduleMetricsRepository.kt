package mk.ukim.finki.examscheduling.schedulingservice.repository

import mk.ukim.finki.examscheduling.schedulingservice.domain.ScheduleMetricsEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ScheduleMetricsRepository : JpaRepository<ScheduleMetricsEntity, UUID> {
    fun findByScheduleId(scheduleId: UUID): ScheduleMetricsEntity?
    fun findByScheduleIdOrderByRecordedAtDesc(scheduleId: UUID): List<ScheduleMetricsEntity>
    fun findByQualityScoreGreaterThanOrderByQualityScoreDesc(minScore: Double): List<ScheduleMetricsEntity>
}