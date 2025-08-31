package mk.ukim.finki.examscheduling.schedulingservice.domain

import jakarta.persistence.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ConflictSeverity
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ConflictStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ConflictType
import java.time.Instant
import java.util.*

@Entity
@Table(name = "schedule_conflicts")
data class ScheduleConflictEntity(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "schedule_id", nullable = false)
    val scheduleId: UUID,

    @Column(name = "conflict_id", nullable = false, unique = true)
    val conflictId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_type", nullable = false)
    val conflictType: ConflictType,

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    val severity: ConflictSeverity,

    @Column(name = "description", nullable = false, length = 1000)
    val description: String,

    @ElementCollection
    @CollectionTable(name = "conflict_affected_exams", joinColumns = [JoinColumn(name = "conflict_id")])
    @Column(name = "exam_id")
    val affectedExamIds: MutableSet<String> = mutableSetOf(),

    @Column(name = "affected_students", nullable = false)
    val affectedStudents: Int = 0,

    @Column(name = "suggested_resolution", length = 1000)
    val suggestedResolution: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: ConflictStatus = ConflictStatus.DETECTED,

    @Column(name = "detected_at", nullable = false)
    val detectedAt: Instant = Instant.now(),

    @Column(name = "resolved_at")
    val resolvedAt: Instant? = null,

    @Column(name = "resolved_by")
    val resolvedBy: String? = null,

    @Version
    val version: Long = 0
)