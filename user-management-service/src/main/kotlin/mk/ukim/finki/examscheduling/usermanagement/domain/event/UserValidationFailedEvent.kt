package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant
import java.util.*

data class UserValidationFailedEvent(
    val userId: UUID,
    val commandType: String,
    val validationErrors: List<String>,
    val failedAt: Instant = Instant.now()
)