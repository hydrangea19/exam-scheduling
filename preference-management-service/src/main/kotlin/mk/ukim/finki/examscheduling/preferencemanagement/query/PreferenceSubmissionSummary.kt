package mk.ukim.finki.examscheduling.preferencemanagement.query

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "preference_submission_summary")
data class PreferenceSubmissionSummary(
    @Id
    val submissionId: String,
    val professorId: String,
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val status: String,
    val submissionVersion: Int,
    val totalTimePreferences: Int,
    val totalRoomPreferences: Int,
    val coursesCount: Int,
    val submittedAt: Instant?,
    val lastUpdatedAt: Instant,
    val hasValidationErrors: Boolean = false,
    val validationErrorsCount: Int = 0,
    val hasSpecialRequirements: Boolean = false,

    @Column(columnDefinition = "TEXT")
    val preferredTimeSlotsJson: String? = null,

    @Column(columnDefinition = "TEXT")
    val unavailableTimeSlotsJson: String? = null,

    @Column(columnDefinition = "TEXT")
    val additionalNotes: String? = null
) {
    constructor() : this("", "", "", "", "", "", 0, 0, 0, 0, null, Instant.now())

    companion object {
        private val objectMapper = ObjectMapper()
    }

    val preferredTimeSlots: List<Map<String, Any>>
        get() = try {
            if (preferredTimeSlotsJson.isNullOrBlank()) emptyList()
            else objectMapper.readValue(preferredTimeSlotsJson, List::class.java) as List<Map<String, Any>>
        } catch (e: Exception) { emptyList() }

    val unavailableTimeSlots: List<Map<String, Any>>
        get() = try {
            if (unavailableTimeSlotsJson.isNullOrBlank()) emptyList()
            else objectMapper.readValue(unavailableTimeSlotsJson, List::class.java) as List<Map<String, Any>>
        } catch (e: Exception) { emptyList() }
}