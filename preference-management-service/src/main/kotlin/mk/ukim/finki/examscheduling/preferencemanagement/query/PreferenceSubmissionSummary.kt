package mk.ukim.finki.examscheduling.preferencemanagement.query

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
    val hasSpecialRequirements: Boolean = false
) {
    constructor() : this("", "", "", "", "", "", 0, 0, 0, 0, null, Instant.now())
}