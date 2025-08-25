package mk.ukim.finki.examscheduling.preferencemanagement.controller

import mk.ukim.finki.examscheduling.preferencemanagement.service.ExternalIntegrationClient
import mk.ukim.finki.examscheduling.preferencemanagement.service.UserManagementClient
import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/test/security")
class SecurityTestController(
    private val userManagementClient: UserManagementClient,
    private val externalIntegrationClient: ExternalIntegrationClient
) {
    private val logger = LoggerFactory.getLogger(SecurityTestController::class.java)

    @GetMapping("/test-service-to-service-auth")
    fun testServiceToServiceAuth(): ResponseEntity<Map<String, Any?>> {
        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            logger.info(
                "Testing service-to-service auth from preference-management - User: {}, Role: {}",
                currentUser?.username, currentUser?.role
            )

            val userServicePing = userManagementClient.ping().get()

            val externalServicePing = externalIntegrationClient.ping().get()

            val allUsers = userManagementClient.getAllUsers().get()
            val allCourses = externalIntegrationClient.getAllCourses().get()

            logger.info("Service-to-service auth test completed successfully from preference-management")

            ResponseEntity.ok(
                mapOf(
                    "status" to "SUCCESS",
                    "message" to "Service-to-service authentication working from preference-management",
                    "authenticationInfo" to mapOf(
                        "currentUser" to currentUser?.username,
                        "currentRole" to currentUser?.role,
                        "authenticated" to SecurityUtils.isAuthenticated(),
                        "serviceName" to "preference-management-service"
                    ),
                    "serviceCallResults" to mapOf(
                        "userManagementPing" to mapOf(
                            "service" to userServicePing["service"],
                            "message" to userServicePing["message"]
                        ),
                        "externalIntegrationPing" to mapOf(
                            "service" to externalServicePing["service"],
                            "message" to externalServicePing["message"]
                        ),
                        "dataCallResults" to mapOf(
                            "usersCount" to allUsers["count"],
                            "coursesCount" to allCourses["count"]
                        )
                    ),
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Service-to-service auth test failed from preference-management: {}", e.message, e)

            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "status" to "ERROR",
                    "message" to "Service-to-service authentication test failed",
                    "error" to e.message,
                    "errorType" to e.javaClass.simpleName,
                    "serviceName" to "preference-management-service",
                    "timestamp" to Instant.now()
                )
            )
        }
    }

    @GetMapping("/test-authentication-context")
    fun testAuthenticationContext(): ResponseEntity<Map<String, Any?>> {
        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            ResponseEntity.ok(
                mapOf(
                    "authenticated" to SecurityUtils.isAuthenticated(),
                    "userInfo" to if (currentUser != null) {
                        mapOf(
                            "id" to currentUser.id,
                            "email" to currentUser.username,
                            "role" to currentUser.role,
                            "fullName" to currentUser.fullName
                        )
                    } else null,
                    "authorities" to (currentUser?.authorities?.map { it.authority } ?: emptyList()),
                    "hasAdminRole" to SecurityUtils.hasRole("ADMIN"),
                    "hasProfessorRole" to SecurityUtils.hasRole("PROFESSOR"),
                    "hasSystemRole" to SecurityUtils.hasRole("SYSTEM"),
                    "serviceName" to "preference-management-service",
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Authentication context test failed: {}", e.message, e)

            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Authentication context test failed",
                    "message" to e.message,
                    "timestamp" to Instant.now()
                )
            )
        }
    }

    @GetMapping("/test-admin-only")
    fun testAdminOnly(): ResponseEntity<Map<String, Any?>> {
        return try {
            SecurityUtils.requireAdmin()

            ResponseEntity.ok(
                mapOf(
                    "message" to "Admin access granted",
                    "user" to SecurityUtils.getCurrentUser()?.username,
                    "serviceName" to "preference-management-service",
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: SecurityException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                mapOf(
                    "error" to "Admin access required",
                    "message" to e.message,
                    "timestamp" to Instant.now()
                )
            )
        }
    }

    @GetMapping("/test-professor-or-admin")
    fun testProfessorOrAdmin(): ResponseEntity<Map<String, Any?>> {
        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            if (currentUser == null || (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("PROFESSOR"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    mapOf(
                        "error" to "Professor or Admin role required",
                        "currentRole" to currentUser?.role,
                        "timestamp" to Instant.now()
                    )
                )
            }

            ResponseEntity.ok(
                mapOf(
                    "message" to "Professor/Admin access granted",
                    "user" to currentUser.username,
                    "role" to currentUser.role,
                    "serviceName" to "preference-management-service",
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Professor/Admin test failed: {}", e.message, e)

            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Authorization test failed",
                    "message" to e.message,
                    "timestamp" to Instant.now()
                )
            )
        }
    }
}