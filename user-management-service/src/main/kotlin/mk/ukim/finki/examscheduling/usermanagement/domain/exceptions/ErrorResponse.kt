package mk.ukim.finki.examscheduling.usermanagement.domain.exceptions

import java.time.Instant

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val details: Map<String, Any>? = null
)