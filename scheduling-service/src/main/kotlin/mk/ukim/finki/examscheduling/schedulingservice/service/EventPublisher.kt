package mk.ukim.finki.examscheduling.schedulingservice.service

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(EventPublisher::class.java)

    fun publishEvent(topic: String, event: Any, key: String? = null) {
        val eventKey = key ?: UUID.randomUUID().toString()

        logger.info("Publishing event to topic: {}, key: {}, event: {}", topic, eventKey, event.javaClass.simpleName)

        kafkaTemplate.send(topic, eventKey, event).whenComplete { result, exception ->
            if (exception == null) {
                logger.info("Event published successfully to topic: {}", topic)
            } else {
                logger.error("Failed to publish event to topic: {}", topic, exception)
            }
        }
    }

    fun publishSchedulingEvent(event: Any, key: String? = null) {
        publishEvent("scheduling-events", event, key)
    }

    fun publishSystemNotification(event: Any, key: String? = null) {
        publishEvent("system-notifications", event, key)
    }
}