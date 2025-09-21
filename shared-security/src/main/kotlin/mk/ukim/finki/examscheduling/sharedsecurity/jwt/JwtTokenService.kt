package mk.ukim.finki.examscheduling.sharedsecurity.jwt

import mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak.TokenValidationResult
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Service

@Service
class JwtTokenService(
    private val keycloakJwtDecoder: JwtDecoder?,
    private val jwtTokenProvider: JwtTokenProvider
) {

    private val logger = LoggerFactory.getLogger(JwtTokenService::class.java)

    /**
     * Determines if a JWT token was issued by Keycloak or our internal system
     */
    private fun tryDecode(token: String): Jwt? {
        return try {
            if (keycloakJwtDecoder == null) {
                return null
            }
            keycloakJwtDecoder.decode(token)
        } catch (e: Exception) {
            logger.debug("Failed to decode token for type check: {}", e.message)
            null
        }
    }


    fun validateToken(token: String): TokenValidationResult {
        val decodedJwt = tryDecode(token)

        return if (decodedJwt != null && isKeycloakIssuer(decodedJwt)) {
            validateKeycloakToken(decodedJwt)
        } else {
            validateInternalToken(token)
        }
    }

    private fun isKeycloakIssuer(jwt: Jwt): Boolean {
        val issuer = jwt.issuer?.toString()
        val isKeycloak = issuer?.contains("/realms/") == true
        logger.debug("Token issuer: {} - isKeycloak: {}", issuer, isKeycloak)
        return isKeycloak
    }

    private fun validateKeycloakToken(jwt: Jwt): TokenValidationResult {
        return TokenValidationResult.keycloakValid(
            subject = jwt.subject,
            email = jwt.getClaimAsString("email"),
            preferredUsername = jwt.getClaimAsString("preferred_username"),
            name = jwt.getClaimAsString("name"),
            givenName = jwt.getClaimAsString("given_name"),
            familyName = jwt.getClaimAsString("family_name"),
            roles = jwt.getClaimAsStringList("roles") ?: emptyList(),
            expiresAt = jwt.expiresAt
        )
    }

    private fun validateInternalToken(token: String): TokenValidationResult {
        return try {
            if (!jwtTokenProvider.validateToken(token)) {
                return TokenValidationResult.invalid("Invalid internal token")
            }

            val userId = jwtTokenProvider.getUserIdFromToken(token)
            val email = jwtTokenProvider.getEmailFromToken(token)
            val role = jwtTokenProvider.getRoleFromToken(token)
            val fullName = jwtTokenProvider.getFullNameFromToken(token)

            if (userId == null || email == null || role == null) {
                return TokenValidationResult.invalid("Invalid internal token claims")
            }

            TokenValidationResult.internalValid(
                userId = userId,
                email = email,
                role = role,
                fullName = fullName,
                expiresAt = jwtTokenProvider.getExpirationDateFromToken(token)?.toInstant()
            )
        } catch (e: Exception) {
            logger.warn("Internal token validation failed: {}", e.message)
            TokenValidationResult.invalid("Invalid internal token: ${e.message}")
        }
    }
}