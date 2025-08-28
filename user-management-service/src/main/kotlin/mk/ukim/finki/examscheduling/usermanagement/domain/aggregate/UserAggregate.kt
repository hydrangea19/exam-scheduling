package mk.ukim.finki.examscheduling.usermanagement.domain.aggregate

import mk.ukim.finki.examscheduling.usermanagement.domain.command.*
import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import mk.ukim.finki.examscheduling.usermanagement.domain.event.*
import mk.ukim.finki.examscheduling.usermanagement.domain.exceptions.UserDomainException
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

@Aggregate
class UserAggregate {

    @AggregateIdentifier
    private lateinit var userId: UUID

    private lateinit var email: String
    private lateinit var firstName: String
    private lateinit var lastName: String
    private var middleName: String? = null
    private lateinit var role: UserRole
    private var hasPassword: Boolean = false
    private var active: Boolean = false
    private var createdAt: Instant = Instant.now()
    private var lastUpdatedAt: Instant = Instant.now()

    private var keycloakUserId: String? = null
    private var lastKeycloakSync: Instant? = null

    private var notificationPreferences: MutableMap<String, Boolean> = mutableMapOf()
    private var uiPreferences: MutableMap<String, String> = mutableMapOf()

    private var lastSuccessfulLogin: Instant? = null
    private var failedLoginAttempts: Int = 0

    private val logger = LoggerFactory.getLogger(UserAggregate::class.java)

    constructor()

    @CommandHandler
    constructor(command: CreateUserCommand) {
        logger.info("Creating user with email: {}", command.email)

        validateCreateUserCommand(command)

        AggregateLifecycle.apply(
            UserCreatedEvent(
                userId = command.userId,
                email = command.email,
                firstName = command.firstName,
                lastName = command.lastName,
                middleName = command.middleName,
                role = command.role,
                hasPassword = command.passwordHash != null,
                createdAt = Instant.now()
            )
        )

        logger.info("UserCreatedEvent applied for user: {}", command.email)
    }

    @CommandHandler
    fun handle(command: UpdateUserProfileCommand) {
        logger.info("Updating profile for user: {}", userId)

        validateUpdateProfileCommand(command)

        if (hasProfileChanges(command)) {
            AggregateLifecycle.apply(
                UserProfileUpdatedEvent(
                    userId = command.userId,
                    previousFirstName = firstName,
                    previousLastName = lastName,
                    previousMiddleName = middleName,
                    newFirstName = command.firstName,
                    newLastName = command.lastName,
                    newMiddleName = command.middleName
                )
            )
        } else {
            logger.debug("No profile changes detected for user: {}", userId)
        }
    }

    @CommandHandler
    fun handle(command: ChangeUserEmailCommand) {
        logger.info("Changing email for user: {} from {} to {}", userId, command.oldEmail, command.newEmail)

        validateEmailChangeCommand(command)

        if (email != command.newEmail) {
            AggregateLifecycle.apply(
                UserEmailChangedEvent(
                    userId = command.userId,
                    oldEmail = command.oldEmail,
                    newEmail = command.newEmail
                )
            )
        }
    }

    @CommandHandler
    fun handle(command: ChangeUserRoleCommand) {
        logger.info("Changing role for user: {} from {} to {}", userId, command.previousRole, command.newRole)

        validateRoleChangeCommand(command)

        if (role != command.newRole) {
            AggregateLifecycle.apply(
                UserRoleChangedEvent(
                    userId = command.userId,
                    previousRole = command.previousRole,
                    newRole = command.newRole,
                    changedBy = command.changedBy,
                    reason = command.reason
                )
            )

            AggregateLifecycle.apply(
                UserAccountAuditEvent(
                    userId = command.userId,
                    action = "ROLE_CHANGED",
                    performedBy = command.changedBy,
                    details = mapOf(
                        "previousRole" to command.previousRole.name,
                        "newRole" to command.newRole.name,
                        "reason" to (command.reason ?: "No reason provided")
                    )
                )
            )
        }
    }

    @CommandHandler
    fun handle(command: SetUserPasswordCommand) {
        logger.info("Setting password for user: {}", userId)

        validatePasswordCommand(command)

        AggregateLifecycle.apply(
            UserPasswordSetEvent(
                userId = command.userId,
                hasPassword = true
            )
        )
    }

    @CommandHandler
    fun handle(command: ActivateUserCommand) {
        logger.info("Activating user: {}", userId)

        validateActivationCommand(command)

        if (!active) {
            AggregateLifecycle.apply(
                UserActivatedEvent(
                    userId = command.userId,
                    activatedBy = command.activatedBy,
                    reason = command.reason
                )
            )
        } else {
            logger.debug("User {} is already active", userId)
        }
    }

    @CommandHandler
    fun handle(command: DeactivateUserCommand) {
        logger.info("Deactivating user: {}", userId)

        validateDeactivationCommand(command)

        if (active) {
            AggregateLifecycle.apply(
                UserDeactivatedEvent(
                    userId = command.userId,
                    deactivatedBy = command.deactivatedBy,
                    reason = command.reason
                )
            )

            AggregateLifecycle.apply(
                UserAccountAuditEvent(
                    userId = command.userId,
                    action = "USER_DEACTIVATED",
                    performedBy = command.deactivatedBy,
                    details = mapOf(
                        "reason" to command.reason,
                        "email" to email
                    )
                )
            )
        } else {
            logger.debug("User {} is already inactive", userId)
        }
    }

    @CommandHandler
    fun handle(command: UpdateUserPreferencesCommand) {
        logger.info("Updating preferences for user: {}", userId)

        validatePreferencesCommand(command)

        AggregateLifecycle.apply(
            UserPreferencesUpdatedEvent(
                userId = command.userId,
                notificationPreferences = command.notificationPreferences,
                uiPreferences = command.uiPreferences
            )
        )
    }

    @CommandHandler
    fun handle(command: RecordLoginAttemptCommand) {
        logger.debug("Recording login attempt for user: {}, successful: {}", userId, command.successful)

        AggregateLifecycle.apply(
            LoginAttemptRecordedEvent(
                userId = command.userId,
                successful = command.successful,
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
                attemptedAt = command.timestamp.toInstant()
            )
        )
    }

    @CommandHandler
    fun handle(command: SynchronizeUserWithKeycloakCommand) {
        logger.info("Synchronizing user {} with Keycloak", userId)

        AggregateLifecycle.apply(
            UserSynchronizedWithKeycloakEvent(
                userId = command.userId,
                keycloakUserId = command.keycloakUserId,
                keycloakData = command.keycloakUserInfo
            )
        )
    }

    @EventSourcingHandler
    fun on(event: UserCreatedEvent) {
        logger.debug("Applying UserCreatedEvent for user: {}", event.email)

        this.userId = event.userId
        this.email = event.email
        this.firstName = event.firstName
        this.lastName = event.lastName
        this.middleName = event.middleName
        this.role = event.role
        this.hasPassword = event.hasPassword
        this.active = true
        this.createdAt = event.createdAt
        this.lastUpdatedAt = event.createdAt

        this.notificationPreferences = mutableMapOf(
            "emailNotifications" to true,
            "scheduleUpdates" to true,
            "systemAlerts" to false
        )
        this.uiPreferences = mutableMapOf(
            "theme" to "light",
            "language" to "en"
        )
    }

    @EventSourcingHandler
    fun on(event: UserProfileUpdatedEvent) {
        logger.debug("Applying UserProfileUpdatedEvent for user: {}", userId)

        this.firstName = event.newFirstName
        this.lastName = event.newLastName
        this.middleName = event.newMiddleName
        this.lastUpdatedAt = event.updatedAt
    }

    @EventSourcingHandler
    fun on(event: UserEmailChangedEvent) {
        logger.debug("Applying UserEmailChangedEvent for user: {}", userId)

        this.email = event.newEmail
        this.lastUpdatedAt = event.changedAt
    }

    @EventSourcingHandler
    fun on(event: UserRoleChangedEvent) {
        logger.debug("Applying UserRoleChangedEvent for user: {}", userId)

        this.role = event.newRole
        this.lastUpdatedAt = event.changedAt
    }

    @EventSourcingHandler
    fun on(event: UserPasswordSetEvent) {
        logger.debug("Applying UserPasswordSetEvent for user: {}", userId)

        this.hasPassword = event.hasPassword
        this.lastUpdatedAt = event.setAt
    }

    @EventSourcingHandler
    fun on(event: UserActivatedEvent) {
        logger.debug("Applying UserActivatedEvent for user: {}", userId)

        this.active = true
        this.lastUpdatedAt = event.activatedAt
    }

    @EventSourcingHandler
    fun on(event: UserDeactivatedEvent) {
        logger.debug("Applying UserDeactivatedEvent for user: {}", userId)

        this.active = false
        this.lastUpdatedAt = event.deactivatedAt
    }

    @EventSourcingHandler
    fun on(event: UserPreferencesUpdatedEvent) {
        logger.debug("Applying UserPreferencesUpdatedEvent for user: {}", userId)

        this.notificationPreferences.clear()
        this.notificationPreferences.putAll(event.notificationPreferences)
        this.uiPreferences.clear()
        this.uiPreferences.putAll(event.uiPreferences)
        this.lastUpdatedAt = event.updatedAt
    }

    @EventSourcingHandler
    fun on(event: LoginAttemptRecordedEvent) {
        logger.debug("Applying LoginAttemptRecordedEvent for user: {}", userId)

        if (event.successful) {
            this.lastSuccessfulLogin = event.attemptedAt
            this.failedLoginAttempts = 0
        } else {
            this.failedLoginAttempts++
        }
    }

    @EventSourcingHandler
    fun on(event: UserSynchronizedWithKeycloakEvent) {
        logger.debug("Applying UserSynchronizedWithKeycloakEvent for user: {}", userId)

        this.keycloakUserId = event.keycloakUserId
        this.lastKeycloakSync = event.synchronizedAt
    }

    private fun validateCreateUserCommand(command: CreateUserCommand) {
        val errors = mutableListOf<String>()

        if (command.email.isBlank()) {
            errors.add("Email cannot be blank")
        } else if (!isValidEmail(command.email)) {
            errors.add("Email format is invalid")
        }

        if (command.firstName.isBlank()) {
            errors.add("First name cannot be blank")
        }

        if (command.lastName.isBlank()) {
            errors.add("Last name cannot be blank")
        }

        if (command.firstName.length > 100) {
            errors.add("First name cannot exceed 100 characters")
        }

        if (command.lastName.length > 100) {
            errors.add("Last name cannot exceed 100 characters")
        }

        command.middleName?.let {
            if (it.length > 100) {
                errors.add("Middle name cannot exceed 100 characters")
            }
        }

        if (errors.isNotEmpty()) {
            throw UserDomainException("User creation validation failed: ${errors.joinToString(", ")}")
        }
    }

    private fun validateUpdateProfileCommand(command: UpdateUserProfileCommand) {
        if (!::userId.isInitialized) {
            throw UserDomainException("Cannot update profile: user does not exist")
        }

        if (command.firstName.isBlank() || command.lastName.isBlank()) {
            throw UserDomainException("First name and last name cannot be blank")
        }

        if (command.firstName.length > 100 || command.lastName.length > 100) {
            throw UserDomainException("Name fields cannot exceed 100 characters")
        }
    }

    private fun validateEmailChangeCommand(command: ChangeUserEmailCommand) {
        if (!::email.isInitialized || email != command.oldEmail) {
            throw UserDomainException("Current email does not match the specified old email")
        }

        if (!isValidEmail(command.newEmail)) {
            throw UserDomainException("New email format is invalid")
        }
    }

    private fun validateRoleChangeCommand(command: ChangeUserRoleCommand) {
        if (!::role.isInitialized || role != command.previousRole) {
            throw UserDomainException("Current role does not match the specified previous role")
        }
    }

    private fun validatePasswordCommand(command: SetUserPasswordCommand) {
        if (command.passwordHash.isBlank()) {
            throw UserDomainException("Password hash cannot be blank")
        }
    }

    private fun validateActivationCommand(command: ActivateUserCommand) {
        if (!::userId.isInitialized) {
            throw UserDomainException("Cannot activate: user does not exist")
        }
    }

    private fun validateDeactivationCommand(command: DeactivateUserCommand) {
        if (!::userId.isInitialized) {
            throw UserDomainException("Cannot deactivate: user does not exist")
        }

        if (command.reason.isBlank()) {
            throw UserDomainException("Reason for deactivation is required")
        }
    }

    private fun validatePreferencesCommand(command: UpdateUserPreferencesCommand) {
        if (!::userId.isInitialized) {
            throw UserDomainException("Cannot update preferences: user does not exist")
        }
    }

    private fun hasProfileChanges(command: UpdateUserProfileCommand): Boolean {
        return firstName != command.firstName ||
                lastName != command.lastName ||
                middleName != command.middleName
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".") && email.length > 5
    }

    fun getUserId(): UUID = userId
    fun getEmail(): String = email
    fun getFirstName(): String = firstName
    fun getLastName(): String = lastName
    fun getMiddleName(): String? = middleName
    fun getRole(): UserRole = role
    fun isActive(): Boolean = active
    fun hasPassword(): Boolean = hasPassword
    fun getFullName(): String = if (middleName.isNullOrBlank()) "$firstName $lastName" else "$firstName $middleName $lastName"
    fun getCreatedAt(): Instant = createdAt
    fun getLastUpdatedAt(): Instant = lastUpdatedAt
}