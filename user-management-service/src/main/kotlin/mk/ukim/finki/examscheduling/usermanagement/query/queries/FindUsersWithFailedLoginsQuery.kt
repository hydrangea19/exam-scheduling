package mk.ukim.finki.examscheduling.usermanagement.query.queries

data class FindUsersWithFailedLoginsQuery(
    val minFailedAttempts: Int = 3,
    val page: Int = 0,
    val size: Int = 20
)