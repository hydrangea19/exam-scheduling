package mk.ukim.finki.examscheduling.schedulingservice.domain

import jakarta.persistence.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.CommentStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.CommentType
import java.time.Instant
import java.util.*

@Entity
@Table(name = "professor_comments")
data class ProfessorComment(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "comment_id", nullable = false)
    val commentId: String,

    @Column(name = "professor_id", nullable = false)
    val professorId: String,

    @Column(name = "scheduled_exam_id")
    val scheduledExamId: String?,

    @Column(name = "comment_text", nullable = false, length = 2000)
    val commentText: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", nullable = false)
    val commentType: CommentType,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: CommentStatus = CommentStatus.SUBMITTED,

    @Column(name = "submitted_at", nullable = false)
    val submittedAt: Instant = Instant.now(),

    @Column(name = "reviewed_at")
    val reviewedAt: Instant? = null,

    @Column(name = "reviewed_by")
    val reviewedBy: String? = null,

    @Version
    val version: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_schedule_id", nullable = false)
    val examSessionSchedule: ExamSessionSchedule
)
