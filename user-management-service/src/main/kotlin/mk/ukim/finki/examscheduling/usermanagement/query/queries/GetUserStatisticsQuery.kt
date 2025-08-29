package mk.ukim.finki.examscheduling.usermanagement.query.queries

data class GetUserStatisticsQuery(
    val includeRoleBreakdown: Boolean = true,
    val includeLoginStats: Boolean = true,
    val includeKeycloakStats: Boolean = true
)