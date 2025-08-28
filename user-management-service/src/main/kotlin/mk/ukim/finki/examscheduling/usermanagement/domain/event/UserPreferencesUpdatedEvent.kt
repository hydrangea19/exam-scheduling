package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant
import java.util.*

data class UserPreferencesUpdatedEvent(
    val userId: UUID,
    val notificationPreferences: Map<String, Boolean>,
    val uiPreferences: Map<String, String>,
    val updatedAt: Instant = Instant.now()
)