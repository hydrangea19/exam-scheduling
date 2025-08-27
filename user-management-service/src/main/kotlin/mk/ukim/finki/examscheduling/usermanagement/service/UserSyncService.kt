package mk.ukim.finki.examscheduling.usermanagement.service

import mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak.KeycloakUserInfo
import mk.ukim.finki.examscheduling.usermanagement.domain.User
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.users.UserSyncStatistics
import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import mk.ukim.finki.examscheduling.usermanagement.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserSyncService(
    private val userRepository: UserRepository
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

        return userRepository.save(newUser)
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

            return userRepository.save(updatedUser)
        } else {
            logger.debug("No updates needed for user: {}", existingUser.email)
            return existingUser
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