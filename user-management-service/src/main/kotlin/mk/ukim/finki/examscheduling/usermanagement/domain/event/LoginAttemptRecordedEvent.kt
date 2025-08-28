package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant
import java.util.*

data class LoginAttemptRecordedEvent(
    val userId: UUID,
    val successful: Boolean,
    val ipAddress: String,
    val userAgent: String,
    val attemptedAt: Instant
)
