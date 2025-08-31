package mk.ukim.finki.examscheduling.schedulingservice.repository

import mk.ukim.finki.examscheduling.schedulingservice.domain.ScheduleQualityBenchmarkEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ScheduleQualityBenchmarkRepository : JpaRepository<ScheduleQualityBenchmarkEntity, UUID> {
    fun findByScheduleId(scheduleId: UUID): List<ScheduleQualityBenchmarkEntity>
    fun findByExamSessionPeriodId(examSessionPeriodId: String): List<ScheduleQualityBenchmarkEntity>
    fun findByBenchmarkTypeOrderByRecordedAtDesc(benchmarkType: String): List<ScheduleQualityBenchmarkEntity>
    fun findTopByOrderByQualityScoreDesc(): ScheduleQualityBenchmarkEntity?
}