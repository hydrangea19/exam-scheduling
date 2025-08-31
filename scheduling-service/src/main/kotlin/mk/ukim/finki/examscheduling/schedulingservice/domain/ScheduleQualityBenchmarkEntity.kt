package mk.ukim.finki.examscheduling.schedulingservice.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "schedule_quality_benchmarks")
data class ScheduleQualityBenchmarkEntity(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "schedule_id", nullable = false)
    val scheduleId: UUID,

    @Column(name = "exam_session_period_id", nullable = false)
    val examSessionPeriodId: String,

    @Column(name = "benchmark_type", nullable = false)
    val benchmarkType: String,

    @Column(name = "quality_score", nullable = false)
    val qualityScore: Double,

    @Column(name = "preference_satisfaction_rate", nullable = false)
    val preferenceSatisfactionRate: Double,

    @Column(name = "conflict_resolution_rate", nullable = false)
    val conflictResolutionRate: Double,

    @Column(name = "room_utilization_efficiency", nullable = false)
    val roomUtilizationEfficiency: Double,

    @Column(name = "student_workload_balance", nullable = false)
    val studentWorkloadBalance: Double,

    @Column(name = "generation_time_ms", nullable = false)
    val generationTimeMs: Long,

    @Lob
    @Column(name = "detailed_metrics")
    val detailedMetrics: String? = null,

    @Column(name = "recorded_at", nullable = false)
    val recordedAt: Instant = Instant.now(),

    @Column(name = "recorded_by", nullable = false)
    val recordedBy: String,

    @Version
    val version: Long = 0
)