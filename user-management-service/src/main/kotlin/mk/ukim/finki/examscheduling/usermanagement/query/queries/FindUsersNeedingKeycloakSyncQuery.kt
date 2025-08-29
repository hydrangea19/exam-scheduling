package mk.ukim.finki.examscheduling.usermanagement.query.queries

data class FindUsersNeedingKeycloakSyncQuery(
    val syncThresholdHours: Long = 24,
    val page: Int = 0,
    val size: Int = 20
)