package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant

data class UserProfileUpdatedEvent(
    val userId: String,
    val previousFirstName: String,
    val previousLastName: String,
    val previousMiddleName: String?,
    val newFirstName: String,
    val newLastName: String,
    val newMiddleName: String?,
    val updatedAt: Instant = Instant.now()
)
