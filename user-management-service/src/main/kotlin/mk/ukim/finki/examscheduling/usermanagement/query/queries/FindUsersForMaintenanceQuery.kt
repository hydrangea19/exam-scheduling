package mk.ukim.finki.examscheduling.usermanagement.query.queries

data class FindUsersForMaintenanceQuery(
    val includeInactiveUsers: Boolean = false,
    val includeUsersWithoutPasswords: Boolean = true,
    val includeUsersNeedingSync: Boolean = true,
    val page: Int = 0,
    val size: Int = 50
)