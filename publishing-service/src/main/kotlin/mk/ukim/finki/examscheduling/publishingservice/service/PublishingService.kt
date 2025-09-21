package mk.ukim.finki.examscheduling.publishingservice.service

import mk.ukim.finki.examscheduling.publishingservice.domain.PublishedScheduleResponse
import mk.ukim.finki.examscheduling.publishingservice.repository.PublishedScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class PublishingService(
    private val publishedScheduleRepository: PublishedScheduleRepository
) {

    private val logger = LoggerFactory.getLogger(PublishingService::class.java)

    fun getPublishedSchedule(scheduleId: UUID): PublishedScheduleResponse? {
        logger.debug("Looking up published schedule for scheduleId: {}", scheduleId)

        val publishedSchedule = publishedScheduleRepository.findByScheduleId(scheduleId)
            ?: return null

        logger.info("Found published schedule: {} for scheduleId: {}", publishedSchedule.id, scheduleId)

        return PublishedScheduleResponse(
            id = publishedSchedule.id,
            scheduleId = publishedSchedule.scheduleId,
            examSessionPeriodId = publishedSchedule.examSessionPeriodId,
            academicYear = publishedSchedule.academicYear,
            examSession = publishedSchedule.examSession,
            title = publishedSchedule.title,
            description = publishedSchedule.description,
            publicationStatus = publishedSchedule.publicationStatus.name,
            publishedAt = publishedSchedule.publishedAt?.toString(),
            publishedBy = publishedSchedule.publishedBy,
            isPublic = publishedSchedule.isPublic,
            createdAt = publishedSchedule.createdAt.toString()
        )
    }
}