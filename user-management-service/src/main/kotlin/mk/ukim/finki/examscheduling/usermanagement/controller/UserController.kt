package mk.ukim.finki.examscheduling.usermanagement.controller

import jakarta.validation.Valid
import jakarta.validation.ValidationException
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.ws.rs.InternalServerErrorException
import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import mk.ukim.finki.examscheduling.usermanagement.domain.command.*
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.users.*
import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import mk.ukim.finki.examscheduling.usermanagement.domain.exceptions.UserDomainException
import mk.ukim.finki.examscheduling.usermanagement.query.repository.UserViewRepository
import org.axonframework.commandhandling.gateway.CommandGateway
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*


@RestController
@RequestMapping("/api/users")
@Validated
class UserController(
    private val commandGateway: CommandGateway,
    private val userViewRepository: UserViewRepository
) {

    private val logger = LoggerFactory.getLogger(UserController::class.java)

    // ===== USER CREATION =====

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {

        logger.info("Creating user: {} by admin: {}", request.email, authentication.name)

        return try {
            val userId = UUID.randomUUID().toString()
            val command = CreateUserCommand(
                userId = userId,
                email = request.email,
                firstName = request.firstName,
                lastName = request.lastName,
                middleName = request.middleName,
                role = UserRole.valueOf(request.role.uppercase()),
                passwordHash = null
            )

            commandGateway.sendAndWait<Any>(command)
            logger.info("User created successfully: {} with ID: {}", request.email, userId)

            return ResponseEntity.accepted().body(mapOf("userId" to userId))
        } catch (e: UserDomainException) {
            logger.warn("User creation validation failed: {}", e.message)
            throw ValidationException("User creation failed: ${e.message}")
        } catch (e: Exception) {
            logger.error("Failed to create user: {}", request.email, e)
            throw InternalServerErrorException("Failed to create user: ${e.message}")
        }
    }

    // ===== USER RETRIEVAL =====

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PROFESSOR') and @securityService.canAccessUser(authentication, #userId))")
    fun getUserById(
        @PathVariable userId: String,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {

        logger.debug("Fetching user by ID: {} requested by: {}", userId, authentication.name)

        return try {
            val userView = userViewRepository.findById(userId).orElse(null)

            if (userView != null) {
                ResponseEntity.ok(UserResponse.fromUserView(userView))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch user by ID: {}", userId, e)
            throw InternalServerErrorException("Failed to fetch user")
        }
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserByEmail(
        @PathVariable email: String,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {

        logger.debug("Fetching user by email: {} requested by: {}", email, authentication.name)

        return try {
            val userView = userViewRepository.findByEmail(email)

            if (userView != null) {
                ResponseEntity.ok(UserResponse.fromUserView(userView))
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch user by email: {}", email, e)
            throw InternalServerErrorException("Failed to fetch user")
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUsers(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "fullName") sortBy: String,
        @RequestParam(defaultValue = "ASC") sortDirection: String,
        authentication: Authentication
    ): ResponseEntity<PagedUserResponse> {

        logger.debug("Fetching all users - page: {}, size: {} by: {}", page, size, authentication.name)

        return try {
            val sort = Sort.by(
                if (sortDirection.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC,
                sortBy
            )
            val pageable = PageRequest.of(page, size, sort)
            val userPage = userViewRepository.findAll(pageable)

            val response = PagedUserResponse(
                users = userPage.content.map { UserResponse.fromUserView(it) },
                page = userPage.number,
                size = userPage.size,
                totalPages = userPage.totalPages,
                totalElements = userPage.totalElements,
                first = userPage.isFirst,
                last = userPage.isLast,
                empty = userPage.isEmpty
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Failed to fetch all users", e)
            throw InternalServerErrorException("Failed to fetch users")
        }
    }

    // ===== USER SEARCH & FILTERING  =====

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    fun searchUsers(
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) fullName: String?,
        @RequestParam(required = false) role: String?,
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "fullName") sortBy: String,
        @RequestParam(defaultValue = "ASC") sortDirection: String,
        authentication: Authentication
    ): ResponseEntity<PagedUserResponse> {

        logger.debug("Searching users with filters by: {}", authentication.name)

        return try {
            val userRole = role?.let { UserRole.valueOf(it.uppercase()) }
            val sort = Sort.by(
                if (sortDirection.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC,
                sortBy
            )
            val pageable = PageRequest.of(page, size, sort)

            val userPage = userViewRepository.findUsersWithFilters(
                email = email?.lowercase(),
                fullName = fullName?.lowercase(),
                role = userRole,
                active = active,
                pageable = pageable
            )

            val response = PagedUserResponse(
                users = userPage.content.map { UserResponse.fromUserView(it) },
                page = userPage.number,
                size = userPage.size,
                totalPages = userPage.totalPages,
                totalElements = userPage.totalElements,
                first = userPage.isFirst,
                last = userPage.isLast,
                empty = userPage.isEmpty
            )

            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            throw ValidationException("Invalid role: $role. Valid roles: ${UserRole.values().joinToString(", ")}")
        } catch (e: Exception) {
            logger.error("Failed to search users", e)
            throw InternalServerErrorException("Failed to search users")
        }
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUsersByRole(
        @PathVariable role: String,
        @RequestParam(defaultValue = "true") activeOnly: Boolean,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        authentication: Authentication
    ): ResponseEntity<PagedUserResponse> {

        logger.debug("Fetching users by role: {} by: {}", role, authentication.name)

        return try {
            val userRole = UserRole.valueOf(role.uppercase())
            val pageable = PageRequest.of(page, size)

            val userPage = if (activeOnly) {
                userViewRepository.findByRoleOrderByFullNameAsc(userRole, pageable)
            } else {
                val allUsers = userViewRepository.findByRole(userRole)
                val startIndex = (page * size).coerceAtMost(allUsers.size)
                val endIndex = ((page + 1) * size).coerceAtMost(allUsers.size)
                val pageUsers = if (startIndex < allUsers.size) allUsers.subList(startIndex, endIndex) else emptyList()
                PageImpl(pageUsers, pageable, allUsers.size.toLong())
            }

            val response = PagedUserResponse(
                users = userPage.content.map { UserResponse.fromUserView(it) },
                page = userPage.number,
                size = userPage.size,
                totalPages = userPage.totalPages,
                totalElements = userPage.totalElements,
                first = userPage.isFirst,
                last = userPage.isLast,
                empty = userPage.isEmpty
            )

            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            throw ValidationException("Invalid role: $role. Valid roles: ${UserRole.values().joinToString(", ")}")
        } catch (e: Exception) {
            logger.error("Failed to fetch users by role: {}", role, e)
            throw InternalServerErrorException("Failed to fetch users by role")
        }
    }

    // ===== USER PROFILE MANAGEMENT =====

    @PutMapping("/{userId}/profile")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PROFESSOR') and @securityService.canAccessUser(authentication, #userId))")
    fun updateUserProfile(
        @PathVariable userId: String,
        @Valid @RequestBody request: UpdateUserProfileRequest,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {

        logger.info("Updating profile for user: {} by: {}", userId, authentication.name)

        return try {
            val command = UpdateUserProfileCommand(
                userId = userId,
                firstName = request.firstName,
                lastName = request.lastName,
                middleName = request.middleName
            )

            commandGateway.sendAndWait<Any>(command)
            logger.info("User profile updated successfully: {}", userId)

            val userView = userViewRepository.findById(userId).orElseThrow {
                IllegalStateException("User not found after update")
            }

            ResponseEntity.ok(UserResponse.fromUserView(userView))
        } catch (e: UserDomainException) {
            logger.warn("User profile update validation failed: {}", e.message)
            throw ValidationException("Profile update failed: ${e.message}")
        } catch (e: Exception) {
            logger.error("Failed to update user profile: {}", userId, e)
            throw InternalServerErrorException("Failed to update user profile")
        }
    }

    @PutMapping("/{userId}/email")
    @PreAuthorize("hasRole('ADMIN')")
    fun changeUserEmail(
        @PathVariable userId: String,
        @Valid @RequestBody request: ChangeUserEmailRequest,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {

        logger.info("Changing email for user: {} by admin: {}", userId, authentication.name)

        return try {
            val command = ChangeUserEmailCommand(
                userId = userId,
                newEmail = request.newEmail,
                oldEmail = request.currentEmail
            )

            commandGateway.sendAndWait<Any>(command)
            logger.info("User email changed successfully: {}", userId)

            val userView = userViewRepository.findById(userId).orElseThrow {
                IllegalStateException("User not found after email change")
            }

            ResponseEntity.ok(UserResponse.fromUserView(userView))
        } catch (e: UserDomainException) {
            logger.warn("User email change validation failed: {}", e.message)
            throw ValidationException("Email change failed: ${e.message}")
        } catch (e: Exception) {
            logger.error("Failed to change user email: {}", userId, e)
            throw InternalServerErrorException("Failed to change user email")
        }
    }

    // ===== USER ROLE MANAGEMENT  =====

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    fun changeUserRole(
        @PathVariable userId: String,
        @Valid @RequestBody request: ChangeUserRoleRequest,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {

        val adminUserId = SecurityUtils.getCurrentUserId()
            ?: throw SecurityException("Unable to determine admin user ID")

        logger.info("Changing role for user: {} by admin: {}", userId, authentication.name)

        return try {
            val newRole = UserRole.valueOf(request.newRole.uppercase())
            val previousRole = UserRole.valueOf(request.currentRole.uppercase())

            val command = ChangeUserRoleCommand(
                userId = userId,
                newRole = newRole,
                previousRole = previousRole,
                changedBy = adminUserId,
                reason = request.reason
            )

            commandGateway.sendAndWait<Any>(command)
            logger.info("User role changed successfully: {} from {} to {}", userId, previousRole, newRole)

            val userView = userViewRepository.findById(userId).orElseThrow {
                IllegalStateException("User not found after role change")
            }

            ResponseEntity.ok(UserResponse.fromUserView(userView))
        } catch (e: IllegalArgumentException) {
            throw ValidationException("Invalid role values. Valid roles: ${UserRole.values().joinToString(", ")}")
        } catch (e: UserDomainException) {
            logger.warn("User role change validation failed: {}", e.message)
            throw ValidationException("Role change failed: ${e.message}")
        } catch (e: Exception) {
            logger.error("Failed to change user role: {}", userId, e)
            throw InternalServerErrorException("Failed to change user role")
        }
    }

    // ===== USER ACTIVATION/DEACTIVATION  =====

    @PutMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    fun activateUser(
        @PathVariable userId: String,
        @RequestBody(required = false) request: UserActivateRequest?,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {

        val adminUserId = SecurityUtils.getCurrentUserId()
        logger.info("Activating user: {} by admin: {}", userId, authentication.name)

        return try {
            val command = ActivateUserCommand(
                userId = userId,
                activatedBy = adminUserId,
                reason = request?.reason
            )

            commandGateway.sendAndWait<Any>(command)
            logger.info("User activated successfully: {}", userId)

            val userView = userViewRepository.findById(userId).orElseThrow {
                IllegalStateException("User not found after activation")
            }

            ResponseEntity.ok(UserResponse.fromUserView(userView))
        } catch (e: UserDomainException) {
            logger.warn("User activation failed: {}", e.message)
            throw ValidationException("User activation failed: ${e.message}")
        } catch (e: Exception) {
            logger.error("Failed to activate user: {}", userId, e)
            throw InternalServerErrorException("Failed to activate user")
        }
    }

    @PutMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    fun deactivateUser(
        @PathVariable userId: String,
        @Valid @RequestBody request: DeactivateUserRequest,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {

        val adminUserId = SecurityUtils.getCurrentUserId()
            ?: throw SecurityException("Unable to determine admin user ID")

        logger.info("Deactivating user: {} by admin: {}", userId, authentication.name)

        return try {
            val command = DeactivateUserCommand(
                userId = userId,
                deactivatedBy = adminUserId,
                reason = request.reason
            )

            commandGateway.sendAndWait<Any>(command)
            logger.info("User deactivated successfully: {}", userId)

            val userView = userViewRepository.findById(userId).orElseThrow {
                IllegalStateException("User not found after deactivation")
            }

            ResponseEntity.ok(UserResponse.fromUserView(userView))
        } catch (e: UserDomainException) {
            logger.warn("User deactivation validation failed: {}", e.message)
            throw ValidationException("User deactivation failed: ${e.message}")
        } catch (e: Exception) {
            logger.error("Failed to deactivate user: {}", userId, e)
            throw InternalServerErrorException("Failed to deactivate user")
        }
    }

    // ===== USER PREFERENCES  =====

    @PutMapping("/{userId}/preferences")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PROFESSOR') and @securityService.canAccessUser(authentication, #userId))")
    fun updateUserPreferences(
        @PathVariable userId: String,
        @Valid @RequestBody request: UpdateUserPreferencesRequest,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {

        logger.info("Updating preferences for user: {} by: {}", userId, authentication.name)

        return try {
            val command = UpdateUserPreferencesCommand(
                userId = userId,
                notificationPreferences = request.notificationPreferences,
                uiPreferences = request.uiPreferences
            )

            commandGateway.sendAndWait<Any>(command)
            logger.info("User preferences updated successfully: {}", userId)

            val userView = userViewRepository.findById(userId).orElseThrow {
                IllegalStateException("User not found after preferences update")
            }

            ResponseEntity.ok(UserResponse.fromUserView(userView))
        } catch (e: UserDomainException) {
            logger.warn("User preferences update validation failed: {}", e.message)
            throw ValidationException("Preferences update failed: ${e.message}")
        } catch (e: Exception) {
            logger.error("Failed to update user preferences: {}", userId, e)
            throw InternalServerErrorException("Failed to update user preferences")
        }
    }

    // ===== CURRENT USER  =====

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUserProfile(authentication: Authentication): ResponseEntity<UserResponse> {
        logger.debug("Fetching current user profile for: {}", authentication.name)

        return try {
            val currentUserId = SecurityUtils.getCurrentUserId()
                ?: throw SecurityException("Unable to determine current user ID")

            val userView = userViewRepository.findById(currentUserId).orElse(null)

            if (userView != null) {
                ResponseEntity.ok(UserResponse.fromUserView(userView))
            } else {
                throw IllegalStateException("Authenticated user not found in system")
            }
        } catch (e: SecurityException) {
            logger.error("Security error fetching current user profile", e)
            throw e
        } catch (e: Exception) {
            logger.error("Failed to fetch current user profile", e)
            throw InternalServerErrorException("Failed to fetch current user profile")
        }
    }

    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    fun updateCurrentUserProfile(
        @Valid @RequestBody request: UpdateUserProfileRequest,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {

        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: throw SecurityException("Unable to determine current user ID")

        logger.info("Updating current user profile for: {}", authentication.name)

        return updateUserProfile(currentUserId, request, authentication)
    }

    @PutMapping("/me/preferences")
    @PreAuthorize("isAuthenticated()")
    fun updateCurrentUserPreferences(
        @Valid @RequestBody request: UpdateUserPreferencesRequest,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {

        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: throw SecurityException("Unable to determine current user ID")

        logger.info("Updating current user preferences for: {}", authentication.name)

        return updateUserPreferences(currentUserId, request, authentication)
    }

    // ===== STATISTICS & REPORTING  =====

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserStatistics(authentication: Authentication): ResponseEntity<UserStatisticsResponse> {
        logger.debug("Fetching user statistics by: {}", authentication.name)

        return try {
            val row = userViewRepository.getUserStatistics() as Array<*>

            val activeUsers = (row[0] as Number).toLong()
            val inactiveUsers = (row[1] as Number).toLong()
            val usersWithPassword = (row[2] as Number).toLong()
            val keycloakUsers = (row[3] as Number).toLong()
            val usersWithLogin = (row[4] as Number).toLong()

            val roleBreakdown = userViewRepository.countActiveUsersByRole()
                .associate { r -> (r[0] as UserRole).name to (r[1] as Number).toLong() }

            val response = UserStatisticsResponse(
                totalUsers = activeUsers + inactiveUsers,
                activeUsers = activeUsers,
                inactiveUsers = inactiveUsers,
                usersWithPassword = usersWithPassword,
                keycloakUsers = keycloakUsers,
                usersWithRecentLogin = usersWithLogin,
                roleBreakdown = roleBreakdown,
                generatedAt = Instant.now()
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Failed to fetch user statistics", e)
            throw InternalServerErrorException("Failed to fetch user statistics")
        }
    }

    // ===== DEBUG ENDPOINT  =====

    @GetMapping("/direct-db-test")
    @PreAuthorize("hasRole('ADMIN')")
    fun testDirectDb(): ResponseEntity<Any> {
        return try {
            val users = userViewRepository.findAll()
            ResponseEntity.ok(
                mapOf(
                    "status" to "SUCCESS",
                    "message" to "Direct database access working",
                    "count" to users.size,
                    "sample_users" to users.take(3).map { "${it.email} - ${it.fullName}" },
                    "method" to "Direct Repository Access"
                )
            )
        } catch (e: Exception) {
            logger.error("Direct DB test failed", e)
            ResponseEntity.status(500).body(
                mapOf(
                    "status" to "ERROR",
                    "message" to "DB Error: ${e.message}"
                )
            )
        }
    }
}