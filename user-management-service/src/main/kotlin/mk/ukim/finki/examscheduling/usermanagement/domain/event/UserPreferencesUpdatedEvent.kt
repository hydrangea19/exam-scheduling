package mk.ukim.finki.examscheduling.usermanagement.domain.event

import java.time.Instant

data class UserPreferencesUpdatedEvent(
    val userId: String,
    val notificationPreferences: Map<String, Boolean>,
    val uiPreferences: Map<String, String>,
    val updatedAt: Instant = Instant.now()
)