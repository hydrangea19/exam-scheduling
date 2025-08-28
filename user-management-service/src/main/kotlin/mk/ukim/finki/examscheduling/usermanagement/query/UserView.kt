package mk.ukim.finki.examscheduling.usermanagement.query

import jakarta.persistence.*
import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "user_view",
    indexes = [
        Index(name = "idx_user_view_email", columnList = "email"),
        Index(name = "idx_user_view_active", columnList = "active"),
        Index(name = "idx_user_view_role", columnList = "role"),
        Index(name = "idx_user_view_created_at", columnList = "created_at"),
        Index(name = "idx_user_view_full_name", columnList = "full_name")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_view_email", columnNames = ["email"])
    ]
)
data class UserView(
    @Id
    @Column(name = "user_id")
    val userId: UUID,

    @Column(name = "email", nullable = false, unique = true, length = 255)
    val email: String,

    @Column(name = "first_name", nullable = false, length = 100)
    val firstName: String,

    @Column(name = "last_name", nullable = false, length = 100)
    val lastName: String,

    @Column(name = "middle_name", length = 100)
    val middleName: String? = null,

    @Column(name = "full_name", nullable = false, length = 302)
    val fullName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    val role: UserRole,

    @Column(name = "has_password", nullable = false)
    val hasPassword: Boolean = false,

    @Column(name = "active", nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "last_updated_at", nullable = false)
    val lastUpdatedAt: Instant,

    @Column(name = "keycloak_user_id", length = 255)
    val keycloakUserId: String? = null,

    @Column(name = "last_keycloak_sync")
    val lastKeycloakSync: Instant? = null,

    @Column(name = "last_successful_login")
    val lastSuccessfulLogin: Instant? = null,

    @Column(name = "failed_login_attempts", nullable = false)
    val failedLoginAttempts: Int = 0,

    @Column(name = "notification_preferences", columnDefinition = "TEXT")
    val notificationPreferences: String? = null,

    @Column(name = "ui_preferences", columnDefinition = "TEXT")
    val uiPreferences: String? = null,

    @Column(name = "last_role_change")
    val lastRoleChange: Instant? = null,

    @Column(name = "last_role_changed_by")
    val lastRoleChangedBy: UUID? = null,

    @Column(name = "deactivation_reason", length = 500)
    val deactivationReason: String? = null,

    @Column(name = "deactivated_by")
    val deactivatedBy: UUID? = null,

    @Column(name = "deactivated_at")
    val deactivatedAt: Instant? = null,

    @CreationTimestamp
    @Column(name = "view_created_at", nullable = false, updatable = false)
    val viewCreatedAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "view_updated_at", nullable = false)
    val viewUpdatedAt: Instant = Instant.now()
) {
    constructor() : this(
        userId = UUID.randomUUID(),
        email = "",
        firstName = "",
        lastName = "",
        fullName = "",
        role = UserRole.STUDENT,
        createdAt = Instant.now(),
        lastUpdatedAt = Instant.now()
    )

    companion object {
        fun buildFullName(firstName: String, lastName: String, middleName: String?): String {
            return if (middleName.isNullOrBlank()) {
                "$firstName $lastName"
            } else {
                "$firstName $middleName $lastName"
            }
        }

        fun create(
            userId: UUID,
            email: String,
            firstName: String,
            lastName: String,
            middleName: String?,
            role: UserRole,
            hasPassword: Boolean,
            createdAt: Instant
        ): UserView {
            return UserView(
                userId = userId,
                email = email,
                firstName = firstName,
                lastName = lastName,
                middleName = middleName,
                fullName = buildFullName(firstName, lastName, middleName),
                role = role,
                hasPassword = hasPassword,
                active = true,
                createdAt = createdAt,
                lastUpdatedAt = createdAt
            )
        }
    }

    fun updateProfile(firstName: String, lastName: String, middleName: String?, updatedAt: Instant): UserView {
        return copy(
            firstName = firstName,
            lastName = lastName,
            middleName = middleName,
            fullName = buildFullName(firstName, lastName, middleName),
            lastUpdatedAt = updatedAt,
            viewUpdatedAt = Instant.now()
        )
    }

    fun updateEmail(newEmail: String, updatedAt: Instant): UserView {
        return copy(
            email = newEmail,
            lastUpdatedAt = updatedAt,
            viewUpdatedAt = Instant.now()
        )
    }

    fun updateRole(newRole: UserRole, changedBy: UUID, updatedAt: Instant): UserView {
        return copy(
            role = newRole,
            lastUpdatedAt = updatedAt,
            lastRoleChange = updatedAt,
            lastRoleChangedBy = changedBy,
            viewUpdatedAt = Instant.now()
        )
    }

    fun updatePassword(hasPassword: Boolean, updatedAt: Instant): UserView {
        return copy(
            hasPassword = hasPassword,
            lastUpdatedAt = updatedAt,
            viewUpdatedAt = Instant.now()
        )
    }

    fun activate(updatedAt: Instant): UserView {
        return copy(
            active = true,
            lastUpdatedAt = updatedAt,
            deactivationReason = null,
            deactivatedBy = null,
            deactivatedAt = null,
            viewUpdatedAt = Instant.now()
        )
    }

    fun deactivate(reason: String, deactivatedBy: UUID, updatedAt: Instant): UserView {
        return copy(
            active = false,
            lastUpdatedAt = updatedAt,
            deactivationReason = reason,
            deactivatedBy = deactivatedBy,
            deactivatedAt = updatedAt,
            viewUpdatedAt = Instant.now()
        )
    }

    fun updateKeycloakSync(keycloakUserId: String, syncedAt: Instant): UserView {
        return copy(
            keycloakUserId = keycloakUserId,
            lastKeycloakSync = syncedAt,
            viewUpdatedAt = Instant.now()
        )
    }

    fun updateLoginAttempt(successful: Boolean, attemptedAt: Instant): UserView {
        return if (successful) {
            copy(
                lastSuccessfulLogin = attemptedAt,
                failedLoginAttempts = 0,
                viewUpdatedAt = Instant.now()
            )
        } else {
            copy(
                failedLoginAttempts = failedLoginAttempts + 1,
                viewUpdatedAt = Instant.now()
            )
        }
    }

    fun updatePreferences(
        notificationPrefs: String?,
        uiPrefs: String?,
        updatedAt: Instant
    ): UserView {
        return copy(
            notificationPreferences = notificationPrefs,
            uiPreferences = uiPrefs,
            lastUpdatedAt = updatedAt,
            viewUpdatedAt = Instant.now()
        )
    }
}
