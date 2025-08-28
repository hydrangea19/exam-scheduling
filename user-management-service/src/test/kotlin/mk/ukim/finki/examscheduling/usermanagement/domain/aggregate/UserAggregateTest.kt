package mk.ukim.finki.examscheduling.usermanagement.domain.aggregate

import mk.ukim.finki.examscheduling.usermanagement.domain.command.*
import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import mk.ukim.finki.examscheduling.usermanagement.domain.event.*
import mk.ukim.finki.examscheduling.usermanagement.domain.exceptions.UserDomainException
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*


class UserAggregateTest {

    private lateinit var fixture: FixtureConfiguration<UserAggregate>
    private val userId = UUID.randomUUID()
    private val adminUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(UserAggregate::class.java)
    }

    @Test
    fun `should create user with valid data`() {
        val command = CreateUserCommand(
            userId = userId,
            email = "john.doe@example.com",
            firstName = "John",
            lastName = "Doe",
            middleName = "Middle",
            role = UserRole.PROFESSOR,
            passwordHash = "hashedPassword"
        )

        fixture.givenNoPriorActivity()
            .`when`(command)
            .expectEvents(
                UserCreatedEvent(
                    userId = userId,
                    email = "john.doe@example.com",
                    firstName = "John",
                    lastName = "Doe",
                    middleName = "Middle",
                    role = UserRole.PROFESSOR,
                    hasPassword = true
                )
            )
    }

    @Test
    fun `should reject user creation with blank email`() {
        val command = CreateUserCommand(
            userId = userId,
            email = "",
            firstName = "John",
            lastName = "Doe",
            role = UserRole.STUDENT
        )

        fixture.givenNoPriorActivity()
            .`when`(command)
            .expectException(UserDomainException::class.java)
    }

    @Test
    fun `should reject user creation with invalid email`() {
        val command = CreateUserCommand(
            userId = userId,
            email = "invalid-email",
            firstName = "John",
            lastName = "Doe",
            role = UserRole.STUDENT
        )

        fixture.givenNoPriorActivity()
            .`when`(command)
            .expectException(UserDomainException::class.java)
    }

    @Test
    fun `should reject user creation with blank first name`() {
        val command = CreateUserCommand(
            userId = userId,
            email = "john@example.com",
            firstName = "",
            lastName = "Doe",
            role = UserRole.STUDENT
        )

        fixture.givenNoPriorActivity()
            .`when`(command)
            .expectException(UserDomainException::class.java)
    }

    @Test
    fun `should update user profile with valid data`() {
        val updateCommand = UpdateUserProfileCommand(
            userId = userId,
            firstName = "Jane",
            lastName = "Smith",
            middleName = "Updated"
        )

        fixture.given(
            UserCreatedEvent(
                userId = userId,
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.PROFESSOR
            )
        )
            .`when`(updateCommand)
            .expectEvents(
                UserProfileUpdatedEvent(
                    userId = userId,
                    previousFirstName = "John",
                    previousLastName = "Doe",
                    previousMiddleName = null,
                    newFirstName = "Jane",
                    newLastName = "Smith",
                    newMiddleName = "Updated"
                )
            )
    }

    @Test
    fun `should not update profile when no changes detected`() {
        val updateCommand = UpdateUserProfileCommand(
            userId = userId,
            firstName = "John",
            lastName = "Doe",
            middleName = null
        )

        fixture.given(
            UserCreatedEvent(
                userId = userId,
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.PROFESSOR
            )
        )
            .`when`(updateCommand)
            .expectNoEvents()
    }

    @Test
    fun `should change user email with valid data`() {
        val changeEmailCommand = ChangeUserEmailCommand(
            userId = userId,
            oldEmail = "john.doe@example.com",
            newEmail = "jane.doe@example.com"
        )

        fixture.given(
            UserCreatedEvent(
                userId = userId,
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.PROFESSOR
            )
        )
            .`when`(changeEmailCommand)
            .expectEvents(
                UserEmailChangedEvent(
                    userId = userId,
                    oldEmail = "john.doe@example.com",
                    newEmail = "jane.doe@example.com"
                )
            )
    }

    @Test
    fun `should reject email change with mismatched old email`() {
        val changeEmailCommand = ChangeUserEmailCommand(
            userId = userId,
            oldEmail = "wrong@example.com",
            newEmail = "jane.doe@example.com"
        )

        fixture.given(
            UserCreatedEvent(
                userId = userId,
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.PROFESSOR
            )
        )
            .`when`(changeEmailCommand)
            .expectException(UserDomainException::class.java)
    }

    @Test
    fun `should change user role with proper authorization`() {
        val changeRoleCommand = ChangeUserRoleCommand(
            userId = userId,
            newRole = UserRole.ADMIN,
            previousRole = UserRole.PROFESSOR,
            changedBy = adminUserId,
            reason = "Promotion to administrator"
        )

        fixture.given(
            UserCreatedEvent(
                userId = userId,
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.PROFESSOR
            )
        )
            .`when`(changeRoleCommand)
            .expectEvents(
                UserRoleChangedEvent(
                    userId = userId,
                    previousRole = UserRole.PROFESSOR,
                    newRole = UserRole.ADMIN,
                    changedBy = adminUserId,
                    reason = "Promotion to administrator"
                ),
                UserAccountAuditEvent(
                    userId = userId,
                    action = "ROLE_CHANGED",
                    performedBy = adminUserId,
                    details = mapOf(
                        "previousRole" to "PROFESSOR",
                        "newRole" to "ADMIN",
                        "reason" to "Promotion to administrator"
                    )
                )
            )
    }

    @Test
    fun `should activate inactive user`() {
        val activateCommand = ActivateUserCommand(
            userId = userId,
            activatedBy = adminUserId,
            reason = "Account reactivation"
        )

        fixture.given(
            UserCreatedEvent(
                userId = userId,
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.PROFESSOR
            ),
            UserDeactivatedEvent(
                userId = userId,
                deactivatedBy = adminUserId,
                reason = "Temporary suspension"
            )
        )
            .`when`(activateCommand)
            .expectEvents(
                UserActivatedEvent(
                    userId = userId,
                    activatedBy = adminUserId,
                    reason = "Account reactivation"
                )
            )
    }

    @Test
    fun `should not activate already active user`() {
        val activateCommand = ActivateUserCommand(
            userId = userId,
            activatedBy = adminUserId
        )

        fixture.given(
            UserCreatedEvent(
                userId = userId,
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.PROFESSOR
            )
        )
            .`when`(activateCommand)
            .expectNoEvents()
    }

    @Test
    fun `should deactivate active user with reason`() {
        val deactivateCommand = DeactivateUserCommand(
            userId = userId,
            deactivatedBy = adminUserId,
            reason = "Account suspended for policy violation"
        )

        fixture.given(
            UserCreatedEvent(
                userId = userId,
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.PROFESSOR
            )
        )
            .`when`(deactivateCommand)
            .expectEvents(
                UserDeactivatedEvent(
                    userId = userId,
                    deactivatedBy = adminUserId,
                    reason = "Account suspended for policy violation",
                ),
                UserAccountAuditEvent(
                    userId = userId,
                    action = "USER_DEACTIVATED",
                    performedBy = adminUserId,
                    details = mapOf(
                        "reason" to "Account suspended for policy violation",
                        "email" to "john.doe@example.com"
                    )
                )
            )
    }

    @Test
    fun `should reject deactivation without reason`() {
        val deactivateCommand = DeactivateUserCommand(
            userId = userId,
            deactivatedBy = adminUserId,
            reason = ""
        )

        fixture.given(
            UserCreatedEvent(
                userId = userId,
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.PROFESSOR
            )
        )
            .`when`(deactivateCommand)
            .expectException(UserDomainException::class.java)
    }

    @Test
    fun `should set user password`() {
        val setPasswordCommand = SetUserPasswordCommand(
            userId = userId,
            passwordHash = "newHashedPassword"
        )

        fixture.given(
            UserCreatedEvent(
                userId = userId,
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.PROFESSOR,
                hasPassword = false
            )
        )
            .`when`(setPasswordCommand)
            .expectEvents(
                UserPasswordSetEvent(
                    userId = userId,
                    hasPassword = true
                )
            )
    }

    @Test
    fun `should update user preferences`() {
        val updatePreferencesCommand = UpdateUserPreferencesCommand(
            userId = userId,
            notificationPreferences = mapOf(
                "emailNotifications" to false,
                "scheduleUpdates" to true
            ),
            uiPreferences = mapOf(
                "theme" to "dark",
                "language" to "mk"
            )
        )

        fixture.given(
            UserCreatedEvent(
                userId = userId,
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.PROFESSOR
            )
        )
            .`when`(updatePreferencesCommand)
            .expectEvents(
                UserPreferencesUpdatedEvent(
                    userId = userId,
                    notificationPreferences = mapOf(
                        "emailNotifications" to false,
                        "scheduleUpdates" to true
                    ),
                    uiPreferences = mapOf(
                        "theme" to "dark",
                        "language" to "mk"
                    )
                )
            )
    }

    @Test
    fun `should record successful login attempt`() {
        val recordLoginCommand = RecordLoginAttemptCommand(
            userId = userId,
            successful = true,
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        fixture.given(
            UserCreatedEvent(
                userId = userId,
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.PROFESSOR
            )
        )
            .`when`(recordLoginCommand)
            .expectEvents(
                LoginAttemptRecordedEvent(
                    userId = userId,
                    successful = true,
                    ipAddress = "192.168.1.1",
                    userAgent = "Mozilla/5.0",
                    attemptedAt = recordLoginCommand.timestamp.toInstant()
                )
            )
    }

    @Test
    fun `should synchronize with Keycloak`() {
        val syncCommand = SynchronizeUserWithKeycloakCommand(
            userId = userId,
            keycloakUserId = "keycloak-uuid",
            keycloakUserInfo = mapOf(
                "sub" to "keycloak-uuid",
                "email" to "john.doe@example.com",
                "name" to "John Doe"
            )
        )

        fixture.given(
            UserCreatedEvent(
                userId = userId,
                email = "john.doe@example.com",
                firstName = "John",
                lastName = "Doe",
                role = UserRole.PROFESSOR
            )
        )
            .`when`(syncCommand)
            .expectEvents(
                UserSynchronizedWithKeycloakEvent(
                    userId = userId,
                    keycloakUserId = "keycloak-uuid",
                    keycloakData = mapOf(
                        "sub" to "keycloak-uuid",
                        "email" to "john.doe@example.com",
                        "name" to "John Doe"
                    )
                )
            )
    }
}