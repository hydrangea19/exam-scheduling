package mk.ukim.finki.examscheduling.preferencemanagement.domain

import jakarta.persistence.*
import mk.ukim.finki.examscheduling.preferencemanagement.domain.enums.PreferenceLevel
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "time_preferences",
    indexes = [
        Index(name = "idx_time_preferences_submission_id", columnList = "preference_submission_id"),
        Index(name = "idx_time_preferences_day", columnList = "day_of_week"),
        Index(name = "idx_time_preferences_level", columnList = "preference_level")
    ]
)
data class TimePreference(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preference_submission_id", nullable = false)
    val preferenceSubmission: ProfessorPreference,

    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: Int,

    @Column(name = "start_time", nullable = false)
    val startTime: java.time.LocalTime,

    @Column(name = "end_time", nullable = false)
    val endTime: java.time.LocalTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_level", nullable = false)
    val preferenceLevel: PreferenceLevel,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    constructor() : this(
        id = UUID.randomUUID(),
        preferenceSubmission = ProfessorPreference(),
        dayOfWeek = 1,
        startTime = java.time.LocalTime.of(9, 0),
        endTime = java.time.LocalTime.of(11, 0),
        preferenceLevel = PreferenceLevel.PREFERRED
    )

    fun getDayName(): String {
        return when (dayOfWeek) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Unknown"
        }
    }

    fun getTimeSlotDisplay(): String = "$startTime - $endTime"

    fun getFullDisplay(): String = "${getDayName()} ${getTimeSlotDisplay()} (${preferenceLevel.displayName})"

    fun overlaps(other: TimePreference): Boolean {
        return this.dayOfWeek == other.dayOfWeek &&
                this.startTime.isBefore(other.endTime) &&
                this.endTime.isAfter(other.startTime)
    }

    override fun toString(): String {
        return "TimePreference(id=$id, day=${getDayName()}, time=${getTimeSlotDisplay()}, level=${preferenceLevel})"
    }
}
