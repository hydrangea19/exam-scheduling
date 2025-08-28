package mk.ukim.finki.examscheduling.usermanagement.query.queries

data class FindActiveUsersQuery(
    val active: Boolean = true,
    val page: Int = 0,
    val size: Int = 20
)