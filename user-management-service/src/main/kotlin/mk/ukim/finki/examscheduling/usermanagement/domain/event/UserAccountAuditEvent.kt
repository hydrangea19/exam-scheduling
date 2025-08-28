package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant
import java.util.*

data class UserAccountAuditEvent(
    val userId: UUID,
    val action: String,
    val performedBy: UUID,
    val details: Map<String, Any>,
    val timestamp: Instant = Instant.now()
)
