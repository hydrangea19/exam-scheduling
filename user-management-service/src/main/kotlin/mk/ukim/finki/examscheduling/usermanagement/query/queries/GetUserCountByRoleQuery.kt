package mk.ukim.finki.examscheduling.usermanagement.query.queries

data class GetUserCountByRoleQuery(
    val activeOnly: Boolean = true
)