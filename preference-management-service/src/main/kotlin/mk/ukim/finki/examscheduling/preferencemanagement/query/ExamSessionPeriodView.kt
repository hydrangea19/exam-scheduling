package mk.ukim.finki.examscheduling.preferencemanagement.query

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "exam_session_period_view")
data class ExamSessionPeriodView(
    @Id
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val isWindowOpen: Boolean,
    val submissionDeadline: Instant?,
    val totalSubmissions: Int = 0,
    val uniqueProfessors: Int = 0,
    val windowOpenedAt: Instant?,
    val windowClosedAt: Instant?,
    val createdAt: Instant,
    val description: String?
) {
    constructor() : this("", "", "", false, null, 0, 0, null, null, Instant.now(), null)
}