package mk.ukim.finki.examscheduling.preferencemanagement.service

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class EventConsumer {
    private val logger = LoggerFactory.getLogger(EventConsumer::class.java)

    @KafkaListener(topics = ["system-notifications"], groupId = "preference-management-service")
    fun handleSystemNotification(
        @Payload message: Map<String, Any>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info(
                "Received system notification: topic={}, offset={}, message={}",
                topic, offset, message
            )

            val notificationType = message["notificationType"] as? String
            val messageContent = message["message"] as? String

            logger.info("Processing system notification: type={}, message={}", notificationType, messageContent)

            acknowledgment.acknowledge()

        } catch (e: Exception) {
            logger.error("Error processing system notification: {}", e.message, e)
        }
    }

    @KafkaListener(topics = ["audit-events"], groupId = "preference-management-service")
    fun handleAuditEvent(
        @Payload message: Map<String, Any>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info("Received audit event: topic={}, message={}", topic, message)

            acknowledgment.acknowledge()

        } catch (e: Exception) {
            logger.error("Error processing audit event: {}", e.message, e)
        }
    }
}