package mk.ukim.finki.examscheduling.usermanagement.service

import mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak.KeycloakUserInfo
import mk.ukim.finki.examscheduling.usermanagement.domain.User
import mk.ukim.finki.examscheduling.usermanagement.domain.command.*
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.users.SyncResult
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.users.UserSyncStatistics
import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import mk.ukim.finki.examscheduling.usermanagement.query.UserView
import mk.ukim.finki.examscheduling.usermanagement.query.queries.FindUserByIdQuery
import mk.ukim.finki.examscheduling.usermanagement.repository.UserRepository
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.queryhandling.QueryGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserSyncService(
    private val userRepository: UserRepository,
    private val commandGateway: CommandGateway,
    private val queryGateway: QueryGateway
) {
    private val logger = LoggerFactory.getLogger(UserSyncService::class.java)

    fun syncKeycloakUser(keycloakUserInfo: KeycloakUserInfo): User {
        val email = keycloakUserInfo.email ?: keycloakUserInfo.preferredUsername ?: "unknown@example.com"

        logger.debug("Syncing Keycloak user: {}", email)

        val existingUser = userRepository.findByEmail(email)

        return if (existingUser != null) {
            updateExistingUser(existingUser, keycloakUserInfo)
        } else {
            createNewUserFromKeycloak(keycloakUserInfo)
        }
    }

    private fun createNewUserFromKeycloak(keycloakUserInfo: KeycloakUserInfo): User {
        val email = keycloakUserInfo.email ?: keycloakUserInfo.preferredUsername ?: "unknown@example.com"
        val firstName = keycloakUserInfo.givenName ?: extractFirstNameFromEmail(email)
        val lastName = keycloakUserInfo.familyName ?: extractLastNameFromEmail(email)
        val role = mapKeycloakRolesToUserRole(keycloakUserInfo.roles)

        logger.info("Creating new user from Keycloak: {} with role: {}", email, role)

        val newUser = User(
            email = email,
            firstName = firstName,
            lastName = lastName,
            middleName = null,
            role = role,
            active = true
        )
        val savedUser = userRepository.save(newUser)

        try {
            val createUserCommand = CreateUserCommand(
                userId = savedUser.id.toString(),
                email = email,
                firstName = firstName,
                lastName = lastName,
                middleName = null,
                role = role,
                passwordHash = null
            )
            commandGateway.sendAndWait<Any>(createUserCommand)
            logger.info("User created in event store: {} with ID: {}", email, savedUser.id)

        } catch (e: Exception) {
            logger.error("Failed to create user in event store for email: {}, ID: {}", email, savedUser.id, e)
        }
        return savedUser
    }

    private fun updateExistingUser(existingUser: User, keycloakUserInfo: KeycloakUserInfo): User {
        val firstName = keycloakUserInfo.givenName ?: existingUser.firstName
        val lastName = keycloakUserInfo.familyName ?: existingUser.lastName
        val newRole = mapKeycloakRolesToUserRole(keycloakUserInfo.roles)

        val needsUpdate = existingUser.firstName != firstName ||
                existingUser.lastName != lastName ||
                existingUser.role != newRole ||
                !existingUser.active

        if (needsUpdate) {
            logger.info(
                "Updating existing user from Keycloak: {} - role: {} -> {}",
                existingUser.email, existingUser.role, newRole
            )
            val updatedUser = existingUser.update(
                firstName = firstName,
                lastName = lastName,
                active = true
            )

            if (updatedUser.role != newRole) {
                updatedUser.role = newRole
            }
            val savedUser = userRepository.save(updatedUser)

            try {
                syncUserWithEventStore(savedUser)
            } catch (e: Exception) {
                logger.error("Failed to sync user update with event store: {}", savedUser.email, e)
            }
            return savedUser
        } else {
            logger.debug("No updates needed for user: {}", existingUser.email)

            try {
                ensureUserExistsInEventStore(existingUser)
            } catch (e: Exception) {
                logger.error("Failed to ensure user exists in event store: {}", existingUser.email, e)
            }
            return existingUser
        }
    }

    private fun ensureUserExistsInEventStore(user: User) {
        try {
            val userView = queryGateway.query(
                FindUserByIdQuery(user.id.toString()),
                UserView::class.java
            ).get()

            if (userView == null) {
                logger.info("User {} not found in event store, creating...", user.email)

                val createUserCommand = CreateUserCommand(
                    userId = user.id.toString(),
                    email = user.email,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    middleName = user.middleName,
                    role = user.role,
                    passwordHash = user.passwordHash
                )

                commandGateway.sendAndWait<Any>(createUserCommand)
                logger.info("User {} created in event store", user.email)
            } else {
                logger.debug("User {} already exists in event store", user.email)
            }
        } catch (e: Exception) {
            logger.error("Error ensuring user exists in event store: {}", user.email, e)
            throw e
        }
    }

    /**
     * Synchronizes an updated traditional User with the event store
     */
    private fun syncUserWithEventStore(user: User) {
        try {
            ensureUserExistsInEventStore(user)

            val userView = queryGateway.query(
                FindUserByIdQuery(user.id.toString()),
                UserView::class.java
            ).get()

            if (userView != null) {
                if (userView.firstName != user.firstName ||
                    userView.lastName != user.lastName ||
                    userView.middleName != user.middleName) {

                    val updateProfileCommand = UpdateUserProfileCommand(
                        userId = user.id.toString(),
                        firstName = user.firstName,
                        lastName = user.lastName,
                        middleName = user.middleName
                    )

                    commandGateway.sendAndWait<Any>(updateProfileCommand)
                    logger.debug("User profile updated in event store: {}", user.email)
                }

                if (userView.role != user.role) {
                    val changeRoleCommand = ChangeUserRoleCommand(
                        userId = user.id.toString(),
                        newRole = user.role,
                        previousRole = userView.role,
                        changedBy = user.id.toString(),
                        reason = "Synchronized from Keycloak"
                    )

                    commandGateway.sendAndWait<Any>(changeRoleCommand)
                    logger.debug("User role updated in event store: {} -> {}", userView.role, user.role)
                }

                if (userView.active != user.active) {
                    if (user.active) {
                        val activateCommand = ActivateUserCommand(
                            userId = user.id.toString(),
                            activatedBy = user.id.toString(),
                            reason = "Synchronized from Keycloak"
                        )
                        commandGateway.sendAndWait<Any>(activateCommand)
                    } else {
                        val deactivateCommand = DeactivateUserCommand(
                            userId = user.id.toString(),
                            deactivatedBy = user.id.toString(),
                            reason = "Synchronized from Keycloak"
                        )
                        commandGateway.sendAndWait<Any>(deactivateCommand)
                    }
                    logger.debug("User activation status updated in event store: {}", user.active)
                }
            }

        } catch (e: Exception) {
            logger.error("Error synchronizing user with event store: {}", user.email, e)
            throw e
        }
    }

    private fun mapKeycloakRolesToUserRole(keycloakRoles: List<String>): UserRole {
        return when {
            keycloakRoles.contains("ADMIN") -> UserRole.ADMIN
            keycloakRoles.contains("PROFESSOR") -> UserRole.PROFESSOR
            keycloakRoles.contains("STUDENT") -> UserRole.STUDENT
            else -> {
                logger.debug("No recognized role found in Keycloak roles: {}, defaulting to STUDENT", keycloakRoles)
                UserRole.STUDENT
            }
        }
    }

    fun findOrCreateUserByKeycloakSubject(
        subject: String,
        email: String?,
        preferredUsername: String?,
        roles: List<String>
    ): User? {

        val foundByEmail = email?.let { userRepository.findByEmail(it) }
        if (foundByEmail != null) {
            return foundByEmail
        }

        val foundByUsername = if (preferredUsername?.contains("@") == true) {
            userRepository.findByEmail(preferredUsername)
        } else null
        if (foundByUsername != null) {
            return foundByUsername
        }

        val userEmail = email ?: preferredUsername ?: return null
        if (!userEmail.contains("@")) {
            logger.warn("Cannot create user without valid email. Subject: {}, username: {}", subject, preferredUsername)
            return null
        }

        logger.info("Creating new user from Keycloak subject: {} with email: {}", subject, userEmail)

        val newUser = User(
            email = userEmail,
            firstName = extractFirstNameFromEmail(userEmail),
            lastName = extractLastNameFromEmail(userEmail),
            role = mapKeycloakRolesToUserRole(roles),
            active = true
        )

        return userRepository.save(newUser)
    }

    fun deactivateUser(email: String): User? {
        val user = userRepository.findByEmail(email)
        if (user != null && user.active) {
            logger.info("Deactivating user: {}", email)
            val deactivatedUser = user.update(active = false)
            return userRepository.save(deactivatedUser)
        }
        return user
    }

    private fun extractFirstNameFromEmail(email: String): String {
        val localPart = email.substringBefore("@")
        return localPart.substringBefore(".").replaceFirstChar { it.uppercase() }
    }

    private fun extractLastNameFromEmail(email: String): String {
        val localPart = email.substringBefore("@")
        return if (localPart.contains(".")) {
            localPart.substringAfter(".").replaceFirstChar { it.uppercase() }
        } else {
            "User"
        }
    }

    fun syncMultipleUsers(keycloakUsers: List<KeycloakUserInfo>): List<User> {
        logger.info("Batch syncing {} users from Keycloak", keycloakUsers.size)

        return keycloakUsers.mapNotNull { keycloakUser ->
            try {
                syncKeycloakUser(keycloakUser)
            } catch (e: Exception) {
                logger.error("Failed to sync user: {}", keycloakUser.email, e)
                null
            }
        }
    }

    fun syncExistingUsersToEventStore(): SyncResult {
        logger.info("Starting batch sync of existing users to event store")

        val allUsers = userRepository.findAll()
        var successCount = 0
        var errorCount = 0
        val errors = mutableListOf<String>()

        for (user in allUsers) {
            try {
                ensureUserExistsInEventStore(user)
                successCount++
                if (successCount % 10 == 0) {
                    logger.info("Synced {} users so far...", successCount)
                }
            } catch (e: Exception) {
                errorCount++
                val errorMsg = "Failed to sync user ${user.email}: ${e.message}"
                errors.add(errorMsg)
                logger.error(errorMsg, e)
            }
        }

        val result = SyncResult(
            totalUsers = allUsers.size,
            successCount = successCount,
            errorCount = errorCount,
            errors = errors
        )

        logger.info("Batch sync completed: {}", result)
        return result
    }

    fun getSyncStatistics(): UserSyncStatistics {
        val totalUsers = userRepository.count()
        val activeUsers = userRepository.findAll().count { it.active }
        val inactiveUsers = totalUsers - activeUsers

        val roleBreakdown = userRepository.findAll()
            .filter { it.role != null }
            .groupBy { it.role!! }
            .mapValues { it.value.size }

        return UserSyncStatistics(
            totalUsers = totalUsers.toInt(),
            activeUsers = activeUsers,
            inactiveUsers = inactiveUsers.toInt(),
            roleBreakdown = roleBreakdown,
            lastSyncTime = Instant.now()
        )
    }
}