package mk.ukim.finki.examscheduling.usermanagement.domain.event

import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import java.time.Instant

data class UserCreatedEvent(
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val middleName: String? = null,
    val role: UserRole,
    val hasPassword: Boolean = false,
    val createdAt: Instant = Instant.now()
)
