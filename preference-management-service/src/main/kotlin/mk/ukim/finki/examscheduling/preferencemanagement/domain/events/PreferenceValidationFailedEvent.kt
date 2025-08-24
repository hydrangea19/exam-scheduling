package mk.ukim.finki.examscheduling.preferencemanagement.domain.events

import java.util.*

data class PreferenceValidationFailedEvent(
    val professorId: UUID,
    val examSessionPeriodId: String,
    val validationErrors: List<String>,
    val attemptedSubmission: String? = null
) : BaseEvent()
