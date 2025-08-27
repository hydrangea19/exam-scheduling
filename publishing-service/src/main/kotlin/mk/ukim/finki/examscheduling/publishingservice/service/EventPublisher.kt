package mk.ukim.finki.examscheduling.publishingservice.service

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class EventPublisher(private val kafkaTemplate: KafkaTemplate<String, Any>) {
    private val logger = LoggerFactory.getLogger(EventPublisher::class.java)

    fun publishEvent(topic: String, event: Any, key: String? = null) {
        val eventKey = key ?: UUID.randomUUID().toString()
        kafkaTemplate.send(topic, eventKey, event)
        logger.info("Published event to {}: {}", topic, event.javaClass.simpleName)
    }

    fun publishPublishingEvent(event: Any) = publishEvent("publishing-events", event)
}