package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant

data class UserAccountAuditEvent(
    val userId: String,
    val action: String,
    val performedBy: String,
    val details: Map<String, Any>,
    val timestamp: Instant = Instant.now()
)
