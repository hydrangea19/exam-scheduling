package mk.ukim.finki.examscheduling.sharedsecurity.utilities

import mk.ukim.finki.examscheduling.sharedsecurity.domain.dto.CurrentUserDetails
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.*

object SecurityUtilsExtensions {

    private val logger = LoggerFactory.getLogger(SecurityUtilsExtensions::class.java)

    fun getCurrentUserId(): UUID? {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
                ?: return null

            when (authentication.principal) {
                is UserDetails -> {
                    val userDetails = authentication.principal as UserDetails
                    extractUserIdFromUsername(userDetails.username)
                }
                is JwtAuthenticationToken -> {
                    val jwt = (authentication as JwtAuthenticationToken).token
                    extractUserIdFromJwt(jwt)
                }
                is String -> {
                    extractUserIdFromUsername(authentication.principal as String)
                }
                else -> {
                    logger.debug("Unknown principal type: {}", authentication.principal?.javaClass?.simpleName)
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Error getting current user ID", e)
            null
        }
    }

    fun getCurrentUserDetails(): CurrentUserDetails? {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
                ?: return null

            val userId = getCurrentUserId()
            val email = getCurrentUserEmail()
            val role = getCurrentUserRole()
            val fullName = getCurrentUserFullName()

            if (userId != null && email != null && role != null) {
                CurrentUserDetails(
                    userId = userId,
                    email = email,
                    role = role,
                    fullName = fullName,
                    authorities = authentication.authorities.map { it.authority }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Error getting current user details", e)
            null
        }
    }

    private fun getCurrentUserEmail(): String? {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
                ?: return null

            when (authentication.principal) {
                is UserDetails -> (authentication.principal as UserDetails).username
                is JwtAuthenticationToken -> {
                    val jwt = (authentication as JwtAuthenticationToken).token
                    jwt.claims["email"] as? String ?: jwt.claims["preferred_username"] as? String
                }
                is String -> authentication.principal as String
                else -> null
            }
        } catch (e: Exception) {
            logger.error("Error getting current user email", e)
            null
        }
    }

    private fun getCurrentUserRole(): String? {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
                ?: return null

            val authorities = authentication.authorities
            val roleAuthority = authorities.firstOrNull { it.authority.startsWith("ROLE_") }

            roleAuthority?.authority?.removePrefix("ROLE_")
        } catch (e: Exception) {
            logger.error("Error getting current user role", e)
            null
        }
    }

    private fun getCurrentUserFullName(): String? {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication
                ?: return null

            when (authentication.principal) {
                is JwtAuthenticationToken -> {
                    val jwt = (authentication as JwtAuthenticationToken).token
                    jwt.claims["name"] as? String
                        ?: jwt.claims["full_name"] as? String
                        ?: jwt.claims["given_name"]?.let { given ->
                            val family = jwt.claims["family_name"]
                            if (family != null) "$given $family" else given as String
                        }
                }
                else -> null
            }
        } catch (e: Exception) {
            logger.debug("Could not extract full name from token", e)
            null
        }
    }

    private fun extractUserIdFromJwt(jwt: Jwt): UUID? {
        return try {
            val userIdClaim = jwt.claims["user_id"]
                ?: jwt.claims["sub"]
                ?: jwt.claims["userId"]
                ?: jwt.claims["id"]

            when (userIdClaim) {
                is String -> {
                    try {
                        UUID.fromString(userIdClaim)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
                is UUID -> userIdClaim
                else -> null
            }
        } catch (e: Exception) {
            logger.debug("Could not extract user ID from JWT", e)
            null
        }
    }

    private fun extractUserIdFromUsername(username: String): UUID? {
        return try {
            try {
                UUID.fromString(username)
            } catch (e: IllegalArgumentException) {
                null
            }
        } catch (e: Exception) {
            logger.debug("Could not extract user ID from username: {}", username, e)
            null
        }
    }

    fun isCurrentUserAdmin(): Boolean {
        return getCurrentUserRole() == "ADMIN"
    }

    fun isCurrentUserProfessor(): Boolean {
        return getCurrentUserRole() == "PROFESSOR"
    }

    fun isCurrentUserStudent(): Boolean {
        return getCurrentUserRole() == "STUDENT"
    }

    fun hasAnyRole(vararg roles: String): Boolean {
        val currentRole = getCurrentUserRole()
        return currentRole != null && roles.contains(currentRole)
    }
}