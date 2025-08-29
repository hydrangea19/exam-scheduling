package mk.ukim.finki.examscheduling.usermanagement.domain.dto.users

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import mk.ukim.finki.examscheduling.usermanagement.query.UserPageResponse
import mk.ukim.finki.examscheduling.usermanagement.query.UserStatisticsResult
import mk.ukim.finki.examscheduling.usermanagement.query.UserView
import java.time.Instant

// ===== REQUEST DTOs =====

data class CreateUserRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email cannot exceed 255 characters")
    val email: String,

    @field:NotBlank(message = "First name is required")
    @field:Size(max = 100, message = "First name cannot exceed 100 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 100, message = "Last name cannot exceed 100 characters")
    val lastName: String,

    @field:Size(max = 100, message = "Middle name cannot exceed 100 characters")
    val middleName: String? = null,

    @field:NotBlank(message = "Role is required")
    @field:Pattern(
        regexp = "^(ADMIN|PROFESSOR|STUDENT)$",
        message = "Role must be one of: ADMIN, PROFESSOR, STUDENT"
    )
    val role: String
)

data class UpdateUserProfileRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 100, message = "First name cannot exceed 100 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 100, message = "Last name cannot exceed 100 characters")
    val lastName: String,

    @field:Size(max = 100, message = "Middle name cannot exceed 100 characters")
    val middleName: String? = null
)

data class ChangeUserEmailRequest(
    @field:NotBlank(message = "Current email is required")
    @field:Email(message = "Current email must be valid")
    val currentEmail: String,

    @field:NotBlank(message = "New email is required")
    @field:Email(message = "New email must be valid")
    @field:Size(max = 255, message = "New email cannot exceed 255 characters")
    val newEmail: String
)

data class ChangeUserRoleRequest(
    @field:NotBlank(message = "Current role is required")
    @field:Pattern(
        regexp = "^(ADMIN|PROFESSOR|STUDENT)$",
        message = "Current role must be one of: ADMIN, PROFESSOR, STUDENT"
    )
    val currentRole: String,

    @field:NotBlank(message = "New role is required")
    @field:Pattern(
        regexp = "^(ADMIN|PROFESSOR|STUDENT)$",
        message = "New role must be one of: ADMIN, PROFESSOR, STUDENT"
    )
    val newRole: String,

    @field:Size(max = 500, message = "Reason cannot exceed 500 characters")
    val reason: String? = null
)

data class UserActivateRequest(
    @field:Size(max = 500, message = "Reason cannot exceed 500 characters")
    val reason: String? = null
)

data class UserDeactivatedRequest(
    @field:NotBlank(message = "Reason is required for deactivation")
    @field:Size(max = 500, message = "Reason cannot exceed 500 characters")
    val reason: String
)

data class UpdateUserPreferencesRequest(
    @field:Valid
    val notificationPreferences: Map<String, Boolean> = mapOf(
        "emailNotifications" to true,
        "scheduleUpdates" to true,
        "systemAlerts" to false
    ),

    @field:Valid
    val uiPreferences: Map<String, String> = mapOf(
        "theme" to "light",
        "language" to "en"
    )
) {
    @AssertTrue(message = "Notification preferences cannot be empty")
    fun hasNotificationPreferences(): Boolean = notificationPreferences.isNotEmpty()

    @AssertTrue(message = "UI preferences cannot be empty")
    fun hasUiPreferences(): Boolean = uiPreferences.isNotEmpty()

    @AssertTrue(message = "Theme must be 'light' or 'dark'")
    fun hasValidTheme(): Boolean = uiPreferences["theme"] in listOf("light", "dark")

    @AssertTrue(message = "Language must be valid ISO code")
    fun hasValidLanguage(): Boolean {
        val language = uiPreferences["language"]
        return language != null && language.matches(Regex("^[a-z]{2}$"))
    }
}

// ===== RESPONSE DTOs =====

data class UserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val middleName: String? = null,
    val fullName: String,
    val role: String,
    val hasPassword: Boolean,
    val active: Boolean,
    val createdAt: Instant,
    val lastUpdatedAt: Instant,
    val keycloakUserId: String? = null,
    val lastKeycloakSync: Instant? = null,
    val lastSuccessfulLogin: Instant? = null,
    val failedLoginAttempts: Int = 0,
    val notificationPreferences: Map<String, Boolean>? = null,
    val uiPreferences: Map<String, String>? = null,
    val lastRoleChange: Instant? = null,
    val lastRoleChangedBy: String? = null,
    val deactivationReason: String? = null,
    val deactivatedBy: String? = null,
    val deactivatedAt: Instant? = null
) {
    companion object {
        fun fromUserView(userView: UserView): UserResponse {
            return UserResponse(
                id = userView.userId,
                email = userView.email,
                firstName = userView.firstName,
                lastName = userView.lastName,
                middleName = userView.middleName,
                fullName = userView.fullName,
                role = userView.role.name,
                hasPassword = userView.hasPassword,
                active = userView.active,
                createdAt = userView.createdAt,
                lastUpdatedAt = userView.lastUpdatedAt,
                keycloakUserId = userView.keycloakUserId,
                lastKeycloakSync = userView.lastKeycloakSync,
                lastSuccessfulLogin = userView.lastSuccessfulLogin,
                failedLoginAttempts = userView.failedLoginAttempts,
                notificationPreferences = parseJsonToMap(userView.notificationPreferences),
                uiPreferences = parseJsonToStringMap(userView.uiPreferences),
                lastRoleChange = userView.lastRoleChange,
                lastRoleChangedBy = userView.lastRoleChangedBy?.toString(),
                deactivationReason = userView.deactivationReason,
                deactivatedBy = userView.deactivatedBy?.toString(),
                deactivatedAt = userView.deactivatedAt
            )
        }

        private fun parseJsonToMap(jsonString: String?): Map<String, Boolean>? {
            if (jsonString.isNullOrBlank()) return null
            return try {
                ObjectMapper().readValue(jsonString, object : TypeReference<Map<String, Boolean>>() {})
            } catch (e: Exception) {
                null
            }
        }

        private fun parseJsonToStringMap(jsonString: String?): Map<String, String>? {
            if (jsonString.isNullOrBlank()) return null
            return try {
                ObjectMapper().readValue(jsonString, object : TypeReference<Map<String, String>>() {})
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class PagedUserResponse(
    val users: List<UserResponse>,
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val totalElements: Long,
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean
) {
    companion object {
        fun fromUserPageResponse(userPageResponse: UserPageResponse): PagedUserResponse {
            return PagedUserResponse(
                users = userPageResponse.users.map { UserResponse.fromUserView(it) },
                page = userPageResponse.page,
                size = userPageResponse.size,
                totalPages = userPageResponse.totalPages,
                totalElements = userPageResponse.totalElements,
                first = userPageResponse.page == 0,
                last = userPageResponse.page >= userPageResponse.totalPages - 1,
                empty = userPageResponse.users.isEmpty()
            )
        }
    }
}

data class UserStatisticsResponse(
    val totalUsers: Long,
    val activeUsers: Long,
    val inactiveUsers: Long,
    val usersWithPassword: Long,
    val keycloakUsers: Long,
    val usersWithRecentLogin: Long,
    val roleBreakdown: Map<String, Long>,
    val generatedAt: Instant
) {
    companion object {
        fun fromUserStatisticsResult(stats: UserStatisticsResult): UserStatisticsResponse {
            return UserStatisticsResponse(
                totalUsers = stats.totalUsers,
                activeUsers = stats.activeUsers,
                inactiveUsers = stats.inactiveUsers,
                usersWithPassword = stats.usersWithPassword,
                keycloakUsers = stats.keycloakUsers,
                usersWithRecentLogin = stats.usersWithRecentLogin,
                roleBreakdown = stats.roleBreakdown,
                generatedAt = stats.generatedAt
            )
        }
    }
}




