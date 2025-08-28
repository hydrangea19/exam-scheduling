package mk.ukim.finki.examscheduling.usermanagement.controller

import mk.ukim.finki.examscheduling.usermanagement.domain.command.CreateUserCommand
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.users.CreateEnhancedUserRequest
import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import mk.ukim.finki.examscheduling.usermanagement.domain.exceptions.UserDomainException
import mk.ukim.finki.examscheduling.usermanagement.query.UserActivitySummary
import mk.ukim.finki.examscheduling.usermanagement.query.UserPageResponse
import mk.ukim.finki.examscheduling.usermanagement.query.UserStatisticsResult
import mk.ukim.finki.examscheduling.usermanagement.query.UserView
import mk.ukim.finki.examscheduling.usermanagement.query.queries.*
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/api/test/axon")
class AxonTestController(
    private val commandGateway: CommandGateway,
    private val queryGateway: QueryGateway,
    private val eventStore: EventStore,
    private val axonHealthIndicator: HealthIndicator
) {

    private val logger = LoggerFactory.getLogger(AxonTestController::class.java)

    @PostMapping("/user/create")
    fun createEnhancedUser(@RequestBody request: CreateEnhancedUserRequest): ResponseEntity<Map<String, Any?>> {
        logger.info("Creating enhanced user via Axon: {}", request.email)

        return try {
            val userId = UUID.randomUUID()
            val command = CreateUserCommand(
                userId = userId,
                email = request.email,
                firstName = request.firstName,
                lastName = request.lastName,
                middleName = request.middleName,
                role = UserRole.valueOf(request.role.uppercase()),
                passwordHash = if (request.password?.isNotBlank() == true) "hashed_${request.password}" else null
            )

            val result: CompletableFuture<Any> = commandGateway.send(command)
            result.get()

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Enhanced user created successfully",
                    "userId" to userId.toString(),
                    "email" to request.email,
                    "role" to request.role
                )
            )
        } catch (e: UserDomainException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                mapOf("success" to false, "error" to "Validation failed", "message" to e.message)
            )
        } catch (e: Exception) {
            logger.error("Failed to create enhanced user: {}", request.email, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "error" to "User creation failed", "message" to e.message)
            )
        }
    }

    @GetMapping("/query/user/{userId}")
    fun findUserById(@PathVariable userId: String): ResponseEntity<Map<String, Any?>> {
        logger.info("Querying user by ID: {}", userId)

        return try {
            val query = FindUserByIdQuery(UUID.fromString(userId))
            val user: UserView? = queryGateway.query(query, UserView::class.java).get()

            if (user != null) {
                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "user" to user,
                        "message" to "User found"
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    mapOf(
                        "success" to false,
                        "message" to "User not found",
                        "userId" to userId
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to query user by ID: {}", userId, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "error" to "Query failed", "message" to e.message)
            )
        }
    }

    @GetMapping("/query/user/email/{email}")
    fun findUserByEmail(@PathVariable email: String): ResponseEntity<Map<String, Any?>> {
        logger.info("Querying user by email: {}", email)

        return try {
            val query = FindUserByEmailQuery(email)
            val user: UserView? = queryGateway.query(query, UserView::class.java).get()

            if (user != null) {
                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "user" to user,
                        "message" to "User found"
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    mapOf(
                        "success" to false,
                        "message" to "User not found",
                        "email" to email
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to query user by email: {}", email, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "error" to "Query failed", "message" to e.message)
            )
        }
    }

    @GetMapping("/query/users")
    fun findAllUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "fullName") sortBy: String,
        @RequestParam(defaultValue = "ASC") sortDirection: String
    ): ResponseEntity<Map<String, Any?>> {
        logger.info("Querying all users - page: {}, size: {}", page, size)

        return try {
            val query = FindAllUsersQuery(page, size, sortBy, sortDirection)
            val usersPage: UserPageResponse = queryGateway.query(
                query,
                ResponseTypes.instanceOf(UserPageResponse::class.java)
            ).get()

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "data" to usersPage
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to query all users", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "error" to "Query failed", "message" to e.message)
            )
        }
    }

    @GetMapping("/query/users/active")
    fun findActiveUsers(
        @RequestParam(defaultValue = "true") active: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Map<String, Any?>> {
        logger.info("Querying active users - active: {}", active)

        return try {
            val query = FindActiveUsersQuery(active, page, size)
            val usersPage: UserPageResponse = queryGateway.query(
                query,
                ResponseTypes.instanceOf(UserPageResponse::class.java)
            ).get()

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "users" to usersPage.users,
                    "filter" to mapOf("active" to active),
                    "pagination" to mapOf(
                        "page" to usersPage.page,
                        "size" to usersPage.size,
                        "totalPages" to usersPage.totalPages,
                        "totalElements" to usersPage.totalElements
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to query active users", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "error" to "Query failed", "message" to e.message)
            )
        }
    }

    @GetMapping("/query/users/role/{role}")
    fun findUsersByRole(
        @PathVariable role: String,
        @RequestParam(defaultValue = "true") activeOnly: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Map<String, Any?>> {
        logger.info("Querying users by role: {}", role)

        return try {
            val userRole = UserRole.valueOf(role.uppercase())
            val query = FindUsersByRoleQuery(userRole, activeOnly, page, size)
            val usersPage: UserPageResponse = queryGateway.query(
                query,
                ResponseTypes.instanceOf(UserPageResponse::class.java)
            ).get()

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "users" to usersPage.users,
                    "filter" to mapOf(
                        "role" to role,
                        "activeOnly" to activeOnly
                    ),
                    "pagination" to mapOf(
                        "page" to usersPage.page,
                        "size" to usersPage.size,
                        "totalPages" to usersPage.totalPages,
                        "totalElements" to usersPage.totalElements
                    )
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                mapOf(
                    "success" to false,
                    "error" to "Invalid role",
                    "message" to "Role must be one of: ${UserRole.values().joinToString(", ")}"
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to query users by role: {}", role, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "error" to "Query failed", "message" to e.message)
            )
        }
    }


    //TODO doesnt work need to fix
    @GetMapping("/query/users/search")
    fun searchUsers(
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) fullName: String?,
        @RequestParam(required = false) role: String?,
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "fullName") sortBy: String,
        @RequestParam(defaultValue = "ASC") sortDirection: String
    ): ResponseEntity<Map<String, Any?>> {
        logger.info(
            "Searching users with filters - email: {}, fullName: {}, role: {}, active: {}",
            email, fullName, role, active
        )

        return try {
            val userRole = role?.let { UserRole.valueOf(it.uppercase()) }
            val query = SearchUsersWithFiltersQuery(
                email, fullName, userRole, active, page, size, sortBy, sortDirection
            )

            val usersPage: UserPageResponse = queryGateway.query(
                query,
                ResponseTypes.instanceOf(UserPageResponse::class.java)
            ).get()

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "users" to usersPage.users,
                    "filters" to mapOf(
                        "email" to email,
                        "fullName" to fullName,
                        "role" to role,
                        "active" to active
                    ),
                    "pagination" to mapOf(
                        "page" to usersPage.page,
                        "size" to usersPage.size,
                        "totalPages" to usersPage.totalPages,
                        "totalElements" to usersPage.totalElements
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to search users", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "error" to "Search failed", "message" to e.message)
            )
        }
    }

    @GetMapping("/query/statistics")
    fun getUserStatistics(): ResponseEntity<Map<String, Any?>> {
        logger.info("Querying user statistics")

        return try {
            val query = GetUserStatisticsQuery(
                includeRoleBreakdown = true,
                includeLoginStats = true,
                includeKeycloakStats = true
            )

            val stats: UserStatisticsResult = queryGateway.query(query, UserStatisticsResult::class.java).get()

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "statistics" to stats,
                    "message" to "Statistics generated successfully"
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to generate user statistics", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "error" to "Statistics query failed", "message" to e.message)
            )
        }
    }

    @GetMapping("/query/user/{userId}/activity")
    fun getUserActivitySummary(@PathVariable userId: String): ResponseEntity<Map<String, Any?>> {
        logger.info("Querying activity summary for user: {}", userId)

        return try {
            val query = FindUserActivitySummaryQuery(
                userId = UUID.fromString(userId),
                includeLoginHistory = true,
                includeRoleHistory = true
            )

            val summary: UserActivitySummary? = queryGateway.query(query, UserActivitySummary::class.java).get()

            if (summary != null) {
                ResponseEntity.ok(
                    mapOf(
                        "success" to true,
                        "activitySummary" to summary,
                        "message" to "Activity summary generated"
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    mapOf(
                        "success" to false,
                        "message" to "User not found",
                        "userId" to userId
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to generate activity summary for user: {}", userId, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "error" to "Activity query failed", "message" to e.message)
            )
        }
    }


    //TODO doesnt work need to fix
    @GetMapping("/query/users/recent")
    fun findRecentUsers(
        @RequestParam(defaultValue = "24") hours: Long,
        @RequestParam(defaultValue = "created") type: String
    ): ResponseEntity<Map<String, Any?>> {
        logger.info("Querying recent users - type: {}, hours: {}", type, hours)

        return try {
            val since = Instant.now().minus(hours, ChronoUnit.HOURS)

            val users: List<UserView> = when (type.lowercase()) {
                "created" -> queryGateway.query(
                    FindUsersCreatedBetweenQuery(since, Instant.now()),
                    ResponseTypes.multipleInstancesOf(UserView::class.java)
                ).join()

                "updated" -> queryGateway.query(
                    FindUsersUpdatedAfterQuery(since),
                    ResponseTypes.multipleInstancesOf(UserView::class.java)
                ).join()

                "role-changed" -> queryGateway.query(
                    FindRecentRoleChangesQuery(since),
                    ResponseTypes.multipleInstancesOf(UserView::class.java)
                ).join()

                "deactivated" -> queryGateway.query(
                    FindRecentlyDeactivatedUsersQuery(since),
                    ResponseTypes.multipleInstancesOf(UserView::class.java)
                ).join()

                else -> return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    mapOf(
                        "success" to false,
                        "error" to "Invalid type",
                        "message" to "Type must be one of: created, updated, role-changed, deactivated"
                    )
                )
            }

            return ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "users" to users,
                    "count" to users.size,
                    "criteria" to mapOf("type" to type, "since" to since.toString(), "hours" to hours)
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to query recent users", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "error" to "Recent users query failed", "message" to e.message)
            )
        }
    }

    @GetMapping("/health")
    fun axonHealth(): ResponseEntity<Map<String, Any>> {
        val health = axonHealthIndicator.health()
        return ResponseEntity.ok(
            mapOf(
                "status" to health.status.code,
                "details" to health.details
            )
        )
    }

    @GetMapping("/events/{userId}")
    fun getUserEvents(@PathVariable userId: String): ResponseEntity<Map<String, Any?>> {
        return try {
            val events = eventStore.readEvents(userId).asSequence().toList()

            val eventSummary = events.map { domainEventMessage ->
                mapOf(
                    "eventId" to domainEventMessage.identifier,
                    "eventType" to domainEventMessage.payloadType.simpleName,
                    "timestamp" to domainEventMessage.timestamp.toString(),
                    "aggregateId" to domainEventMessage.aggregateIdentifier,
                    "sequenceNumber" to domainEventMessage.sequenceNumber,
                    "payload" to domainEventMessage.payload
                )
            }

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "userId" to userId,
                    "eventCount" to eventSummary.size,
                    "events" to eventSummary
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to retrieve events for user: {}", userId, e)
            ResponseEntity.status(500).body(
                mapOf("success" to false, "error" to "Failed to retrieve events", "message" to e.message)
            )
        }
    }

    @GetMapping("/info")
    fun axonInfo(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "message" to "Complete CQRS Axon Framework test endpoints",
                "commandEndpoints" to listOf(
                    "POST /api/test/axon/user/create - Create user",
                    "PUT /api/test/axon/user/{userId}/profile - Update profile",
                    "PUT /api/test/axon/user/{userId}/email - Change email",
                    "PUT /api/test/axon/user/{userId}/role - Change role",
                    "PUT /api/test/axon/user/{userId}/activate - Activate user",
                    "PUT /api/test/axon/user/{userId}/deactivate - Deactivate user",
                    "PUT /api/test/axon/user/{userId}/preferences - Update preferences"
                ),
                "queryEndpoints" to listOf(
                    "GET /api/test/axon/query/user/{userId} - Find user by ID",
                    "GET /api/test/axon/query/user/email/{email} - Find user by email",
                    "GET /api/test/axon/query/users - Find all users (paginated)",
                    "GET /api/test/axon/query/users/active - Find active/inactive users",
                    "GET /api/test/axon/query/users/role/{role} - Find users by role",
                    "GET /api/test/axon/query/users/search - Search users with filters",
                    "GET /api/test/axon/query/statistics - Get user statistics",
                    "GET /api/test/axon/query/user/{userId}/activity - Get user activity",
                    "GET /api/test/axon/query/users/recent - Find recent users"
                ),
                "utilityEndpoints" to listOf(
                    "GET /api/test/axon/health - Check Axon health",
                    "GET /api/test/axon/events/{userId} - Get user events",
                    "GET /api/test/axon/info - This endpoint"
                ),
                "note" to "Complete CQRS implementation with command/query separation"
            )
        )
    }
}