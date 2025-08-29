package mk.ukim.finki.examscheduling.usermanagement.domain.dto.users

data class UpdatePreferencesRequest(
    val notificationPreferences: Map<String, Boolean>,
    val uiPreferences: Map<String, String>
)