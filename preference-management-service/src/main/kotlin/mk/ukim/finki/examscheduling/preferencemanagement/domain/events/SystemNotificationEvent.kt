package mk.ukim.finki.examscheduling.preferencemanagement.domain.events

data class SystemNotificationEvent(
    val notificationType: String,
    val message: String,
    val targetService: String? = null,
    val metadata: Map<String, Any> = emptyMap()
) : BaseEvent()
