package mk.ukim.finki.examscheduling.preferencemanagement.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import mk.ukim.finki.examscheduling.preferencemanagement.domain.enums.PreferenceLevel
import mk.ukim.finki.examscheduling.preferencemanagement.domain.enums.RoomFeature
import java.time.Instant
import java.time.LocalTime
import java.util.*

data class ExamSessionPeriodId @JsonCreator constructor(
    @JsonProperty("value") val value: String
) {
    init {
        require(value.isNotBlank()) { "ExamSessionPeriodId cannot be blank" }
        require(value.length <= 100) { "ExamSessionPeriodId cannot exceed 100 characters" }
    }

    companion object {
        fun from(academicYear: String, examSession: String): ExamSessionPeriodId {
            return ExamSessionPeriodId("${academicYear}_${examSession.uppercase().replace(" ", "_")}")
        }
    }

    override fun toString(): String = value
}

data class CourseId @JsonCreator constructor(
    @JsonProperty("value") val value: String
) {
    init {
        require(value.isNotBlank()) { "CourseId cannot be blank" }
        require(value.length <= 20) { "CourseId cannot exceed 20 characters" }
    }

    override fun toString(): String = value
}

data class ProfessorId @JsonCreator constructor(
    @JsonProperty("value") val value: UUID
) {
    constructor(value: String) : this(UUID.fromString(value))

    override fun toString(): String = value.toString()
}


data class TimeSlot @JsonCreator constructor(
    @JsonProperty("dayOfWeek") val dayOfWeek: Int,
    @JsonProperty("startTime") val startTime: LocalTime,
    @JsonProperty("endTime") val endTime: LocalTime
) {
    init {
        require(dayOfWeek in 1..7) { "Day of week must be between 1 (Monday) and 7 (Sunday)" }
        require(startTime.isBefore(endTime)) { "Start time must be before end time" }
        require(endTime.minusMinutes(30).isAfter(startTime) || endTime.minusMinutes(30) == startTime) {
            "Time slot must be at least 30 minutes long"
        }
    }

    @JsonIgnore
    fun getDayName(): String = when (dayOfWeek) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> "Unknown"
    }

    @JsonIgnore
    fun overlaps(other: TimeSlot): Boolean {
        return this.dayOfWeek == other.dayOfWeek &&
                this.startTime.isBefore(other.endTime) &&
                this.endTime.isAfter(other.startTime)
    }

    @JsonIgnore
    fun getDisplayString(): String = "${getDayName()} $startTime-$endTime"
}

data class SpaceAssignment @JsonCreator constructor(
    @JsonProperty("roomId") val roomId: String,
    @JsonProperty("capacity") val capacity: Int,
    @JsonProperty("features") val features: Set<RoomFeature> = emptySet()
) {
    init {
        require(roomId.isNotBlank()) { "Room ID cannot be blank" }
        require(capacity > 0) { "Room capacity must be positive" }
    }

    fun hasFeature(feature: RoomFeature): Boolean = features.contains(feature)
}

data class PreferenceDetails @JsonCreator constructor(
    @JsonProperty("courseId") val courseId: CourseId,
    @JsonProperty("timePreferences") val timePreferences: List<TimeSlotPreference>,
    @JsonProperty("roomPreferences") val roomPreferences: List<RoomPreference> = emptyList(),
    @JsonProperty("durationPreference") val durationPreference: DurationPreference? = null,
    @JsonProperty("specialRequirements") val specialRequirements: String? = null,
    @JsonProperty("lastUpdated") val lastUpdated: Instant = Instant.now()
) {
    init {
        require(timePreferences.isNotEmpty()) { "At least one time preference must be specified" }
        require(specialRequirements == null || specialRequirements.length <= 500) {
            "Special requirements cannot exceed 500 characters"
        }
    }

    fun hasConflictingTimePreferences(): Boolean {
        for (i in timePreferences.indices) {
            for (j in i + 1 until timePreferences.size) {
                if (timePreferences[i].timeSlot.overlaps(timePreferences[j].timeSlot) &&
                    timePreferences[i].preferenceLevel.isPositive() &&
                    timePreferences[j].preferenceLevel.isPositive()
                ) {
                    return true
                }
            }
        }
        return false
    }
}

data class TimeSlotPreference @JsonCreator constructor(
    @JsonProperty("timeSlot") val timeSlot: TimeSlot,
    @JsonProperty("preferenceLevel") val preferenceLevel: PreferenceLevel,
    @JsonProperty("reason") val reason: String? = null
) {
    init {
        require(reason == null || reason.length <= 200) { "Reason cannot exceed 200 characters" }
    }
}

data class RoomPreference @JsonCreator constructor(
    @JsonProperty("roomId") val roomId: String,
    @JsonProperty("preferenceLevel") val preferenceLevel: PreferenceLevel,
    @JsonProperty("reason") val reason: String? = null
) {
    init {
        require(roomId.isNotBlank()) { "Room ID cannot be blank" }
        require(reason == null || reason.length <= 200) { "Reason cannot exceed 200 characters" }
    }
}

data class DurationPreference @JsonCreator constructor(
    @JsonProperty("preferredDurationMinutes") val preferredDurationMinutes: Int,
    @JsonProperty("minimumDurationMinutes") val minimumDurationMinutes: Int,
    @JsonProperty("maximumDurationMinutes") val maximumDurationMinutes: Int
) {
    init {
        require(preferredDurationMinutes > 0) { "Preferred duration must be positive" }
        require(minimumDurationMinutes > 0) { "Minimum duration must be positive" }
        require(maximumDurationMinutes > 0) { "Maximum duration must be positive" }
        require(minimumDurationMinutes <= preferredDurationMinutes) {
            "Minimum duration cannot be greater than preferred duration"
        }
        require(preferredDurationMinutes <= maximumDurationMinutes) {
            "Preferred duration cannot be greater than maximum duration"
        }
    }
}

data class SubmissionId @JsonCreator constructor(
    @JsonProperty("value") val value: UUID = UUID.randomUUID()
) {
    constructor(value: String) : this(UUID.fromString(value))

    override fun toString(): String = value.toString()
}