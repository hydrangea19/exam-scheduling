package mk.ukim.finki.examscheduling.usermanagement.query.queries

import java.util.*

data class FindUserActivitySummaryQuery(
    val userId: UUID,
    val includeLoginHistory: Boolean = true,
    val includeRoleHistory: Boolean = true,
    val includePreferenceHistory: Boolean = false
)