package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant

data class UserDeactivatedEvent(
    val userId: String,
    val deactivatedBy: String,
    val reason: String,
    val deactivatedAt: Instant = Instant.now()
)
