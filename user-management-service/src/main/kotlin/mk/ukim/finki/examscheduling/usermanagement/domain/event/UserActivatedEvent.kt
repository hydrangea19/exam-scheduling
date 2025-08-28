package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant
import java.util.*

data class UserActivatedEvent(
    val userId: UUID,
    val activatedBy: UUID? = null,
    val reason: String? = null,
    val activatedAt: Instant = Instant.now()
)
