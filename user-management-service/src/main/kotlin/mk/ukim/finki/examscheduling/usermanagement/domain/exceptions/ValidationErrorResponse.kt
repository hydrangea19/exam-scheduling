package mk.ukim.finki.examscheduling.usermanagement.domain.exceptions

import java.time.Instant

data class ValidationErrorResponse(
    val error: String = "Validation Failed",
    val message: String,
    val timestamp: Instant = Instant.now(),
    val fieldErrors: Map<String, String>? = null
)