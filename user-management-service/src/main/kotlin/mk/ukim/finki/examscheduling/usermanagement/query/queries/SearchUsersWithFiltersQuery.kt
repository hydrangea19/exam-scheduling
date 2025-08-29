package mk.ukim.finki.examscheduling.usermanagement.query.queries

import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole

data class SearchUsersWithFiltersQuery(
    val email: String? = null,
    val fullName: String? = null,
    val role: UserRole? = null,
    val active: Boolean? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "fullName",
    val sortDirection: String = "ASC"
)