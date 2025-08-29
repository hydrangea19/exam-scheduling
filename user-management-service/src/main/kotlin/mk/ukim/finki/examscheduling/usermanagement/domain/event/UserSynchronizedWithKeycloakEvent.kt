package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant

data class UserSynchronizedWithKeycloakEvent(
    val userId: String,
    val keycloakUserId: String,
    val synchronizedAt: Instant = Instant.now(),
    val keycloakData: Map<String, Any>
)