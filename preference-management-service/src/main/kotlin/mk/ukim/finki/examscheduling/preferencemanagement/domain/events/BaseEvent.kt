package mk.ukim.finki.examscheduling.preferencemanagement.domain.events

import java.time.Instant
import java.util.*

abstract class BaseEvent {
    val eventId: String = UUID.randomUUID().toString()
    val timestamp: Instant = Instant.now()
    val serviceName: String = "preference-management-service"
}