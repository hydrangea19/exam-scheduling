package mk.ukim.finki.examscheduling.schedulingservice.controller

import mk.ukim.finki.examscheduling.schedulingservice.service.ExternalIntegrationClient
import mk.ukim.finki.examscheduling.schedulingservice.service.PreferenceManagementClient
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
    private val preferenceManagementClient: PreferenceManagementClient,
    private val externalIntegrationClient: ExternalIntegrationClient
) {
    private val logger = LoggerFactory.getLogger(SecurityTestController::class.java)

    @GetMapping("/test-service-to-service-auth")
    fun testServiceToServiceAuth(): ResponseEntity<Map<String, Any?>> {
        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            logger.info(
                "Testing service-to-service auth from scheduling-service - User: {}, Role: {}",
                currentUser?.username, currentUser?.role
            )

            val prefServicePing = preferenceManagementClient.ping().get()

            val externalServicePing = externalIntegrationClient.ping().get()

            val allPreferences = preferenceManagementClient.getAllPreferences().get()
            val allCourses = externalIntegrationClient.getAllCourses().get()
            val enrollmentData = externalIntegrationClient.getEnrollmentData().get()

            logger.info("Service-to-service auth test completed successfully from scheduling-service")

            ResponseEntity.ok(
                mapOf(
                    "status" to "SUCCESS",
                    "message" to "Service-to-service authentication working from scheduling-service",
                    "authenticationInfo" to mapOf(
                        "currentUser" to currentUser?.username,
                        "currentRole" to currentUser?.role,
                        "authenticated" to SecurityUtils.isAuthenticated(),
                        "serviceName" to "scheduling-service"
                    ),
                    "serviceCallResults" to mapOf(
                        "preferenceManagementPing" to mapOf(
                            "service" to prefServicePing["service"],
                            "message" to prefServicePing["message"]
                        ),
                        "externalIntegrationPing" to mapOf(
                            "service" to externalServicePing["service"],
                            "message" to externalServicePing["message"]
                        ),
                        "dataCallResults" to mapOf(
                            "preferencesCount" to allPreferences["count"],
                            "coursesCount" to allCourses["count"],
                            "enrollmentDataAvailable" to (enrollmentData["enrollmentData"] != null)
                        )
                    ),
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Service-to-service auth test failed from scheduling-service: {}", e.message, e)

            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "status" to "ERROR",
                    "message" to "Service-to-service authentication test failed",
                    "error" to e.message,
                    "errorType" to e.javaClass.simpleName,
                    "serviceName" to "scheduling-service",
                    "timestamp" to Instant.now()
                )
            )
        }
    }

    @GetMapping("/test-scheduling-workflow")
    fun testSchedulingWorkflow(): ResponseEntity<Map<String, Any?>> {
        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            logger.info("Testing scheduling workflow - simulating schedule generation process")

            val preferences = preferenceManagementClient.getAllPreferences().get()
            val courses = externalIntegrationClient.getAllCourses().get()
            val enrollmentData = externalIntegrationClient.getEnrollmentData().get()

            val preferencesCount = preferences["count"] as? Int ?: 0
            val coursesCount = courses["count"] as? Int ?: 0
            val canGenerateSchedule = preferencesCount > 0 && coursesCount > 0

            logger.info("Scheduling workflow test completed - can generate: {}", canGenerateSchedule)

            ResponseEntity.ok(
                mapOf(
                    "status" to "SUCCESS",
                    "message" to "Scheduling workflow test completed",
                    "workflowResults" to mapOf(
                        "preferencesAvailable" to preferencesCount,
                        "coursesAvailable" to coursesCount,
                        "enrollmentDataAvailable" to (enrollmentData["error"] == null),
                        "canGenerateSchedule" to canGenerateSchedule,
                        "schedulingReadiness" to if (canGenerateSchedule) "READY" else "NOT_READY"
                    ),
                    "authContext" to mapOf(
                        "user" to currentUser?.username,
                        "role" to currentUser?.role,
                        "serviceName" to "scheduling-service"
                    ),
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Scheduling workflow test failed: {}", e.message, e)

            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "status" to "ERROR",
                    "message" to "Scheduling workflow test failed",
                    "error" to e.message,
                    "errorType" to e.javaClass.simpleName,
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
                    "serviceName" to "scheduling-service",
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
                    "serviceName" to "scheduling-service",
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
}