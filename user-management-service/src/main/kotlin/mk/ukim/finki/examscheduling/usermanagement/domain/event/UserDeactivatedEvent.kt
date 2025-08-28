package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant
import java.util.*

data class UserDeactivatedEvent(
    val userId: UUID,
    val deactivatedBy: UUID,
    val reason: String,
    val deactivatedAt: Instant = Instant.now()
)
