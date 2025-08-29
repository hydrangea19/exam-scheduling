package mk.ukim.finki.examscheduling.usermanagement.controller

import mk.ukim.finki.examscheduling.usermanagement.domain.User
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.migration.MigrationResponse
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.migration.MigrationStatusResponse
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.migration.UserSummary
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.migration.UserSyncVerificationResponse
import mk.ukim.finki.examscheduling.usermanagement.query.UserStatisticsResult
import mk.ukim.finki.examscheduling.usermanagement.query.UserView
import mk.ukim.finki.examscheduling.usermanagement.query.queries.FindUserByIdQuery
import mk.ukim.finki.examscheduling.usermanagement.query.queries.GetUserStatisticsQuery
import mk.ukim.finki.examscheduling.usermanagement.repository.UserRepository
import mk.ukim.finki.examscheduling.usermanagement.service.UserSyncService
import org.axonframework.queryhandling.QueryGateway
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/admin/migration")
@PreAuthorize("hasRole('ADMIN')")
class UserMigrationController(
    private val userSyncService: UserSyncService,
    private val userRepository: UserRepository,
    private val queryGateway: QueryGateway
) {

    private val logger = LoggerFactory.getLogger(UserMigrationController::class.java)

    @PostMapping("/sync-users-to-event-store")
    fun syncUsersToEventStore(authentication: Authentication): ResponseEntity<MigrationResponse> {
        logger.info("Starting user migration to event store by admin: {}", authentication.name)

        return try {
            val result = userSyncService.syncExistingUsersToEventStore()

            ResponseEntity.ok(
                MigrationResponse(
                    success = true,
                    message = "User migration completed",
                    totalUsers = result.totalUsers,
                    successCount = result.successCount,
                    errorCount = result.errorCount,
                    errors = result.errors.take(10),
                    timestamp = Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to migrate users to event store", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                MigrationResponse(
                    success = false,
                    message = "Migration failed: ${e.message}",
                    errors = listOf(e.message ?: "Unknown error"),
                    timestamp = Instant.now()
                )
            )
        }
    }

    @GetMapping("/migration-status")
    fun getMigrationStatus(authentication: Authentication): ResponseEntity<MigrationStatusResponse> {
        logger.debug("Checking migration status by admin: {}", authentication.name)

        return try {
            val traditionalUserCount = userRepository.count().toInt()

            val userStatsQuery = GetUserStatisticsQuery(
                includeRoleBreakdown = false,
                includeLoginStats = false,
                includeKeycloakStats = false
            )
            val userStats = queryGateway.query(userStatsQuery, UserStatisticsResult::class.java).get()
            val eventStoreUserCount = userStats.totalUsers.toInt()

            val traditionalUsers = userRepository.findAll().take(5).map { user ->
                UserSummary(
                    id = user.id.toString(),
                    email = user.email,
                    fullName = user.getFullName(),
                    role = user.role.name,
                    active = user.active,
                    createdAt = user.createdAt
                )
            }

            val status = MigrationStatusResponse(
                traditionalStorageCount = traditionalUserCount,
                eventStoreCount = eventStoreUserCount,
                migrationNeeded = traditionalUserCount > eventStoreUserCount,
                migrationProgress = if (traditionalUserCount > 0) {
                    (eventStoreUserCount.toDouble() / traditionalUserCount * 100).toInt()
                } else 100,
                sampleTraditionalUsers = traditionalUsers,
                timestamp = Instant.now()
            )

            ResponseEntity.ok(status)
        } catch (e: Exception) {
            logger.error("Failed to get migration status", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                MigrationStatusResponse(
                    error = "Failed to get migration status: ${e.message}",
                    timestamp = Instant.now()
                )
            )
        }
    }

    @PostMapping("/verify-user-sync/{userId}")
    fun verifyUserSync(
        @PathVariable userId: UUID,
        authentication: Authentication
    ): ResponseEntity<UserSyncVerificationResponse> {
        logger.debug("Verifying user sync for ID: {} by admin: {}", userId, authentication.name)

        return try {
            val traditionalUser = userRepository.findById(userId).orElse(null)

            val eventStoreUser = queryGateway.query(
                FindUserByIdQuery(userId.toString()),
                UserView::class.java
            ).get()

            val response = UserSyncVerificationResponse(
                userId = userId.toString(),
                traditionalUserExists = traditionalUser != null,
                eventStoreUserExists = eventStoreUser != null,
                inSync = traditionalUser != null && eventStoreUser != null && areUsersInSync(
                    traditionalUser,
                    eventStoreUser
                ),
                traditionalUser = traditionalUser?.let {
                    UserSummary(
                        id = it.id.toString(),
                        email = it.email,
                        fullName = it.getFullName(),
                        role = it.role.name,
                        active = it.active,
                        createdAt = it.createdAt
                    )
                },
                eventStoreUser = eventStoreUser?.let {
                    UserSummary(
                        id = it.userId,
                        email = it.email,
                        fullName = it.fullName,
                        role = it.role.name,
                        active = it.active,
                        createdAt = it.createdAt
                    )
                },
                differences = if (traditionalUser != null && eventStoreUser != null) {
                    findUserDifferences(traditionalUser, eventStoreUser)
                } else null,
                timestamp = Instant.now()
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Failed to verify user sync for ID: {}", userId, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                UserSyncVerificationResponse(
                    userId = userId.toString(),
                    error = "Failed to verify user sync: ${e.message}",
                    timestamp = Instant.now()
                )
            )
        }
    }

    private fun areUsersInSync(traditionalUser: User, eventStoreUser: UserView): Boolean {
        return traditionalUser.email == eventStoreUser.email &&
                traditionalUser.firstName == eventStoreUser.firstName &&
                traditionalUser.lastName == eventStoreUser.lastName &&
                traditionalUser.middleName == eventStoreUser.middleName &&
                traditionalUser.role == eventStoreUser.role &&
                traditionalUser.active == eventStoreUser.active
    }

    private fun findUserDifferences(traditionalUser: User, eventStoreUser: UserView): Map<String, Any> {
        val differences = mutableMapOf<String, Any>()

        if (traditionalUser.email != eventStoreUser.email) {
            differences["email"] = mapOf(
                "traditional" to traditionalUser.email,
                "eventStore" to eventStoreUser.email
            )
        }

        if (traditionalUser.firstName != eventStoreUser.firstName) {
            differences["firstName"] = mapOf(
                "traditional" to traditionalUser.firstName,
                "eventStore" to eventStoreUser.firstName
            )
        }

        if (traditionalUser.lastName != eventStoreUser.lastName) {
            differences["lastName"] = mapOf(
                "traditional" to traditionalUser.lastName,
                "eventStore" to eventStoreUser.lastName
            )
        }

        if (traditionalUser.middleName != eventStoreUser.middleName) {
            differences["middleName"] = mapOf(
                "traditional" to traditionalUser.middleName,
                "eventStore" to eventStoreUser.middleName
            )
        }

        if (traditionalUser.role != eventStoreUser.role) {
            differences["role"] = mapOf(
                "traditional" to traditionalUser.role.name,
                "eventStore" to eventStoreUser.role.name
            )
        }

        if (traditionalUser.active != eventStoreUser.active) {
            differences["active"] = mapOf(
                "traditional" to traditionalUser.active,
                "eventStore" to eventStoreUser.active
            )
        }

        return differences
    }
}