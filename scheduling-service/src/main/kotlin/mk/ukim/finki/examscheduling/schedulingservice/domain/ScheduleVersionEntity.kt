package mk.ukim.finki.examscheduling.schedulingservice.domain

import jakarta.persistence.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ScheduleStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ScheduleVersionType
import java.time.Instant
import java.util.*

@Entity
@Table(name = "schedule_versions")
data class ScheduleVersionEntity(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "schedule_id", nullable = false)
    val scheduleId: UUID,

    @Column(name = "version_number", nullable = false)
    val versionNumber: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "version_type", nullable = false)
    val versionType: ScheduleVersionType,

    @Column(name = "version_notes", length = 1000)
    val versionNotes: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_status", nullable = false)
    val scheduleStatus: ScheduleStatus,

    @Lob
    @Column(name = "snapshot_data", nullable = false)
    val snapshotData: String,

    @Column(name = "created_by", nullable = false)
    val createdBy: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Version
    val entityVersion: Long = 0
)