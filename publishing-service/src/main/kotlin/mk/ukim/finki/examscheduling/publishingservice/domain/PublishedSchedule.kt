package mk.ukim.finki.examscheduling.publishingservice.domain

import jakarta.persistence.*
import mk.ukim.finki.examscheduling.publishingservice.domain.enums.PublicationStatus
import java.time.Instant
import java.util.*

@Entity
@Table(name = "published_schedules")
data class PublishedSchedule(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "schedule_id", nullable = false)
    val scheduleId: UUID,

    @Column(name = "exam_session_period_id", nullable = false)
    val examSessionPeriodId: String,

    @Column(name = "academic_year", nullable = false)
    val academicYear: String,

    @Column(name = "exam_session", nullable = false)
    val examSession: String,

    @Column(name = "title", nullable = false)
    val title: String,

    @Column(name = "description", length = 1000)
    val description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false)
    val publicationStatus: PublicationStatus = PublicationStatus.DRAFT,

    @Column(name = "published_at")
    val publishedAt: Instant? = null,

    @Column(name = "published_by")
    val publishedBy: String? = null,

    @Column(name = "is_public", nullable = false)
    val isPublic: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    val updatedAt: Instant? = null,

    @Version
    val version: Long = 0
)
