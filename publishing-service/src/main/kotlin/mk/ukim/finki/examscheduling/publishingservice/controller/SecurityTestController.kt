package mk.ukim.finki.examscheduling.publishingservice.controller

import mk.ukim.finki.examscheduling.publishingservice.service.SchedulingServiceClient
import mk.ukim.finki.examscheduling.publishingservice.service.UserManagementClient
import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate

@RestController
@RequestMapping("/api/test/security")
class SecurityTestController(
    private val schedulingServiceClient: SchedulingServiceClient,
    private val userManagementClient: UserManagementClient
) {
    private val logger = LoggerFactory.getLogger(SecurityTestController::class.java)

    @GetMapping("/test-service-to-service-auth")
    fun testServiceToServiceAuth(): ResponseEntity<Map<String, Any?>> {
        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            logger.info(
                "Testing service-to-service auth from publishing-service - User: {}, Role: {}",
                currentUser?.username, currentUser?.role
            )

            val schedulingServicePing = schedulingServiceClient.ping().get()

            val userServicePing = userManagementClient.ping().get()

            val allSchedules = schedulingServiceClient.getAllSchedules().get()
            val finalizedSchedules = schedulingServiceClient.getFinalizedSchedules().get()

            logger.info("Service-to-service auth test completed successfully from publishing-service")

            ResponseEntity.ok(
                mapOf(
                    "status" to "SUCCESS",
                    "message" to "Service-to-service authentication working from publishing-service",
                    "authenticationInfo" to mapOf(
                        "currentUser" to currentUser?.username,
                        "currentRole" to currentUser?.role,
                        "authenticated" to SecurityUtils.isAuthenticated(),
                        "serviceName" to "publishing-service"
                    ),
                    "serviceCallResults" to mapOf(
                        "schedulingServicePing" to mapOf(
                            "service" to schedulingServicePing["service"],
                            "message" to schedulingServicePing["message"]
                        ),
                        "userManagementPing" to mapOf(
                            "service" to userServicePing["service"],
                            "message" to userServicePing["message"]
                        ),
                        "dataCallResults" to mapOf(
                            "schedulesCount" to allSchedules["count"],
                            "finalizedSchedulesCount" to finalizedSchedules["count"]
                        )
                    ),
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Service-to-service auth test failed from publishing-service: {}", e.message, e)

            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "status" to "ERROR",
                    "message" to "Service-to-service authentication test failed",
                    "error" to e.message,
                    "errorType" to e.javaClass.simpleName,
                    "serviceName" to "publishing-service",
                    "timestamp" to Instant.now()
                )
            )
        }
    }

    @GetMapping("/test-publishing-workflow")
    fun testPublishingWorkflow(): ResponseEntity<Map<String, Any?>> {
        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            logger.info("Testing publishing workflow - simulating schedule publication process")

            val finalizedSchedules = schedulingServiceClient.getFinalizedSchedules().get()
            val scheduleStatistics = schedulingServiceClient.getScheduleStatistics().get()

            val todaysExams = schedulingServiceClient.getExamsByDate(LocalDate.now()).get()

            val schedulesCount = finalizedSchedules["count"] as? Int ?: 0
            val canPublish = schedulesCount > 0

            logger.info("Publishing workflow test completed - can publish: {}", canPublish)

            ResponseEntity.ok(
                mapOf(
                    "status" to "SUCCESS",
                    "message" to "Publishing workflow test completed",
                    "workflowResults" to mapOf(
                        "finalizedSchedulesAvailable" to schedulesCount,
                        "statisticsAvailable" to (scheduleStatistics["error"] == null),
                        "todaysExamsCount" to (todaysExams["count"] as? Int ?: 0),
                        "canPublish" to canPublish,
                        "publishingReadiness" to if (canPublish) "READY" else "NOT_READY",
                        "lastChecked" to Instant.now()
                    ),
                    "authContext" to mapOf(
                        "user" to currentUser?.username,
                        "role" to currentUser?.role,
                        "serviceName" to "publishing-service"
                    ),
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Publishing workflow test failed: {}", e.message, e)

            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "status" to "ERROR",
                    "message" to "Publishing workflow test failed",
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
                    "serviceName" to "publishing-service",
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
                    "serviceName" to "publishing-service",
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

    @GetMapping("/test-end-to-end-publishing")
    fun testEndToEndPublishing(): ResponseEntity<Map<String, Any?>> {
        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            logger.info("Testing end-to-end publishing process")

            val finalizedSchedules = schedulingServiceClient.getFinalizedSchedules().get()

            val statistics = schedulingServiceClient.getScheduleStatistics().get()

            val results = mapOf(
                "step1_schedules" to mapOf(
                    "count" to finalizedSchedules["count"],
                    "status" to if (finalizedSchedules["error"] == null) "SUCCESS" else "FAILED"
                ),
                "step3_statistics" to mapOf(
                    "available" to (statistics["error"] == null),
                    "status" to if (statistics["error"] == null) "SUCCESS" else "FAILED"
                ),
                "overallStatus" to if (finalizedSchedules["error"] == null &&
                    statistics["error"] == null
                ) "SUCCESS" else "PARTIAL_SUCCESS"
            )

            logger.info("End-to-end publishing test completed with status: {}", results["overallStatus"])

            ResponseEntity.ok(
                mapOf(
                    "status" to "SUCCESS",
                    "message" to "End-to-end publishing test completed",
                    "results" to results,
                    "authContext" to mapOf(
                        "user" to currentUser?.username,
                        "role" to currentUser?.role,
                        "serviceName" to "publishing-service"
                    ),
                    "timestamp" to Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("End-to-end publishing test failed: {}", e.message, e)

            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "status" to "ERROR",
                    "message" to "End-to-end publishing test failed",
                    "error" to e.message,
                    "errorType" to e.javaClass.simpleName,
                    "timestamp" to Instant.now()
                )
            )
        }
    }
}