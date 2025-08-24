package mk.ukim.finki.examscheduling.usermanagement.service

import mk.ukim.finki.examscheduling.sharedsecurity.dto.*
import mk.ukim.finki.examscheduling.sharedsecurity.jwt.JwtTokenProvider
import mk.ukim.finki.examscheduling.usermanagement.domain.User
import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import mk.ukim.finki.examscheduling.usermanagement.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {
    private val logger = LoggerFactory.getLogger(AuthenticationService::class.java)

    fun authenticateUser(request: AuthenticationRequest): AuthenticationResponse? {
        logger.info("Authentication attempt for email: {}", request.email)

        return when (request.loginType) {
            "test" -> handleTestAuthentication(request)
            "email" -> handleEmailAuthentication(request)
            else -> {
                logger.warn("Unknown login type: {}", request.loginType)
                null
            }
        }
    }

    private fun handleTestAuthentication(request: AuthenticationRequest): AuthenticationResponse? {
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

        return generateAuthenticationResponse(user)
    }

    private fun handleEmailAuthentication(request: AuthenticationRequest): AuthenticationResponse? {
        val user = userRepository.findByEmail(request.email)

        if (user == null) {
            logger.warn("User not found for email: {}", request.email)
            return null
        }

        if (!user.active) {
            logger.warn("User account is inactive: {}", request.email)
            return null
        }

        // TODO: Add password validation when implementing proper authentication
        logger.info("User authenticated successfully: {}", request.email)

        return generateAuthenticationResponse(user)
    }

    private fun generateAuthenticationResponse(user: User): AuthenticationResponse {
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

        return TokenValidationResponse(
            valid = true,
            user = UserInfo(
                id = userId,
                email = email,
                fullName = fullName ?: email,
                role = role
            )
        )
    }

    fun refreshToken(refreshToken: String): AuthenticationResponse? {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return null
        }

        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken) ?: return null
        val user = userRepository.findById(UUID.fromString(userId)).orElse(null) ?: return null

        if (!user.active) {
            return null
        }

        return generateAuthenticationResponse(user)
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