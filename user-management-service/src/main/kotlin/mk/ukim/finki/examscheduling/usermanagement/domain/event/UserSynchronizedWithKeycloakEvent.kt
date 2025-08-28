package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant
import java.util.*

data class UserSynchronizedWithKeycloakEvent(
    val userId: UUID,
    val keycloakUserId: String,
    val synchronizedAt: Instant = Instant.now(),
    val keycloakData: Map<String, Any>
)