package mk.ukim.finki.examscheduling.schedulingservice.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "schedule_metrics")
data class ScheduleMetricsEntity(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "schedule_id", nullable = false)
    val scheduleId: UUID,

    @Column(name = "quality_score", nullable = false)
    val qualityScore: Double,

    @Column(name = "final_quality_score")
    val finalQualityScore: Double? = null,

    @Column(name = "preference_satisfaction_rate", nullable = false)
    val preferenceSatisfactionRate: Double,

    @Column(name = "total_conflicts", nullable = false)
    val totalConflicts: Int,

    @Column(name = "resolved_conflicts", nullable = false)
    val resolvedConflicts: Int,

    @Column(name = "room_utilization_rate", nullable = false)
    val roomUtilizationRate: Double,

    @Column(name = "average_student_exams_per_day", nullable = false)
    val averageStudentExamsPerDay: Double,

    @Column(name = "total_courses_scheduled", nullable = false)
    val totalCoursesScheduled: Int,

    @Column(name = "total_professor_preferences_considered", nullable = false)
    val totalProfessorPreferencesConsidered: Int,

    @Column(name = "preferences_satisfied", nullable = false)
    val preferencesSatisfied: Int,

    @Column(name = "processing_time_ms", nullable = false)
    val processingTimeMs: Long,

    @Column(name = "recorded_at", nullable = false)
    val recordedAt: Instant = Instant.now(),

    @Column(name = "finalized_at")
    val finalizedAt: Instant? = null,

    @Version
    val version: Long = 0
)