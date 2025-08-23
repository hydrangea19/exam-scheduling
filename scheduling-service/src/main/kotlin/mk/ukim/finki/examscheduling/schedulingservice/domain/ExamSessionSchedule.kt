package mk.ukim.finki.examscheduling.schedulingservice.domain

import jakarta.persistence.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ScheduleStatus
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "exam_session_schedules")
data class ExamSessionSchedule(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "exam_session_period_id", nullable = false, unique = true)
    val examSessionPeriodId: String,

    @Column(name = "academic_year", nullable = false)
    val academicYear: String,

    @Column(name = "exam_session", nullable = false)
    val examSession: String,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: ScheduleStatus = ScheduleStatus.DRAFT,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    val updatedAt: Instant? = null,

    @Column(name = "finalized_at")
    val finalizedAt: Instant? = null,

    @Column(name = "published_at")
    val publishedAt: Instant? = null,

    @Version
    val version: Long = 0,

    @OneToMany(mappedBy = "examSessionSchedule", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val scheduledExams: MutableSet<ScheduledExam> = mutableSetOf(),

    @OneToMany(mappedBy = "examSessionSchedule", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val professorComments: MutableSet<ProfessorComment> = mutableSetOf(),

    @OneToMany(mappedBy = "examSessionSchedule", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val adjustmentLogs: MutableSet<AdjustmentLog> = mutableSetOf()
)
