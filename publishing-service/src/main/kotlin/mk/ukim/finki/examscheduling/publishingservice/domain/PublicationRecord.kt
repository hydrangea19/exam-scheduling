package mk.ukim.finki.examscheduling.publishingservice.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "publication_records")
data class PublicationRecord(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "published_schedule_id", nullable = false)
    val publishedScheduleId: UUID,

    @Column(name = "record_type", nullable = false)
    val recordType: String,

    @Column(name = "action_by", nullable = false)
    val actionBy: String,

    @Column(name = "action_description", length = 500)
    val actionDescription: String? = null,

    @Column(name = "metadata", length = 1000)
    val metadata: String? = null,

    @Column(name = "timestamp", nullable = false)
    val timestamp: Instant = Instant.now(),

    @Version
    val version: Long = 0
)
