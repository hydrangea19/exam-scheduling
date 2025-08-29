package mk.ukim.finki.examscheduling.sharedsecurity.domain.dto

import java.util.*

data class CurrentUserDetails(
    val userId: UUID,
    val email: String,
    val role: String,
    val fullName: String?,
    val authorities: List<String>
)