package mk.ukim.finki.examscheduling.usermanagement.query.handler

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import mk.ukim.finki.examscheduling.usermanagement.query.UserActivitySummary
import mk.ukim.finki.examscheduling.usermanagement.query.UserPageResponse
import mk.ukim.finki.examscheduling.usermanagement.query.UserStatisticsResult
import mk.ukim.finki.examscheduling.usermanagement.query.UserView
import mk.ukim.finki.examscheduling.usermanagement.query.queries.*
import mk.ukim.finki.examscheduling.usermanagement.query.repository.UserViewRepository
import org.axonframework.queryhandling.QueryHandler
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class UserQueryHandler(
    private val userViewRepository: UserViewRepository,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(UserQueryHandler::class.java)

    @QueryHandler
    fun handle(query: FindUserByIdQuery): UserView? {
        logger.debug("Handling FindUserByIdQuery for userId: {}", query.userId)
        return userViewRepository.findById(query.userId).orElse(null)
    }

    @QueryHandler
    fun handle(query: FindUserByEmailQuery): UserView? {
        logger.debug("Handling FindUserByEmailQuery for email: {}", query.email)
        return userViewRepository.findByEmail(query.email)
    }

    @QueryHandler
    fun handle(query: FindAllUsersQuery): UserPageResponse {
        logger.debug("Handling FindAllUsersQuery - page: {}, size: {}", query.page, query.size)

        val sort = Sort.by(
            if (query.sortDirection.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC,
            query.sortBy
        )

        val page = userViewRepository.findAll(
            PageRequest.of(
                query.page,
                query.size,
                Sort.by(Sort.Direction.fromString(query.sortDirection), query.sortBy)
            )
        )
        return UserPageResponse(
            users = page.content,
            page = page.number,
            size = page.size,
            totalPages = page.totalPages,
            totalElements = page.totalElements
        )
    }

    @QueryHandler
    fun handle(query: FindActiveUsersQuery): UserPageResponse {
        logger.debug("Handling FindActiveUsersQuery - active: {}", query.active)

        val pageable = PageRequest.of(query.page, query.size)
        val page = userViewRepository.findByActiveOrderByFullNameAsc(query.active, pageable)

        return UserPageResponse(
            users = page.content,
            page = page.number,
            size = page.size,
            totalPages = page.totalPages,
            totalElements = page.totalElements
        )
    }

    @QueryHandler
    fun handle(query: FindUsersByRoleQuery): UserPageResponse {
        logger.debug("Handling FindUsersByRoleQuery for role: {}", query.role)

        val pageable = PageRequest.of(query.page, query.size)

        val page: Page<UserView> = if (query.activeOnly) {
            userViewRepository.findByRoleOrderByFullNameAsc(query.role, pageable)
        } else {
            val allUsers = userViewRepository.findByRole(query.role)
            val totalElements = allUsers.size.toLong()
            val startIndex = (query.page * query.size).coerceAtMost(allUsers.size)
            val endIndex = ((query.page + 1) * query.size).coerceAtMost(allUsers.size)
            val pageUsers = if (startIndex < allUsers.size) allUsers.subList(startIndex, endIndex) else emptyList()
            PageImpl(pageUsers, pageable, totalElements)
        }

        return UserPageResponse(
            users = page.content,
            page = page.number,
            size = page.size,
            totalPages = page.totalPages,
            totalElements = page.totalElements
        )
    }

    @QueryHandler
    fun handle(query: FindUsersByRolesQuery): List<UserView> {
        logger.debug("Handling FindUsersByRolesQuery for roles: {}", query.roles)

        return if (query.activeOnly) {
            userViewRepository.findByRoleIn(query.roles).filter { it.active }
        } else {
            userViewRepository.findByRoleIn(query.roles)
        }
    }

    @QueryHandler
    fun handle(query: SearchUsersByNameQuery): List<UserView> {
        logger.debug("Handling SearchUsersByNameQuery for pattern: {}", query.namePattern)

        val users = userViewRepository.findByFullNameContaining(query.namePattern)
        return if (query.activeOnly) {
            users.filter { it.active }
        } else {
            users
        }
    }

    @QueryHandler
    fun handle(query: SearchUsersWithFiltersQuery): UserPageResponse {
        logger.debug(
            "Handling SearchUsersWithFiltersQuery with filters: email={}, fullName={}, role={}, active={}",
            query.email, query.fullName, query.role, query.active
        )

        val sort = Sort.by(
            if (query.sortDirection.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC,
            query.sortBy
        )

        val pageable = PageRequest.of(query.page, query.size, sort)

        val page: Page<UserView> = userViewRepository.findUsersWithFilters(
            email = query.email,
            fullName = query.fullName,
            role = query.role,
            active = query.active,
            pageable = pageable
        )

        return UserPageResponse(
            users = page.content,
            page = page.number,
            size = page.size,
            totalPages = page.totalPages,
            totalElements = page.totalElements
        )
    }

    @QueryHandler
    fun handle(query: FindUsersCreatedBetweenQuery): List<UserView> {
        logger.debug(
            "Handling FindUsersCreatedBetweenQuery from {} to {}",
            query.startDate, query.endDate
        )

        return userViewRepository.findByCreatedAtBetween(query.startDate, query.endDate)
    }

    @QueryHandler
    fun handle(query: FindUsersUpdatedAfterQuery): List<UserView> {
        logger.debug("Handling FindUsersUpdatedAfterQuery after: {}", query.after)

        return userViewRepository.findByLastUpdatedAtAfter(query.after)
    }

    @QueryHandler
    fun handle(query: FindKeycloakUsersQuery): List<UserView> {
        logger.debug("Handling FindKeycloakUsersQuery - syncedOnly: {}", query.syncedOnly)

        return if (query.syncedOnly) {
            userViewRepository.findByKeycloakUserIdIsNotNull()
        } else {
            userViewRepository.findAll()
        }
    }

    @QueryHandler
    fun handle(query: FindUserByKeycloakIdQuery): UserView? {
        logger.debug("Handling FindUserByKeycloakIdQuery for keycloakId: {}", query.keycloakUserId)

        return userViewRepository.findByKeycloakUserId(query.keycloakUserId)
    }

    @QueryHandler
    fun handle(query: FindUsersNeedingKeycloakSyncQuery): List<UserView> {
        logger.debug(
            "Handling FindUsersNeedingKeycloakSyncQuery with threshold: {} hours",
            query.syncThresholdHours
        )

        val syncThreshold = Instant.now().minus(query.syncThresholdHours, ChronoUnit.HOURS)
        return userViewRepository.findUsersNeedingKeycloakSync(syncThreshold)
    }

    @QueryHandler
    fun handle(query: FindUsersWithFailedLoginsQuery): List<UserView> {
        logger.debug(
            "Handling FindUsersWithFailedLoginsQuery with min attempts: {}",
            query.minFailedAttempts
        )

        return userViewRepository.findUsersWithFailedLogins(query.minFailedAttempts)
    }

    @QueryHandler
    fun handle(query: FindUsersWithRecentLoginQuery): List<UserView> {
        logger.debug("Handling FindUsersWithRecentLoginQuery since: {}", query.since)

        return userViewRepository.findByLastSuccessfulLoginAfter(query.since)
    }

    @QueryHandler
    fun handle(query: FindRecentlyDeactivatedUsersQuery): List<UserView> {
        logger.debug("Handling FindRecentlyDeactivatedUsersQuery since: {}", query.since)

        return userViewRepository.findRecentlyDeactivated(query.since)
    }

    @QueryHandler
    fun handle(query: FindRecentRoleChangesQuery): List<UserView> {
        logger.debug("Handling FindRecentRoleChangesQuery since: {}", query.since)

        return userViewRepository.findRecentRoleChanges(query.since)
    }

    @QueryHandler
    fun handle(query: GetUserStatisticsQuery): UserStatisticsResult {
        logger.debug("Handling GetUserStatisticsQuery")

        val row = userViewRepository.getUserStatistics() as Array<*>

        val activeUsers = (row[0] as Number).toLong()
        val inactiveUsers = (row[1] as Number).toLong()
        val usersWithPassword = (row[2] as Number).toLong()
        val keycloakUsers = (row[3] as Number).toLong()
        val usersWithLogin = (row[4] as Number).toLong()

        val roleBreakdown = if (query.includeRoleBreakdown) {
            userViewRepository.countActiveUsersByRole()
                .associate { r -> (r[0] as UserRole).name to (r[1] as Number).toLong() }
        } else {
            emptyMap()
        }

        return UserStatisticsResult(
            totalUsers = activeUsers + inactiveUsers,
            activeUsers = activeUsers,
            inactiveUsers = inactiveUsers,
            usersWithPassword = usersWithPassword,
            keycloakUsers = keycloakUsers,
            usersWithRecentLogin = usersWithLogin,
            roleBreakdown = roleBreakdown,
            generatedAt = Instant.now()
        )
    }

    @QueryHandler
    fun handle(query: GetUserCountByRoleQuery): Map<String, Long> {
        logger.debug("Handling GetUserCountByRoleQuery - activeOnly: {}", query.activeOnly)

        return if (query.activeOnly) {
            userViewRepository.countActiveUsersByRole()
                .associate { row ->
                    val role = row[0] as mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
                    val count = (row[1] as Number).toLong()
                    role.name to count
                }
        } else {
            userViewRepository.findAll()
                .groupBy { it.role }
                .mapKeys { it.key.name }
                .mapValues { it.value.size.toLong() }
        }
    }

    @QueryHandler
    fun handle(query: FindUsersByEmailDomainQuery): List<UserView> {
        logger.debug("Handling FindUsersByEmailDomainQuery for domain: {}", query.domain)

        val users = userViewRepository.findByEmailDomain(query.domain)
        return if (query.activeOnly) {
            users.filter { it.active }
        } else {
            users
        }
    }

    @QueryHandler
    fun handle(query: FindUserActivitySummaryQuery): UserActivitySummary? {
        logger.debug("Handling FindUserActivitySummaryQuery for user: {}", query.userId)

        val userView = userViewRepository.findById(query.userId).orElse(null)
            ?: return null

        return UserActivitySummary(
            userId = userView.userId,
            email = userView.email,
            fullName = userView.fullName,
            role = userView.role,
            active = userView.active,
            createdAt = userView.createdAt,
            lastUpdatedAt = userView.lastUpdatedAt,
            lastSuccessfulLogin = userView.lastSuccessfulLogin,
            failedLoginAttempts = userView.failedLoginAttempts,
            lastRoleChange = userView.lastRoleChange,
            lastRoleChangedBy = userView.lastRoleChangedBy,
            keycloakUserId = userView.keycloakUserId,
            lastKeycloakSync = userView.lastKeycloakSync,
            notificationPreferences = parseJsonToMap(userView.notificationPreferences),
            uiPreferences = parseJsonToMap(userView.uiPreferences),
            deactivationReason = userView.deactivationReason,
            deactivatedBy = userView.deactivatedBy,
            deactivatedAt = userView.deactivatedAt
        )
    }

    @QueryHandler
    fun handle(query: FindUsersForMaintenanceQuery): List<UserView> {
        logger.debug("Handling FindUsersForMaintenanceQuery")

        val allUsers = userViewRepository.findAll()

        return allUsers.filter { user ->
            var includeUser = false

            if (query.includeInactiveUsers && !user.active) {
                includeUser = true
            }

            if (query.includeUsersWithoutPasswords && !user.hasPassword) {
                includeUser = true
            }

            if (query.includeUsersNeedingSync && user.keycloakUserId != null) {
                val syncThreshold = Instant.now().minus(24, ChronoUnit.HOURS)
                if (user.lastKeycloakSync == null || user.lastKeycloakSync.isBefore(syncThreshold)) {
                    includeUser = true
                }
            }

            includeUser
        }
    }

    private fun parseJsonToMap(jsonString: String?): Map<String, Any> {
        return if (jsonString != null) {
            try {
                objectMapper.readValue(jsonString, object : TypeReference<Map<String, Any>>() {})
            } catch (e: Exception) {
                logger.warn("Failed to parse JSON preferences: {}", jsonString, e)
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }
}