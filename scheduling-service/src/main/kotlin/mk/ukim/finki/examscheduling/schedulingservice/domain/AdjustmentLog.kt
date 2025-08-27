package mk.ukim.finki.examscheduling.schedulingservice.domain

import jakarta.persistence.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.AdjustmentStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.AdjustmentType
import java.time.Instant
import java.util.*

@Entity
@Table(name = "adjustment_logs")
data class AdjustmentLog(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "adjustment_id", nullable = false)
    val adjustmentId: String,

    @Column(name = "admin_id", nullable = false)
    val adminId: String,

    @Column(name = "comment_id")
    val commentId: String? = null,

    @Column(name = "scheduled_exam_id")
    val scheduledExamId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false)
    val adjustmentType: AdjustmentType,

    @Column(name = "description", nullable = false, length = 2000)
    val description: String,

    @Column(name = "old_values", length = 1000)
    val oldValues: String? = null,

    @Column(name = "new_values", length = 1000)
    val newValues: String? = null,

    @Column(name = "reason", length = 1000)
    val reason: String? = null,

    @Column(name = "timestamp", nullable = false)
    val timestamp: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: AdjustmentStatus = AdjustmentStatus.APPLIED,

    @Version
    val version: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_schedule_id", nullable = false)
    val examSessionSchedule: ExamSessionSchedule
)
