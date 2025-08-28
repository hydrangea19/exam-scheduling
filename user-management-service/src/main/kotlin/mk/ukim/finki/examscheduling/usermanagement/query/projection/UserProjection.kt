package mk.ukim.finki.examscheduling.usermanagement.query.projection

import com.fasterxml.jackson.databind.ObjectMapper
import mk.ukim.finki.examscheduling.usermanagement.domain.event.*
import mk.ukim.finki.examscheduling.usermanagement.query.UserView
import mk.ukim.finki.examscheduling.usermanagement.query.repository.UserViewRepository
import org.axonframework.eventhandling.EventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserProjection(
    private val userViewRepository: UserViewRepository,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(UserProjection::class.java)

    @EventHandler
    @Transactional
    fun on(event: UserCreatedEvent) {
        logger.info("Projecting UserCreatedEvent for user: {}", event.email)

        try {
            val userView = UserView.create(
                userId = event.userId,
                email = event.email,
                firstName = event.firstName,
                lastName = event.lastName,
                middleName = event.middleName,
                role = event.role,
                hasPassword = event.hasPassword,
                createdAt = event.createdAt
            )

            userViewRepository.save(userView)
            logger.info("UserView created successfully for user: {}", event.email)
        } catch (e: Exception) {
            logger.error("Failed to project UserCreatedEvent for user: {}", event.email, e)
            throw e
        }
    }

    @EventHandler
    @Transactional
    fun on(event: UserProfileUpdatedEvent) {
        logger.info("Projecting UserProfileUpdatedEvent for user: {}", event.userId)

        try {
            val existingUser = userViewRepository.findById(event.userId)
                .orElseThrow {
                    IllegalStateException("UserView not found for profile update: ${event.userId}")
                }

            val updatedUser = existingUser.updateProfile(
                firstName = event.newFirstName,
                lastName = event.newLastName,
                middleName = event.newMiddleName,
                updatedAt = event.updatedAt
            )

            userViewRepository.save(updatedUser)
            logger.info("UserView profile updated successfully for user: {}", event.userId)
        } catch (e: Exception) {
            logger.error("Failed to project UserProfileUpdatedEvent for user: {}", event.userId, e)
            throw e
        }
    }

    @EventHandler
    @Transactional
    fun on(event: UserEmailChangedEvent) {
        logger.info("Projecting UserEmailChangedEvent for user: {}", event.userId)

        try {
            val existingUser = userViewRepository.findById(event.userId)
                .orElseThrow {
                    IllegalStateException("UserView not found for email change: ${event.userId}")
                }

            val updatedUser = existingUser.updateEmail(
                newEmail = event.newEmail,
                updatedAt = event.changedAt
            )

            userViewRepository.save(updatedUser)
            logger.info("UserView email updated successfully for user: {}", event.userId)
        } catch (e: Exception) {
            logger.error("Failed to project UserEmailChangedEvent for user: {}", event.userId, e)
            throw e
        }
    }

    @EventHandler
    @Transactional
    fun on(event: UserRoleChangedEvent) {
        logger.info("Projecting UserRoleChangedEvent for user: {}", event.userId)

        try {
            val existingUser = userViewRepository.findById(event.userId)
                .orElseThrow {
                    IllegalStateException("UserView not found for role change: ${event.userId}")
                }

            val updatedUser = existingUser.updateRole(
                newRole = event.newRole,
                changedBy = event.changedBy,
                updatedAt = event.changedAt
            )

            userViewRepository.save(updatedUser)
            logger.info(
                "UserView role updated successfully for user: {} to role: {}",
                event.userId, event.newRole
            )
        } catch (e: Exception) {
            logger.error("Failed to project UserRoleChangedEvent for user: {}", event.userId, e)
            throw e
        }
    }

    @EventHandler
    @Transactional
    fun on(event: UserPasswordSetEvent) {
        logger.info("Projecting UserPasswordSetEvent for user: {}", event.userId)

        try {
            val existingUser = userViewRepository.findById(event.userId)
                .orElseThrow {
                    IllegalStateException("UserView not found for password set: ${event.userId}")
                }

            val updatedUser = existingUser.updatePassword(
                hasPassword = event.hasPassword,
                updatedAt = event.setAt
            )

            userViewRepository.save(updatedUser)
            logger.info("UserView password status updated for user: {}", event.userId)
        } catch (e: Exception) {
            logger.error("Failed to project UserPasswordSetEvent for user: {}", event.userId, e)
            throw e
        }
    }

    @EventHandler
    @Transactional
    fun on(event: UserActivatedEvent) {
        logger.info("Projecting UserActivatedEvent for user: {}", event.userId)

        try {
            val existingUser = userViewRepository.findById(event.userId)
                .orElseThrow {
                    IllegalStateException("UserView not found for activation: ${event.userId}")
                }

            val updatedUser = existingUser.activate(updatedAt = event.activatedAt)

            userViewRepository.save(updatedUser)
            logger.info("UserView activated successfully for user: {}", event.userId)
        } catch (e: Exception) {
            logger.error("Failed to project UserActivatedEvent for user: {}", event.userId, e)
            throw e
        }
    }

    @EventHandler
    @Transactional
    fun on(event: UserDeactivatedEvent) {
        logger.info("Projecting UserDeactivatedEvent for user: {}", event.userId)

        try {
            val existingUser = userViewRepository.findById(event.userId)
                .orElseThrow {
                    IllegalStateException("UserView not found for deactivation: ${event.userId}")
                }

            val updatedUser = existingUser.deactivate(
                reason = event.reason,
                deactivatedBy = event.deactivatedBy,
                updatedAt = event.deactivatedAt
            )

            userViewRepository.save(updatedUser)
            logger.info("UserView deactivated successfully for user: {}", event.userId)
        } catch (e: Exception) {
            logger.error("Failed to project UserDeactivatedEvent for user: {}", event.userId, e)
            throw e
        }
    }

    @EventHandler
    @Transactional
    fun on(event: UserPreferencesUpdatedEvent) {
        logger.info("Projecting UserPreferencesUpdatedEvent for user: {}", event.userId)

        try {
            val existingUser = userViewRepository.findById(event.userId)
                .orElseThrow {
                    IllegalStateException("UserView not found for preferences update: ${event.userId}")
                }

            val notificationPrefsJson = if (event.notificationPreferences.isNotEmpty()) {
                objectMapper.writeValueAsString(event.notificationPreferences)
            } else null

            val uiPrefsJson = if (event.uiPreferences.isNotEmpty()) {
                objectMapper.writeValueAsString(event.uiPreferences)
            } else null

            val updatedUser = existingUser.updatePreferences(
                notificationPrefs = notificationPrefsJson,
                uiPrefs = uiPrefsJson,
                updatedAt = event.updatedAt
            )

            userViewRepository.save(updatedUser)
            logger.info("UserView preferences updated successfully for user: {}", event.userId)
        } catch (e: Exception) {
            logger.error("Failed to project UserPreferencesUpdatedEvent for user: {}", event.userId, e)
            throw e
        }
    }

    @EventHandler
    @Transactional
    fun on(event: LoginAttemptRecordedEvent) {
        logger.debug("Projecting LoginAttemptRecordedEvent for user: {}", event.userId)

        try {
            val existingUser = userViewRepository.findById(event.userId)
                .orElseThrow {
                    IllegalStateException("UserView not found for login attempt: ${event.userId}")
                }

            val updatedUser = existingUser.updateLoginAttempt(
                successful = event.successful,
                attemptedAt = event.attemptedAt
            )

            userViewRepository.save(updatedUser)
            logger.debug(
                "UserView login attempt updated for user: {} (successful: {})",
                event.userId, event.successful
            )
        } catch (e: Exception) {
            logger.error("Failed to project LoginAttemptRecordedEvent for user: {}", event.userId, e)
            throw e
        }
    }

    @EventHandler
    @Transactional
    fun on(event: UserSynchronizedWithKeycloakEvent) {
        logger.info("Projecting UserSynchronizedWithKeycloakEvent for user: {}", event.userId)

        try {
            val existingUser = userViewRepository.findById(event.userId)
                .orElseThrow {
                    IllegalStateException("UserView not found for Keycloak sync: ${event.userId}")
                }

            val updatedUser = existingUser.updateKeycloakSync(
                keycloakUserId = event.keycloakUserId,
                syncedAt = event.synchronizedAt
            )

            userViewRepository.save(updatedUser)
            logger.info("UserView Keycloak sync updated for user: {}", event.userId)
        } catch (e: Exception) {
            logger.error("Failed to project UserSynchronizedWithKeycloakEvent for user: {}", event.userId, e)
            throw e
        }
    }

    @EventHandler
    @Transactional
    fun on(event: UserAccountAuditEvent) {
        logger.info(
            "Projecting UserAccountAuditEvent for user: {} action: {}",
            event.userId, event.action
        )
        try {
            logger.info(
                "Audit event processed: User {} performed {} by {} at {}",
                event.userId, event.action, event.performedBy, event.timestamp
            )
        } catch (e: Exception) {
            logger.error("Failed to project UserAccountAuditEvent for user: {}", event.userId, e)
            throw e
        }
    }

    @EventHandler
    @Transactional
    fun on(event: UserValidationFailedEvent) {
        logger.warn(
            "Projecting UserValidationFailedEvent for user: {} command: {}",
            event.userId, event.commandType
        )
        try {
            logger.warn(
                "Validation failed for user {}: {} - Errors: {}",
                event.userId, event.commandType, event.validationErrors.joinToString(", ")
            )
        } catch (e: Exception) {
            logger.error("Failed to project UserValidationFailedEvent for user: {}", event.userId, e)
            throw e
        }
    }
}