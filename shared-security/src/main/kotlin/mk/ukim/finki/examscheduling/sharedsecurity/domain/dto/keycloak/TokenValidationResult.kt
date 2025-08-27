package mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak

import mk.ukim.finki.examscheduling.sharedsecurity.domain.enums.TokenType

sealed class TokenValidationResult {
    abstract val isValid: Boolean
    abstract val error: String?

    data class Valid(
        val tokenType: TokenType,
        val subject: String?,
        val email: String?,
        val role: String?,
        val fullName: String?,
        val preferredUsername: String? = null,
        val givenName: String? = null,
        val familyName: String? = null,
        val roles: List<String> = emptyList(),
        val expiresAt: java.time.Instant? = null
    ) : TokenValidationResult() {
        override val isValid = true
        override val error: String? = null
    }

    data class Invalid(
        val reason: String
    ) : TokenValidationResult() {
        override val isValid = false
        override val error = reason
    }

    companion object {
        fun keycloakValid(
            subject: String?,
            email: String?,
            preferredUsername: String?,
            name: String?,
            givenName: String?,
            familyName: String?,
            roles: List<String>,
            expiresAt: java.time.Instant?
        ): Valid {
            val primaryRole = roles.firstOrNull { it in listOf("ADMIN", "PROFESSOR", "STUDENT") } ?: "STUDENT"

            return Valid(
                tokenType = TokenType.KEYCLOAK,
                subject = subject,
                email = email,
                role = primaryRole,
                fullName = name,
                preferredUsername = preferredUsername,
                givenName = givenName,
                familyName = familyName,
                roles = roles,
                expiresAt = expiresAt
            )
        }

        fun internalValid(
            userId: String,
            email: String,
            role: String,
            fullName: String?,
            expiresAt: java.time.Instant?
        ): Valid {
            return Valid(
                tokenType = TokenType.INTERNAL,
                subject = userId,
                email = email,
                role = role,
                fullName = fullName,
                expiresAt = expiresAt
            )
        }

        fun invalid(reason: String): Invalid {
            return Invalid(reason)
        }
    }
}