package mk.ukim.finki.examscheduling.usermanagement.domain.event

import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import java.time.Instant

data class UserRoleChangedEvent(
    val userId: String,
    val previousRole: UserRole,
    val newRole: UserRole,
    val changedBy: String,
    val reason: String?,
    val changedAt: Instant = Instant.now()
)
