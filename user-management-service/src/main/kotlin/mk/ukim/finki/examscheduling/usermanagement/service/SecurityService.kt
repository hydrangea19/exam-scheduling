package mk.ukim.finki.examscheduling.usermanagement.service

import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
class SecurityService {

    private val logger = LoggerFactory.getLogger(SecurityService::class.java)

    fun canAccessUser(authentication: Authentication, targetUserId: String): Boolean {
        try {
            val currentUser = SecurityUtils.getCurrentUser()

            if (currentUser == null) {
                logger.warn("No authenticated user found")
                return false
            }

            val currentRole = SecurityUtils.getCurrentUserRole()
            logger.debug(
                "Checking access: user {} (role: {}) accessing user {}",
                currentUser.username, currentRole, targetUserId
            )

            if (currentRole == "ADMIN") {
                logger.debug("Admin user {} granted access to user {}", currentUser.username, targetUserId)
                return true
            }

            val currentUserId = SecurityUtils.getCurrentUserId()
            val canAccess = currentUserId != null && currentUserId == targetUserId

            if (canAccess) {
                logger.debug("User {} granted access to their own data", currentUser.username)
            } else {
                logger.warn(
                    "User {} denied access to user {} (current userId: {})",
                    currentUser.username, targetUserId, currentUserId
                )
            }

            return canAccess

        } catch (e: Exception) {
            logger.error("Error checking user access permission", e)
            return false
        }
    }

    fun hasRole(authentication: Authentication, role: String): Boolean {
        return try {
            val currentRole = SecurityUtils.getCurrentUserRole()
            currentRole == role
        } catch (e: Exception) {
            logger.error("Error checking user role", e)
            false
        }
    }

    fun hasAnyRole(authentication: Authentication, vararg roles: String): Boolean {
        return try {
            val currentRole = SecurityUtils.getCurrentUserRole()
            currentRole != null && roles.contains(currentRole)
        } catch (e: Exception) {
            logger.error("Error checking user roles", e)
            false
        }
    }

    fun getCurrentUserId(): String? {
        return SecurityUtils.getCurrentUserId()
    }

    fun getCurrentUserEmail(): String? {
        return SecurityUtils.getCurrentUser()?.username
    }

    fun isAuthenticated(): Boolean {
        return SecurityUtils.isAuthenticated()
    }
}