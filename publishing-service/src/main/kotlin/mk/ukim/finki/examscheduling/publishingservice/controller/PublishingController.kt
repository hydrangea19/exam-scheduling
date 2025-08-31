package mk.ukim.finki.examscheduling.publishingservice.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/publishing")
class PublishingController(
    private val publishingService: PublishingService
) {

    private val logger = LoggerFactory.getLogger(PublishingController::class.java)

    @GetMapping("/schedules/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    fun getPublishedSchedule(@PathVariable scheduleId: UUID): ResponseEntity<PublishedScheduleResponse> {
        logger.info("Fetching published schedule: {}", scheduleId)

        val publishedSchedule = publishingService.getPublishedSchedule(scheduleId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(publishedSchedule)
    }
}

