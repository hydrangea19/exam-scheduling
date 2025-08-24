package mk.finki.ukim.examscheduling.apigateway.controller

import UserPrincipal
import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.ReactiveSecurityContextHolder
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
    fun authStatus(): Mono<ResponseEntity<Map<String, Any?>>> {
        return ReactiveSecurityContextHolder.getContext()
            .map { context ->
                val currentUser = context.authentication.principal as? UserPrincipal

                if (currentUser != null) {
                    ResponseEntity.ok(
                        mapOf(
                            "authenticated" to true,
                            "user" to mapOf<String, Any?>(
                                "id" to currentUser.id,
                                "email" to currentUser.username,
                                "role" to currentUser.role,
                                "fullName" to currentUser.fullName
                            ),
                            "timestamp" to Instant.now()
                        )
                    )
                } else {
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        mapOf<String, Any?>(
                            "authenticated" to false,
                            "message" to "No authentication found",
                            "timestamp" to Instant.now()
                        )
                    )
                }
            }
            .defaultIfEmpty(
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    mapOf<String, Any?>(
                        "authenticated" to false,
                        "message" to "No authentication found",
                        "timestamp" to Instant.now()
                    )
                )
            )
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
        return ReactiveSecurityContextHolder.getContext()
            .map { context ->
                val currentUser = context.authentication.principal as? UserPrincipal

                if (currentUser != null && currentUser.hasRole("ADMIN")) {
                    ResponseEntity.ok(
                        mapOf<String, Any?>(
                            "message" to "Admin access granted",
                            "adminUser" to mapOf(
                                "id" to currentUser.id,
                                "email" to currentUser.username,
                                "role" to currentUser.role
                            ),
                            "timestamp" to Instant.now()
                        )
                    )
                } else {
                    ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        mapOf<String, Any?>(
                            "error" to "Admin access required",
                            "message" to "Current user does not have the ADMIN role.",
                            "timestamp" to Instant.now()
                        )
                    )
                }
            }
            .defaultIfEmpty(
                ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    mapOf<String, Any?>(
                        "error" to "Admin access required",
                        "message" to "No authenticated user found.",
                        "timestamp" to Instant.now()
                    )
                )
            )
    }
}