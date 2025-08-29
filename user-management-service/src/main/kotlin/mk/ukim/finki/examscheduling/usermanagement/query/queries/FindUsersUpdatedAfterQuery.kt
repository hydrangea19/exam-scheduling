package mk.ukim.finki.examscheduling.usermanagement.query.queries

import java.time.Instant

data class FindUsersUpdatedAfterQuery(
    val after: Instant,
    val page: Int = 0,
    val size: Int = 20
)