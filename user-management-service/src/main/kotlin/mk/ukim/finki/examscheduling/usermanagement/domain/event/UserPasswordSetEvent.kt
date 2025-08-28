package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant
import java.util.*

data class UserPasswordSetEvent(
    val userId: UUID,
    val hasPassword: Boolean = true,
    val setAt: Instant = Instant.now()
)