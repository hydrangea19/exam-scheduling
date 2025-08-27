package mk.ukim.finki.examscheduling.preferencemanagement.domain.events

data class PreferenceSubmissionWindowClosedEvent(
    val examSessionPeriodId: String,
    val closedBy: String,
    val reason: String? = null,
    val totalSubmissions: Int = 0
) : BaseEvent()
