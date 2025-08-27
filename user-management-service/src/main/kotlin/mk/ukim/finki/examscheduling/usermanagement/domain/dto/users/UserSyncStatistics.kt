package mk.ukim.finki.examscheduling.usermanagement.domain.dto.users

import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole

data class UserSyncStatistics(
    val totalUsers: Int,
    val activeUsers: Int,
    val inactiveUsers: Int,
    val roleBreakdown: Map<UserRole, Int>,
    val lastSyncTime: java.time.Instant
)
