package mk.ukim.finki.examscheduling.sharedsecurity.utilities

import mk.ukim.finki.examscheduling.sharedsecurity.domain.UserPrincipal
import mk.ukim.finki.examscheduling.sharedsecurity.domain.dto.CurrentUserDetails
import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtils {

    fun getCurrentUser(): UserPrincipal? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication?.principal is UserPrincipal) {
            authentication.principal as UserPrincipal
        } else {
            null
        }
    }

    fun getCurrentUserId(): String? {
        return getCurrentUser()?.id
    }

    fun getCurrentUserEmail(): String? {
        return getCurrentUser()?.username
    }

    fun getCurrentUserRole(): String? {
        return getCurrentUser()?.authorities?.firstOrNull()?.authority?.removePrefix("ROLE_")
    }

    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication != null && authentication.isAuthenticated && authentication.principal is UserPrincipal
    }

    fun hasRole(role: String): Boolean {
        val currentUser = getCurrentUser()
        return currentUser?.hasRole(role) ?: false
    }

    fun isAdmin(): Boolean = hasRole("ADMIN")
    fun isProfessor(): Boolean = hasRole("PROFESSOR")
    fun isStudent(): Boolean = hasRole("STUDENT")

    fun requireAuthentication(): UserPrincipal {
        return getCurrentUser() ?: throw SecurityException("Authentication required")
    }

    fun requireRole(role: String): UserPrincipal {
        val user = requireAuthentication()
        if (!user.hasRole(role)) {
            throw SecurityException("Role $role required")
        }
        return user
    }

    fun requireAdmin(): UserPrincipal = requireRole("ADMIN")
    fun requireProfessor(): UserPrincipal = requireRole("PROFESSOR")

    //fun getCurrentUserId(): UUID? = SecurityUtilsExtensions.getCurrentUserId()

    fun getCurrentUserDetails(): CurrentUserDetails? = SecurityUtilsExtensions.getCurrentUserDetails()

    fun isCurrentUserAdmin(): Boolean = SecurityUtilsExtensions.isCurrentUserAdmin()

    fun isCurrentUserProfessor(): Boolean = SecurityUtilsExtensions.isCurrentUserProfessor()


    fun isCurrentUserStudent(): Boolean = SecurityUtilsExtensions.isCurrentUserStudent()

    fun hasAnyRole(vararg roles: String): Boolean = SecurityUtilsExtensions.hasAnyRole(*roles)
}