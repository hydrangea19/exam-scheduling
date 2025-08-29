package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant

data class UserValidationFailedEvent(
    val userId: String,
    val commandType: String,
    val validationErrors: List<String>,
    val failedAt: Instant = Instant.now()
)