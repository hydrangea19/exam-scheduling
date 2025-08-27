package mk.ukim.finki.examscheduling.usermanagement.repository

import mk.ukim.finki.examscheduling.usermanagement.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, UUID> {

    fun existsByEmail(email: String): Boolean

    fun findByEmail(email: String): User?

    @Query(
        """
        SELECT u FROM User u 
        WHERE LOWER(CONCAT(u.firstName, ' ', COALESCE(u.middleName, ''), ' ', u.lastName)) 
        LIKE LOWER(CONCAT('%', :namePattern, '%'))
    """
    )
    fun findByFullNameContaining(@Param("namePattern") namePattern: String): List<User>

    @Query(
        value = """
            SELECT 
                COUNT(*) as total_users,
                COUNT(CASE WHEN active = true THEN 1 END) as active_users,
                COUNT(CASE WHEN active = false THEN 1 END) as inactive_users,
                COUNT(CASE WHEN middle_name IS NOT NULL THEN 1 END) as users_with_middle_name
            FROM users
        """,
        nativeQuery = true
    )
    fun getUserStatistics(): Map<String, Any>
}