package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant

data class LoginAttemptRecordedEvent(
    val userId: String,
    val successful: Boolean,
    val ipAddress: String,
    val userAgent: String,
    val attemptedAt: Instant
)
