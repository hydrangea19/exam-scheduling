package mk.ukim.finki.examscheduling.preferencemanagement.query

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import mk.ukim.finki.examscheduling.preferencemanagement.domain.ExamSessionPeriodId
import mk.ukim.finki.examscheduling.preferencemanagement.domain.ProfessorId

data class FindPreferencesByProfessorQuery(
    val professorId: ProfessorId,
    val examSessionPeriodId: ExamSessionPeriodId? = null
)

data class FindPreferencesBySessionQuery @JsonCreator constructor(
    @JsonProperty("examSessionPeriodId")
    val examSessionPeriodId: String
)

data class GetPreferenceStatisticsQuery(
    val examSessionPeriodId: ExamSessionPeriodId
)

data class GetExamSessionPeriodsQuery(
    val includeWindowStatus: Boolean = true
)

data class FindConflictingPreferencesQuery(
    val examSessionPeriodId: ExamSessionPeriodId,
    val professorIds: List<ProfessorId>? = null
)