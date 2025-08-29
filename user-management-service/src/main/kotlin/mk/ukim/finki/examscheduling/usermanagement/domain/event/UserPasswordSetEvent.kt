package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant

data class UserPasswordSetEvent(
    val userId: String,
    val hasPassword: Boolean = true,
    val setAt: Instant = Instant.now()
)