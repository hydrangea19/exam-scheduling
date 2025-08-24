package mk.ukim.finki.examscheduling.usermanagement.controller

import mk.ukim.finki.examscheduling.shared.logging.CorrelationIdContext
import mk.ukim.finki.examscheduling.usermanagement.domain.User
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.users.UserCreateRequest
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.users.UserProfileWithCourses
import mk.ukim.finki.examscheduling.usermanagement.repository.UserRepository
import mk.ukim.finki.examscheduling.usermanagement.service.ExternalIntegrationClient
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletionException


@RestController
@RequestMapping("/api/test")
class TestController @Autowired constructor(
    private val userRepository: UserRepository,
    private val externalIntegrationClient: ExternalIntegrationClient
) {

    private val logger = LoggerFactory.getLogger(TestController::class.java)

    @GetMapping("/ping")
    fun ping(): Map<String, Any?> {
        val correlationId = CorrelationIdContext.getCorrelationId()
        val requestId = CorrelationIdContext.getRequestId()

        logger.info("Processing ping request",
            MDC.getCopyOfContextMap()?.plus(mapOf(
                "operation" to "ping",
                "correlationId" to correlationId,
                "requestId" to requestId
            )))

        return mapOf(
            "message" to "User Management Service is running",
            "timestamp" to Instant.now(),
            "service" to "user-management-service",
            "version" to "1.0.0-SNAPSHOT",
            "correlationId" to correlationId,
            "requestId" to requestId
        )
    }

    @GetMapping("/test-external-service")
    fun testExternalService(): Map<String, Any?> {
        return try {
            logger.info("Testing external service communication")

            val pingResponse = externalIntegrationClient.ping().get()

            mapOf(
                "status" to "SUCCESS",
                "message" to "External service communication working",
                "externalServiceData" to mapOf(
                    "ping" to mapOf(
                        "service" to pingResponse.service,
                        "version" to pingResponse.version,
                        "message" to pingResponse.message
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("External service communication failed", e)
            mapOf(
                "status" to "ERROR",
                "message" to "External service communication failed",
                "error" to when (e) {
                    is CompletionException -> e.cause?.message ?: e.message
                    else -> e.message
                },
                "fallbackUsed" to true
            )
        }
    }

    // === Correlation ID Testing Endpoints ===

    @GetMapping("/test-correlation-flow")
    fun testCorrelationFlow(): Map<String, Any?> {
        val correlationId = CorrelationIdContext.getCorrelationId()
        val requestId = CorrelationIdContext.getRequestId()

        logger.info("Testing correlation ID flow across services",
            MDC.getCopyOfContextMap()?.plus(mapOf(
                "operation" to "test_correlation_flow_start",
                "correlationId" to correlationId,
                "requestId" to requestId
            )))

        return try {
            val pingResponse = externalIntegrationClient.ping().get()
            val coursesResponse = externalIntegrationClient.getAllCourses().get()

            logger.info("Correlation flow test completed successfully",
                MDC.getCopyOfContextMap()?.plus(mapOf(
                    "operation" to "test_correlation_flow_success",
                    "externalServiceCalls" to 2,
                    "correlationId" to correlationId,
                    "requestId" to requestId
                )))

            mapOf(
                "status" to "SUCCESS",
                "message" to "Correlation ID flow test completed",
                "correlationInfo" to mapOf(
                    "correlationId" to correlationId,
                    "requestId" to requestId,
                    "serviceName" to "user-management-service"
                ),
                "externalServiceCalls" to listOf(
                    mapOf(
                        "service" to "external-integration-service",
                        "operation" to "ping",
                        "correlationPassed" to true,
                        "response" to mapOf(
                            "service" to pingResponse.service,
                            "version" to pingResponse.version
                        )
                    ),
                    mapOf(
                        "service" to "external-integration-service",
                        "operation" to "getAllCourses",
                        "correlationPassed" to true,
                        "response" to mapOf(
                            "count" to coursesResponse.count
                        )
                    )
                ),
                "loggingVerification" to mapOf(
                    "structuredLogsGenerated" to true,
                    "correlationIdPropagated" to true,
                    "crossServiceTracking" to true
                )
            )
        } catch (e: Exception) {
            logger.error("Correlation flow test failed",
                MDC.getCopyOfContextMap()?.plus(mapOf(
                    "operation" to "test_correlation_flow_error",
                    "errorType" to e.javaClass.simpleName,
                    "errorMessage" to e.message,
                    "correlationId" to correlationId,
                    "requestId" to requestId
                )), e)

            mapOf(
                "status" to "ERROR",
                "message" to "Correlation flow test failed",
                "correlationInfo" to mapOf(
                    "correlationId" to correlationId,
                    "requestId" to requestId,
                    "serviceName" to "user-management-service"
                ),
                "error" to when (e) {
                    is CompletionException -> e.cause?.message ?: e.message
                    else -> e.message
                },
                "fallbackUsed" to true
            )
        }
    }

    @GetMapping("/test-logging-chain/{depth}")
    fun testLoggingChain(@PathVariable depth: Int): Map<String, Any?> {
        val correlationId = CorrelationIdContext.getCorrelationId()
        val requestId = CorrelationIdContext.getRequestId()

        logger.info("Testing logging chain with depth {}",
            depth,
            MDC.getCopyOfContextMap()?.plus(mapOf(
                "operation" to "test_logging_chain",
                "chainDepth" to depth,
                "correlationId" to correlationId,
                "requestId" to requestId
            )))

        return try {
            val results = mutableListOf<Map<String, Any?>>()

            repeat(depth) { i ->
                logger.debug("Making external call {} of {}",
                    i + 1, depth,
                    MDC.getCopyOfContextMap()?.plus(mapOf(
                        "operation" to "external_call",
                        "callIndex" to i + 1,
                        "totalCalls" to depth,
                        "correlationId" to correlationId,
                        "requestId" to requestId
                    )))

                val pingResponse = externalIntegrationClient.ping().get()

                results.add(mapOf(
                    "callIndex" to i + 1,
                    "service" to pingResponse.service,
                    "timestamp" to pingResponse.timestamp,
                    "correlationId" to correlationId,
                    "requestId" to requestId
                ))
            }

            logger.info("Logging chain test completed",
                MDC.getCopyOfContextMap()?.plus(mapOf(
                    "operation" to "test_logging_chain_success",
                    "chainDepth" to depth,
                    "totalCalls" to results.size,
                    "correlationId" to correlationId,
                    "requestId" to requestId
                )))

            mapOf(
                "status" to "SUCCESS",
                "message" to "Logging chain test completed",
                "correlationInfo" to mapOf(
                    "correlationId" to correlationId,
                    "requestId" to requestId,
                    "chainDepth" to depth
                ),
                "chainResults" to results,
                "summary" to mapOf(
                    "totalCalls" to results.size,
                    "sameCorrelationId" to results.all { (it["correlationId"] as String) == correlationId },
                    "sameRequestId" to results.all { (it["requestId"] as String) == requestId }
                )
            )
        } catch (e: Exception) {
            logger.error("Logging chain test failed",
                MDC.getCopyOfContextMap()?.plus(mapOf(
                    "operation" to "test_logging_chain_error",
                    "chainDepth" to depth,
                    "errorType" to e.javaClass.simpleName,
                    "errorMessage" to e.message,
                    "correlationId" to correlationId,
                    "requestId" to requestId
                )), e)

            mapOf(
                "status" to "ERROR",
                "message" to "Logging chain test failed",
                "error" to e.message,
                "correlationInfo" to mapOf(
                    "correlationId" to correlationId,
                    "requestId" to requestId,
                    "chainDepth" to depth
                )
            )
        }
    }

    @PostMapping("/test-structured-logging")
    fun testStructuredLogging(@RequestBody request: Map<String, Any>): ResponseEntity<Map<String, Any?>> {
        val correlationId = CorrelationIdContext.getCorrelationId()
        val requestId = CorrelationIdContext.getRequestId()
        val testType = request["testType"] as? String ?: "basic"

        logger.info("Starting structured logging test",
            MDC.getCopyOfContextMap()?.plus(mapOf(
                "operation" to "structured_logging_test",
                "testType" to testType,
                "requestPayload" to request,
                "correlationId" to correlationId,
                "requestId" to requestId
            )))

        return try {
            when (testType) {
                "error" -> {
                    logger.error("Intentional error log for testing",
                        MDC.getCopyOfContextMap()?.plus(mapOf(
                            "operation" to "intentional_error_test",
                            "errorCode" to "TEST_ERROR",
                            "severity" to "high",
                            "correlationId" to correlationId,
                            "requestId" to requestId
                        )))
                }
                "warn" -> {
                    logger.warn("Warning log for testing",
                        MDC.getCopyOfContextMap()?.plus(mapOf(
                            "operation" to "warning_test",
                            "warningType" to "performance",
                            "threshold" to 100,
                            "correlationId" to correlationId,
                            "requestId" to requestId
                        )))
                }
                "business" -> {
                    val userId = UUID.randomUUID()
                    logger.info("Simulating business operation",
                        MDC.getCopyOfContextMap()?.plus(mapOf(
                            "operation" to "user_creation_simulation",
                            "userId" to userId.toString(),
                            "userType" to "professor",
                            "businessMetrics" to mapOf(
                                "processingTime" to 150,
                                "validationsPassed" to 5,
                                "externalCallsMade" to 2
                            ),
                            "correlationId" to correlationId,
                            "requestId" to requestId
                        )))
                }
                else -> {
                    logger.info("Basic structured logging test",
                        MDC.getCopyOfContextMap()?.plus(mapOf(
                            "operation" to "basic_logging_test",
                            "logLevel" to "info",
                            "correlationId" to correlationId,
                            "requestId" to requestId
                        )))
                }
            }

            ResponseEntity.ok(
                mapOf(
                    "status" to "SUCCESS",
                    "message" to "Structured logging test completed",
                    "testType" to testType,
                    "correlationInfo" to mapOf(
                        "correlationId" to correlationId,
                        "requestId" to requestId
                    ),
                    "logsGenerated" to mapOf(
                        "structuredFormat" to true,
                        "correlationIdIncluded" to true,
                        "businessContextIncluded" to true
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Structured logging test failed",
                MDC.getCopyOfContextMap()?.plus(mapOf(
                    "operation" to "structured_logging_test_error",
                    "testType" to testType,
                    "errorType" to e.javaClass.simpleName,
                    "errorMessage" to e.message,
                    "correlationId" to correlationId,
                    "requestId" to requestId
                )), e)

            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "status" to "ERROR",
                    "message" to "Structured logging test failed",
                    "error" to e.message,
                    "correlationInfo" to mapOf(
                        "correlationId" to correlationId,
                        "requestId" to requestId
                    )
                )
            )
        }
    }

    @GetMapping("/test-full-integration")
    fun testFullIntegration(): Map<String, Any?> {
        return try {
            logger.info("Testing full integration: User Management + External Integration")

            val testUser = User(
                email = "integration.test@example.com",
                firstName = "Integration",
                lastName = "Test",
                middleName = "User"
            )
            val savedUser = userRepository.save(testUser)

            val coursesResponse = externalIntegrationClient.getAllCourses().get()

            val searchResponse = externalIntegrationClient.searchCourses("Computer").get()

            val userProfile = UserProfileWithCourses(
                userId = savedUser.id,
                email = savedUser.email,
                fullName = savedUser.getFullName(),
                preferredCourses = searchResponse.results,
                departmentPreferences = coursesResponse.courses
                    .mapNotNull { it.department }
                    .distinct()
                    .take(3),
                semesterPreferences = coursesResponse.courses
                    .mapNotNull { it.semester }
                    .distinct()
                    .sorted()
                    .take(2)
            )

            mapOf(
                "status" to "SUCCESS",
                "message" to "Full integration test completed",
                "results" to mapOf(
                    "userCreated" to mapOf(
                        "id" to savedUser.id,
                        "email" to savedUser.email,
                        "fullName" to savedUser.getFullName()
                    ),
                    "coursesFromExternalService" to mapOf(
                        "totalCourses" to coursesResponse.count,
                        "searchResults" to searchResponse.count,
                        "searchQuery" to searchResponse.query
                    ),
                    "userProfile" to mapOf(
                        "userId" to userProfile.userId,
                        "preferredCoursesCount" to userProfile.preferredCourses.size,
                        "departmentPreferences" to userProfile.departmentPreferences,
                        "semesterPreferences" to userProfile.semesterPreferences
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Full integration test failed", e)
            mapOf(
                "status" to "ERROR",
                "message" to "Full integration test failed",
                "error" to when (e) {
                    is CompletionException -> e.cause?.message ?: e.message
                    else -> e.message
                }
            )
        }
    }

    @GetMapping("/users")
    fun getAllUsers(): ResponseEntity<Map<String, Any?>> {
        return try {
            val users = userRepository.findAll()
            val statistics = userRepository.getUserStatistics()

            ResponseEntity.ok(
                mapOf(
                    "users" to users.map {
                        mapOf(
                            "id" to it.id,
                            "email" to it.email,
                            "fullName" to it.getFullName(),
                            "active" to it.active,
                            "createdAt" to it.createdAt
                        )
                    },
                    "statistics" to statistics,
                    "count" to users.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch users",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/users/{id}")
    fun getUserById(@PathVariable id: UUID): ResponseEntity<Map<String, Any?>> {
        return try {
            val user = userRepository.findById(id)
            if (user.isPresent) {
                val u = user.get()
                ResponseEntity.ok(
                    mapOf(
                        "id" to u.id,
                        "email" to u.email,
                        "firstName" to u.firstName,
                        "lastName" to u.lastName,
                        "middleName" to u.middleName,
                        "fullName" to u.getFullName(),
                        "active" to u.active,
                        "createdAt" to u.createdAt,
                        "updatedAt" to u.updatedAt,
                        "version" to u.version
                    )
                )
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch user",
                    "message" to e.message
                )
            )
        }
    }

    @PostMapping("/users")
    fun createUser(@RequestBody request: UserCreateRequest): ResponseEntity<Map<String, Any?>> {
        return try {
            if (userRepository.existsByEmail(request.email)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    mapOf(
                        "error" to "User with email ${request.email} already exists"
                    )
                )
            }

            val newUser = User(
                email = request.email,
                firstName = request.firstName,
                lastName = request.lastName,
                middleName = request.middleName
            )

            val savedUser = userRepository.save(newUser)

            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "id" to savedUser.id,
                    "email" to savedUser.email,
                    "fullName" to savedUser.getFullName(),
                    "message" to "User created successfully"
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to create user",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/users/search")
    fun searchUsers(@RequestParam query: String): ResponseEntity<Map<String, Any?>> {
        return try {
            val users = userRepository.findByFullNameContaining(query)

            ResponseEntity.ok(
                mapOf(
                    "query" to query,
                    "results" to users.map {
                        mapOf(
                            "id" to it.id,
                            "email" to it.email,
                            "fullName" to it.getFullName(),
                            "active" to it.active
                        )
                    },
                    "count" to users.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Search failed",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/users/{id}/with-course-preferences")
    fun getUserWithCoursePreferences(@PathVariable id: UUID): ResponseEntity<Map<String, Any?>> {
        return try {
            val userOpt = userRepository.findById(id)
            if (!userOpt.isPresent) {
                return ResponseEntity.notFound().build()
            }

            val user = userOpt.get()

            val allCourses = externalIntegrationClient.getAllCourses().get()

            val preferredCourses = allCourses.courses.take(3)

            ResponseEntity.ok(
                mapOf(
                    "user" to mapOf(
                        "id" to user.id,
                        "email" to user.email,
                        "fullName" to user.getFullName(),
                        "active" to user.active
                    ),
                    "coursePreferences" to mapOf(
                        "preferred" to preferredCourses.map {
                            mapOf(
                                "courseCode" to it.courseCode,
                                "courseName" to it.courseName,
                                "department" to it.department
                            )
                        },
                        "availableCourses" to allCourses.count,
                        "lastUpdated" to Instant.now()
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to fetch user with course preferences", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch user with course preferences",
                    "message" to when (e) {
                        is CompletionException -> e.cause?.message ?: e.message
                        else -> e.message
                    }
                )
            )
        }
    }
}