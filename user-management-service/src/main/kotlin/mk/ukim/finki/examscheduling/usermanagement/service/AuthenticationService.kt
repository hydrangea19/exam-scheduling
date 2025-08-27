package mk.ukim.finki.examscheduling.usermanagement.service

import mk.ukim.finki.examscheduling.sharedsecurity.dto.*
import mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak.KeycloakTokenResponse
import mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak.KeycloakUserInfo
import mk.ukim.finki.examscheduling.sharedsecurity.jwt.JwtTokenProvider
import mk.ukim.finki.examscheduling.sharedsecurity.jwt.keycloak.KeycloakClientService
import mk.ukim.finki.examscheduling.usermanagement.domain.User
import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import mk.ukim.finki.examscheduling.usermanagement.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val keycloakClientService: KeycloakClientService,
    private val userSyncService: UserSyncService
) {
    private val logger = LoggerFactory.getLogger(AuthenticationService::class.java)

    @Value("\${authentication.keycloak.enabled:true}")
    private var keycloakEnabled: Boolean = true

    @Value("\${authentication.test.enabled:true}")
    private var testEnabled: Boolean = true

    @Value("\${authentication.method:both}")
    private lateinit var authenticationMethod: String

    fun authenticateUser(request: AuthenticationRequest): AuthenticationResponse? {
        logger.info("Authentication attempt for email: {} with method: {}", request.email, request.loginType)

        return when (request.loginType?.lowercase()) {
            "keycloak" -> handleKeycloakAuthentication(request)
            "test" -> handleTestAuthentication(request)
            "email" -> handleEmailAuthentication(request)
            else -> handleAutoDetectAuthentication(request)
        }
    }

    private fun handleAutoDetectAuthentication(request: AuthenticationRequest): AuthenticationResponse? {
        logger.debug("Auto-detecting authentication method for: {}", request.email)

        if (keycloakEnabled && authenticationMethod in listOf("keycloak", "both")) {
            val keycloakResult = handleKeycloakAuthentication(request)
            if (keycloakResult != null) {
                return keycloakResult
            }
            logger.debug("Keycloak authentication failed for {}, trying test authentication", request.email)
        }

        // Fallback to test authentication if enabled
        if (testEnabled && authenticationMethod in listOf("test", "both")) {
            return handleTestAuthentication(request)
        }

        logger.warn("No authentication methods enabled or succeeded for: {}", request.email)
        return null
    }

    private fun handleKeycloakAuthentication(request: AuthenticationRequest): AuthenticationResponse? {
        if (!keycloakEnabled) {
            logger.debug("Keycloak authentication is disabled")
            return null
        }

        return try {
            logger.info("Attempting Keycloak authentication for: {}", request.email)

            val keycloakResponse = keycloakClientService
                .authenticateUser(request.email, request.password)
                .block()

            if (keycloakResponse?.accessToken != null) {
                logger.info("Keycloak authentication successful for: {}", request.email)

                val userInfo = extractUserInfoFromKeycloakResponse(keycloakResponse)

                val localUser = userSyncService.syncKeycloakUser(userInfo)

                generateKeycloakAuthenticationResponse(keycloakResponse, localUser)
            } else {
                logger.warn("Keycloak authentication failed for: {}", request.email)
                null
            }
        } catch (e: Exception) {
            logger.warn("Keycloak authentication error for {}: {}", request.email, e.message)
            null
        }
    }

    private fun handleTestAuthentication(request: AuthenticationRequest): AuthenticationResponse? {
        if (!testEnabled) {
            logger.debug("Test authentication is disabled")
            return null
        }

        logger.info("Using test authentication for: {}", request.email)

        val user = userRepository.findByEmail(request.email) ?: run {
            logger.info("Creating test user for email: {}", request.email)
            val newUser = User(
                email = request.email,
                firstName = extractFirstName(request.email),
                lastName = extractLastName(request.email),
                role = determineRoleFromEmail(request.email),
                active = true
            )
            userRepository.save(newUser)
        }

        if (!user.active) {
            logger.warn("User account is inactive: {}", request.email)
            return null
        }

        return generateInternalAuthenticationResponse(user)
    }

    private fun handleEmailAuthentication(request: AuthenticationRequest): AuthenticationResponse? {
        logger.info("Using email/password authentication for: {}", request.email)

        val user = userRepository.findByEmail(request.email)

        if (user == null) {
            logger.warn("User not found for email: {}", request.email)
            return null
        }

        if (!user.active) {
            logger.warn("User account is inactive: {}", request.email)
            return null
        }

        // TODO: Add actual password validation when implementing proper authentication
        logger.info("User authenticated successfully: {}", request.email)

        return generateInternalAuthenticationResponse(user)
    }

    private fun extractUserInfoFromKeycloakResponse(keycloakResponse: KeycloakTokenResponse): KeycloakUserInfo {
        val accessToken = keycloakResponse.accessToken!!

        return try {
            keycloakClientService.getUserInfo(accessToken).block() ?: run {
                logger.warn("Could not fetch user info from Keycloak, extracting from token")
                KeycloakUserInfo(
                    preferredUsername = "unknown",
                    email = "unknown@example.com",
                    name = "Unknown User"
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to get user info from Keycloak: {}", e.message)
            KeycloakUserInfo(
                preferredUsername = "unknown",
                email = "unknown@example.com",
                name = "Unknown User"
            )
        }
    }

    private fun generateKeycloakAuthenticationResponse(
        keycloakResponse: KeycloakTokenResponse,
        localUser: User
    ): AuthenticationResponse {
        return AuthenticationResponse(
            accessToken = keycloakResponse.accessToken!!,
            refreshToken = keycloakResponse.refreshToken,
            expiresIn = keycloakResponse.expiresIn ?: 300L,
            tokenType = "Keycloak",
            user = UserInfo(
                id = localUser.id.toString(),
                email = localUser.email,
                fullName = localUser.getFullName(),
                role = localUser.role.name
            )
        )
    }

    private fun generateInternalAuthenticationResponse(user: User): AuthenticationResponse {
        val accessToken = jwtTokenProvider.generateToken(
            userId = user.id.toString(),
            email = user.email,
            role = user.role.name,
            fullName = user.getFullName()
        )

        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id.toString())

        val expirationDate = jwtTokenProvider.getExpirationDateFromToken(accessToken)
        val expiresIn = if (expirationDate != null) {
            (expirationDate.time - System.currentTimeMillis()) / 1000
        } else {
            86400L
        }

        return AuthenticationResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = expiresIn,
            tokenType = "Internal",
            user = UserInfo(
                id = user.id.toString(),
                email = user.email,
                fullName = user.getFullName(),
                role = user.role.name
            )
        )
    }

    fun validateToken(request: TokenValidationRequest): TokenValidationResponse {
        val token = request.token

        return try {
            if (!jwtTokenProvider.validateToken(token)) {
                return TokenValidationResponse(
                    valid = false,
                    expired = jwtTokenProvider.isTokenExpired(token),
                    error = "Invalid token"
                )
            }

            val userId = jwtTokenProvider.getUserIdFromToken(token)
            val email = jwtTokenProvider.getEmailFromToken(token)
            val role = jwtTokenProvider.getRoleFromToken(token)
            val fullName = jwtTokenProvider.getFullNameFromToken(token)

            if (userId == null || email == null || role == null) {
                return TokenValidationResponse(
                    valid = false,
                    error = "Invalid token claims"
                )
            }

            TokenValidationResponse(
                valid = true,
                user = UserInfo(
                    id = userId,
                    email = email,
                    fullName = fullName ?: email,
                    role = role
                )
            )
        } catch (e: Exception) {
            logger.error("Token validation error", e)
            TokenValidationResponse(
                valid = false,
                error = "Token validation failed: ${e.message}"
            )
        }
    }

    fun refreshToken(refreshToken: String): AuthenticationResponse? {
        if (keycloakEnabled) {
            try {
                val keycloakResponse = keycloakClientService.refreshToken(refreshToken).block()
                if (keycloakResponse?.accessToken != null) {
                    logger.info("Successfully refreshed Keycloak token")

                    val userInfo = extractUserInfoFromKeycloakResponse(keycloakResponse)
                    val localUser = userSyncService.syncKeycloakUser(userInfo)

                    return generateKeycloakAuthenticationResponse(keycloakResponse, localUser)
                }
            } catch (e: Exception) {
                logger.debug("Keycloak token refresh failed, trying internal refresh: {}", e.message)
            }
        }

        return try {
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                return null
            }

            val userId = jwtTokenProvider.getUserIdFromToken(refreshToken) ?: return null
            val user = userRepository.findById(UUID.fromString(userId)).orElse(null) ?: return null

            if (!user.active) {
                return null
            }

            generateInternalAuthenticationResponse(user)
        } catch (e: Exception) {
            logger.error("Token refresh error", e)
            null
        }
    }

    private fun extractFirstName(email: String): String {
        val localPart = email.substringBefore("@")
        return localPart.substringBefore(".").replaceFirstChar { it.uppercase() }
    }

    private fun extractLastName(email: String): String {
        val localPart = email.substringBefore("@")
        return if (localPart.contains(".")) {
            localPart.substringAfter(".").replaceFirstChar { it.uppercase() }
        } else {
            "User"
        }
    }

    private fun determineRoleFromEmail(email: String): UserRole {
        return when {
            email.contains("admin") -> UserRole.ADMIN
            email.contains("professor") || email.contains("prof") -> UserRole.PROFESSOR
            else -> UserRole.STUDENT
        }
    }
}