package mk.ukim.finki.examscheduling.preferencemanagement.domain.events

import java.time.Instant

data class PreferenceSubmissionWindowOpenedEvent(
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val openedBy: String,
    val submissionDeadline: Instant,
    val description: String? = null
) : BaseEvent()
