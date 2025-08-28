package mk.ukim.finki.examscheduling.usermanagement.query.queries

data class FindKeycloakUsersQuery(
    val syncedOnly: Boolean = true,
    val page: Int = 0,
    val size: Int = 20
)
