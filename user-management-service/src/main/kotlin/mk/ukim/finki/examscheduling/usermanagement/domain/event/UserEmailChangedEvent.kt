package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant

data class UserEmailChangedEvent(
    val userId: String,
    val oldEmail: String,
    val newEmail: String,
    val changedAt: Instant = Instant.now()
)
