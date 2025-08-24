import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class UserPrincipal(
    val id: String,
    private val email: String,
    private val role: String,
    val fullName: String?,
    private val enabled: Boolean = true
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_$role"))
    }

    override fun getPassword(): String {
        return ""
    }

    override fun getUsername(): String {
        return email
    }

    override fun isAccountNonExpired(): Boolean = enabled

    override fun isAccountNonLocked(): Boolean = enabled

    override fun isCredentialsNonExpired(): Boolean = enabled

    override fun isEnabled(): Boolean = enabled

    fun hasRole(roleToCheck: String): Boolean {
        return role.equals(roleToCheck, ignoreCase = true)
    }

    fun isAdmin(): Boolean = hasRole("ADMIN")
    fun isProfessor(): Boolean = hasRole("PROFESSOR")
    fun isStudent(): Boolean = hasRole("STUDENT")
}
