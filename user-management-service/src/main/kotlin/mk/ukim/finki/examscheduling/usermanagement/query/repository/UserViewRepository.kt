package mk.ukim.finki.examscheduling.usermanagement.query.repository

import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import mk.ukim.finki.examscheduling.usermanagement.query.UserView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*


@Repository
interface UserViewRepository : JpaRepository<UserView, UUID> {

    fun findByEmail(email: String): UserView?

    fun existsByEmail(email: String): Boolean

    // Status-based queries
    fun findByActive(active: Boolean): List<UserView>

    fun findByActiveTrue(): List<UserView>

    fun findByActiveFalse(): List<UserView>

    fun findByRole(role: UserRole): List<UserView>

    fun findByRoleIn(roles: List<UserRole>): List<UserView>

    fun findByRoleAndActive(role: UserRole, active: Boolean): List<UserView>

    @Query(
        """
        SELECT u FROM UserView u 
        WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :namePattern, '%'))
        ORDER BY u.fullName ASC
        """
    )
    fun findByFullNameContaining(@Param("namePattern") namePattern: String): List<UserView>

    @Query(
        """
        SELECT u FROM UserView u 
        WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))
        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))
        ORDER BY u.fullName ASC
        """
    )
    fun findByFirstNameOrLastNameContaining(
        @Param("firstName") firstName: String,
        @Param("lastName") lastName: String
    ): List<UserView>

    @Query(
        """
        SELECT u FROM UserView u 
        WHERE u.email LIKE CONCAT('%@', :domain)
        ORDER BY u.email ASC
        """
    )
    fun findByEmailDomain(@Param("domain") domain: String): List<UserView>

    fun findByCreatedAtBetween(startDate: Instant, endDate: Instant): List<UserView>

    fun findByLastUpdatedAtAfter(after: Instant): List<UserView>

    fun findByKeycloakUserIdIsNotNull(): List<UserView>

    fun findByKeycloakUserId(keycloakUserId: String): UserView?

    fun findByLastKeycloakSyncIsNull(): List<UserView>

    fun findByLastSuccessfulLoginAfter(after: Instant): List<UserView>

    fun findByFailedLoginAttemptsGreaterThan(attempts: Int): List<UserView>

    fun findAllByOrderByFullNameAsc(pageable: Pageable): Page<UserView>

    fun findByActiveOrderByFullNameAsc(active: Boolean, pageable: Pageable): Page<UserView>

    fun findByRoleOrderByFullNameAsc(role: UserRole, pageable: Pageable): Page<UserView>

    @Query(
        """
        SELECT u FROM UserView u 
        WHERE (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
        AND (:fullName IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :fullName, '%')))
        AND (:role IS NULL OR u.role = :role)
        AND (:active IS NULL OR u.active = :active)
        ORDER BY u.fullName ASC
        """
    )
    fun findUsersWithFilters(
        @Param("email") email: String?,
        @Param("fullName") fullName: String?,
        @Param("role") role: UserRole?,
        @Param("active") active: Boolean?
    ): List<UserView>

    @Query(
        ("SELECT u FROM UserView u " +
                "WHERE (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
                "AND (:fullName IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))) " +
                "AND (:role IS NULL OR u.role = :role) " +
                "AND (:active IS NULL OR u.active = :active)")
    )
    fun findUsersWithFilters(
        @Param("email") email: String?,
        @Param("fullName") fullName: String?,
        @Param("role") role: UserRole?,
        @Param("active") active: Boolean?,
        pageable: Pageable
    ): Page<UserView>

    @Query(
        """
        SELECT COUNT(u) FROM UserView u WHERE u.active = true
        """
    )
    fun countActiveUsers(): Long

    @Query(
        """
        SELECT COUNT(u) FROM UserView u WHERE u.active = false
        """
    )
    fun countInactiveUsers(): Long

    @Query(
        """
        SELECT u.role, COUNT(u) 
        FROM UserView u 
        WHERE u.active = true 
        GROUP BY u.role
        """
    )
    fun countActiveUsersByRole(): List<Array<Any>>

    @Query(
        """
    SELECT 
        COUNT(CASE WHEN u.active = true THEN 1 END),
        COUNT(CASE WHEN u.active = false THEN 1 END),
        COUNT(CASE WHEN u.hasPassword = true THEN 1 END),
        COUNT(CASE WHEN u.keycloakUserId IS NOT NULL THEN 1 END),
        COUNT(CASE WHEN u.lastSuccessfulLogin IS NOT NULL THEN 1 END)
    FROM UserView u
    """
    )
    fun getUserStatistics(): Any

    // Recent activity queries
    @Query(
        """
        SELECT u FROM UserView u 
        WHERE u.createdAt >= :since
        ORDER BY u.createdAt DESC
        """
    )
    fun findRecentlyCreated(@Param("since") since: Instant): List<UserView>

    @Query(
        """
        SELECT u FROM UserView u 
        WHERE u.lastUpdatedAt >= :since
        ORDER BY u.lastUpdatedAt DESC
        """
    )
    fun findRecentlyUpdated(@Param("since") since: Instant): List<UserView>

    @Query(
        """
        SELECT u FROM UserView u 
        WHERE u.lastRoleChange >= :since
        ORDER BY u.lastRoleChange DESC
        """
    )
    fun findRecentRoleChanges(@Param("since") since: Instant): List<UserView>

    @Query(
        """
        SELECT u FROM UserView u 
        WHERE u.failedLoginAttempts >= :minAttempts
        AND u.active = true
        ORDER BY u.failedLoginAttempts DESC, u.viewUpdatedAt DESC
        """
    )
    fun findUsersWithFailedLogins(@Param("minAttempts") minAttempts: Int): List<UserView>

    @Query(
        """
        SELECT u FROM UserView u 
        WHERE u.active = false 
        AND u.deactivatedAt >= :since
        ORDER BY u.deactivatedAt DESC
        """
    )
    fun findRecentlyDeactivated(@Param("since") since: Instant): List<UserView>

    @Query(
        """
        SELECT u FROM UserView u 
        WHERE u.keycloakUserId IS NOT NULL 
        AND (u.lastKeycloakSync IS NULL OR u.lastKeycloakSync < :syncThreshold)
        ORDER BY u.lastKeycloakSync ASC NULLS FIRST
        """
    )
    fun findUsersNeedingKeycloakSync(@Param("syncThreshold") syncThreshold: Instant): List<UserView>
}