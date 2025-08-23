package mk.ukim.finki.examscheduling.usermanagement.controller

import mk.ukim.finki.examscheduling.usermanagement.domain.User
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.users.UserCreateRequest
import mk.ukim.finki.examscheduling.usermanagement.domain.dto.users.UserProfileWithCourses
import mk.ukim.finki.examscheduling.usermanagement.repository.UserRepository
import mk.ukim.finki.examscheduling.usermanagement.service.ExternalIntegrationClient
import org.slf4j.LoggerFactory
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
    fun ping(): Map<String, Any> {
        return mapOf(
            "message" to "User Management Service is running",
            "timestamp" to Instant.now(),
            "service" to "user-management-service",
            "version" to "1.0.0-SNAPSHOT"
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