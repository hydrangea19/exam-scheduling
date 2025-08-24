package mk.finki.ukim.examscheduling.apigateway.controller

import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Instant

@RestController
@RequestMapping("/api/gateway")
class ApiGatewayTestController {

    @GetMapping("/ping")
    fun ping(): Mono<Map<String, Any>> {
        return Mono.just(
            mapOf(
                "message" to "API Gateway is running",
                "timestamp" to Instant.now(),
                "service" to "api-gateway",
                "version" to "1.0.0-SNAPSHOT"
            )
        )
    }

    @GetMapping("/auth-status")
    fun authStatus(): Mono<ResponseEntity<Map<String, Any>>> {
        return Mono.fromCallable {
            val currentUser = SecurityUtils.getCurrentUser()

            if (currentUser != null) {
                ResponseEntity.ok(
                    mapOf(
                        "authenticated" to true,
                        "user" to mapOf(
                            "id" to currentUser.id,
                            "email" to currentUser.username,
                            "role" to SecurityUtils.getCurrentUserRole(),
                            "fullName" to currentUser.fullName
                        ),
                        "timestamp" to Instant.now()
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    mapOf(
                        "authenticated" to false,
                        "message" to "No authentication found",
                        "timestamp" to Instant.now()
                    )
                )
            }
        }
    }

    @GetMapping("/test-auth-required")
    fun testAuthRequired(): Mono<ResponseEntity<Map<String, Any?>>> {
        return Mono.fromCallable {
            try {
                val currentUser = SecurityUtils.requireAuthentication()

                ResponseEntity.ok(
                    mapOf(
                        "message" to "Authentication successful",
                        "authenticatedUser" to mapOf(
                            "id" to currentUser.id,
                            "email" to currentUser.username,
                            "role" to SecurityUtils.getCurrentUserRole()
                        ),
                        "timestamp" to Instant.now()
                    )
                )
            } catch (e: SecurityException) {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    mapOf(
                        "error" to "Authentication required",
                        "message" to e.message,
                        "timestamp" to Instant.now()
                    )
                )
            }
        }
    }

    @GetMapping("/test-admin-only")
    fun testAdminOnly(): Mono<ResponseEntity<Map<String, Any?>>> {
        return Mono.fromCallable {
            try {
                val adminUser = SecurityUtils.requireAdmin()

                ResponseEntity.ok(
                    mapOf(
                        "message" to "Admin access granted",
                        "adminUser" to mapOf(
                            "id" to adminUser.id,
                            "email" to adminUser.username,
                            "role" to SecurityUtils.getCurrentUserRole()
                        ),
                        "timestamp" to Instant.now()
                    )
                )
            } catch (e: SecurityException) {
                ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    mapOf(
                        "error" to "Admin access required",
                        "message" to e.message,
                        "currentRole" to SecurityUtils.getCurrentUserRole(),
                        "timestamp" to Instant.now()
                    )
                )
            }
        }
    }
}