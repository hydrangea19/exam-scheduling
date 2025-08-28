package mk.ukim.finki.examscheduling.usermanagement.query

import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import java.time.Instant
import java.util.*

data class UserActivitySummary(
    val userId: UUID,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val active: Boolean,
    val createdAt: Instant,
    val lastUpdatedAt: Instant,
    val lastSuccessfulLogin: Instant?,
    val failedLoginAttempts: Int,
    val lastRoleChange: Instant?,
    val lastRoleChangedBy: UUID?,
    val keycloakUserId: String?,
    val lastKeycloakSync: Instant?,
    val notificationPreferences: Map<String, Any>,
    val uiPreferences: Map<String, Any>,
    val deactivationReason: String?,
    val deactivatedBy: UUID?,
    val deactivatedAt: Instant?
)