package mk.ukim.finki.examscheduling.usermanagement.controller

import mk.ukim.finki.examscheduling.sharedsecurity.dto.AuthenticationRequest
import mk.ukim.finki.examscheduling.sharedsecurity.dto.TokenValidationRequest
import mk.ukim.finki.examscheduling.sharedsecurity.dto.TokenValidationResponse
import mk.ukim.finki.examscheduling.sharedsecurity.dto.UserInfo
import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import mk.ukim.finki.examscheduling.usermanagement.service.AuthenticationService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthenticationController(
    private val authenticationService: AuthenticationService
) {
    private val logger = LoggerFactory.getLogger(AuthenticationController::class.java)

    @PostMapping("/login")
    fun login(@RequestBody request: AuthenticationRequest): ResponseEntity<Any> {
        return try {
            logger.info("Login request received for email: {}", request.email)

            val authResponse = authenticationService.authenticateUser(request)

            if (authResponse != null) {
                logger.info("Login successful for email: {}", request.email)
                ResponseEntity.ok(authResponse)
            } else {
                logger.warn("Login failed for email: {}", request.email)
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    mapOf(
                        "error" to "Authentication failed",
                        "message" to "Invalid credentials or inactive account"
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("Login error for email: {}", request.email, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Authentication error",
                    "message" to e.message
                )
            )
        }
    }

    @PostMapping("/validate")
    fun validateToken(@RequestBody request: TokenValidationRequest): ResponseEntity<TokenValidationResponse> {
        return try {
            val validationResponse = authenticationService.validateToken(request)
            ResponseEntity.ok(validationResponse)
        } catch (e: Exception) {
            logger.error("Token validation error", e)
            ResponseEntity.ok(
                TokenValidationResponse(
                    valid = false,
                    error = "Validation error: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/refresh")
    fun refreshToken(@RequestBody request: Map<String, String>): ResponseEntity<Any> {
        return try {
            val refreshToken = request["refreshToken"]

            if (refreshToken.isNullOrBlank()) {
                return ResponseEntity.badRequest().body(
                    mapOf("error" to "Refresh token is required")
                )
            }

            val authResponse = authenticationService.refreshToken(refreshToken)

            if (authResponse != null) {
                ResponseEntity.ok(authResponse)
            } else {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    mapOf("error" to "Invalid refresh token")
                )
            }
        } catch (e: Exception) {
            logger.error("Token refresh error", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Token refresh error",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/me")
    fun getCurrentUser(): ResponseEntity<Any> {
        return try {
            val currentUser = SecurityUtils.getCurrentUser()

            if (currentUser != null) {
                ResponseEntity.ok(
                    UserInfo(
                        id = currentUser.id,
                        email = currentUser.username,
                        fullName = currentUser.fullName ?: currentUser.username,
                        role = SecurityUtils.getCurrentUserRole() ?: "UNKNOWN"
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    mapOf("error" to "Not authenticated")
                )
            }
        } catch (e: Exception) {
            logger.error("Get current user error", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("error" to e.message)
            )
        }
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "message" to "Logout successful",
                "instruction" to "Remove token from client storage"
            )
        )
    }
}