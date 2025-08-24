package mk.ukim.finki.examscheduling.preferencemanagement.service

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(EventPublisher::class.java)

    fun publishEvent(topic: String, event: Any, key: String? = null) {
        val eventKey = key ?: UUID.randomUUID().toString()

        logger.info("Publishing event to topic: {}, key: {}, event: {}", topic, eventKey, event.javaClass.simpleName)

        val future: CompletableFuture<SendResult<String, Any>> = kafkaTemplate.send(topic, eventKey, event)

        future.whenComplete { result, exception ->
            if (exception == null) {
                logger.info(
                    "Event published successfully to topic: {}, offset: {}",
                    topic, result?.recordMetadata?.offset()
                )
            } else {
                logger.error("Failed to publish event to topic: {}, error: {}", topic, exception.message, exception)
            }
        }
    }

    fun publishPreferenceEvent(event: Any, key: String? = null) {
        publishEvent("preference-management-events", event, key)
    }

    fun publishSystemNotification(event: Any, key: String? = null) {
        publishEvent("system-notifications", event, key)
    }
}