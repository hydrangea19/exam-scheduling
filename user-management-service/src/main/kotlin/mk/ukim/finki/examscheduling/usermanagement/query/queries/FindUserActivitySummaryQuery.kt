package mk.ukim.finki.examscheduling.usermanagement.query.queries

data class FindUserActivitySummaryQuery(
    val userId: String,
    val includeLoginHistory: Boolean = true,
    val includeRoleHistory: Boolean = true,
    val includePreferenceHistory: Boolean = false
)