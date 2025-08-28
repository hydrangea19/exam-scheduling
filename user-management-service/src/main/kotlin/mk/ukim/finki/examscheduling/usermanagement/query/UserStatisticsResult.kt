package mk.ukim.finki.examscheduling.usermanagement.query

import java.time.Instant

data class UserStatisticsResult(
    val totalUsers: Long,
    val activeUsers: Long,
    val inactiveUsers: Long,
    val usersWithPassword: Long,
    val keycloakUsers: Long,
    val usersWithRecentLogin: Long,
    val roleBreakdown: Map<String, Long>,
    val generatedAt: Instant
)