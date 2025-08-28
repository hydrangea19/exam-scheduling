package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant
import java.util.*

data class UserEmailChangedEvent(
    val userId: UUID,
    val oldEmail: String,
    val newEmail: String,
    val changedAt: Instant = Instant.now()
)
