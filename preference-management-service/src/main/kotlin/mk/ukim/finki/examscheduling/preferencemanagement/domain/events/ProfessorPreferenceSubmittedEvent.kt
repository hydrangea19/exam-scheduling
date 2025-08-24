package mk.ukim.finki.examscheduling.preferencemanagement.domain.events

import java.util.*

data class ProfessorPreferenceSubmittedEvent(
    val professorId: UUID,
    val examSessionPeriodId: String,
    val preferenceId: UUID,
    val courseIds: List<String>,
    val submissionVersion: Int = 1
) : BaseEvent()
