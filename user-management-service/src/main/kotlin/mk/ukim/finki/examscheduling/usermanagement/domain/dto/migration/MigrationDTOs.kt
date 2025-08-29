package mk.ukim.finki.examscheduling.usermanagement.domain.dto.migration

import java.time.Instant

data class MigrationResponse(
    val success: Boolean,
    val message: String,
    val totalUsers: Int = 0,
    val successCount: Int = 0,
    val errorCount: Int = 0,
    val errors: List<String> = emptyList(),
    val timestamp: Instant
)

data class MigrationStatusResponse(
    val traditionalStorageCount: Int = 0,
    val eventStoreCount: Int = 0,
    val migrationNeeded: Boolean = false,
    val migrationProgress: Int = 0, // percentage
    val sampleTraditionalUsers: List<UserSummary> = emptyList(),
    val error: String? = null,
    val timestamp: Instant
)

data class UserSyncVerificationResponse(
    val userId: String,
    val traditionalUserExists: Boolean = false,
    val eventStoreUserExists: Boolean = false,
    val inSync: Boolean = false,
    val traditionalUser: UserSummary? = null,
    val eventStoreUser: UserSummary? = null,
    val differences: Map<String, Any>? = null,
    val error: String? = null,
    val timestamp: Instant
)

data class UserSummary(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val active: Boolean,
    val createdAt: Instant
)