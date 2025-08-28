package mk.ukim.finki.examscheduling.usermanagement.query.queries

import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole

data class FindUsersByRoleQuery(
    val role: UserRole,
    val activeOnly: Boolean = true,
    val page: Int = 0,
    val size: Int = 20
)