package mk.ukim.finki.examscheduling.usermanagement.domain.event

import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import java.time.Instant
import java.util.*

data class UserRoleChangedEvent(
    val userId: UUID,
    val previousRole: UserRole,
    val newRole: UserRole,
    val changedBy: UUID,
    val reason: String?,
    val changedAt: Instant = Instant.now()
)
