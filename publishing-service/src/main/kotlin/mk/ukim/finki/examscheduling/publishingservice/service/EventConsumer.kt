package mk.ukim.finki.examscheduling.publishingservice.service

import mk.ukim.finki.examscheduling.publishingservice.domain.PublishedSchedule
import mk.ukim.finki.examscheduling.publishingservice.domain.enums.PublicationStatus
import mk.ukim.finki.examscheduling.publishingservice.repository.PublishedScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.util.*

@Component
class EventConsumer(
    private val publishedScheduleRepository: PublishedScheduleRepository,
    private val eventPublisher: EventPublisher
) {
    private val logger = LoggerFactory.getLogger(EventConsumer::class.java)

    @KafkaListener(topics = ["scheduling-events"], groupId = "publishing-service")
    fun handleSchedulingEvent(@Payload message: Map<String, Any>, acknowledgment: Acknowledgment) {
        try {
            logger.info("Received scheduling event: {}", message)

            val eventType = message["eventType"] as? String
            if (eventType == "ScheduleReadyForGeneration") {
                val scheduleId = UUID.fromString(message["scheduleId"] as String)
                val sessionPeriodId = message["examSessionPeriodId"] as String

                val publishedSchedule = PublishedSchedule(
                    scheduleId = scheduleId,
                    examSessionPeriodId = sessionPeriodId,
                    academicYear = "2024-2025",
                    examSession = "AUTO_KAFKA",
                    title = "Auto-published from Kafka: $sessionPeriodId",
                    publicationStatus = PublicationStatus.DRAFT,
                    isPublic = false
                )
                publishedScheduleRepository.save(publishedSchedule)

                logger.info("Created published schedule from Kafka event: {}", publishedSchedule.id)
            }

            acknowledgment.acknowledge()
        } catch (e: Exception) {
            logger.error("Error processing scheduling event", e)
        }
    }
}