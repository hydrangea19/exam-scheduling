package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant

data class UserActivatedEvent(
    val userId: String,
    val activatedBy: String? = null,
    val reason: String? = null,
    val activatedAt: Instant = Instant.now()
)
