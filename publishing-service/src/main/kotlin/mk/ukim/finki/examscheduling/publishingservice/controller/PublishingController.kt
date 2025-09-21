package mk.ukim.finki.examscheduling.publishingservice.controller

import mk.ukim.finki.examscheduling.publishingservice.domain.PublishedScheduleResponse
import mk.ukim.finki.examscheduling.publishingservice.service.ExportService
import mk.ukim.finki.examscheduling.publishingservice.service.PublishingService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/publishing")
class PublishingController(
    private val publishingService: PublishingService,
    private val exportService: ExportService
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

    @GetMapping("/schedules/{scheduleId}/export/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    fun exportScheduleToExcel(@PathVariable scheduleId: UUID): ResponseEntity<ByteArray> {
        logger.info("Exporting schedule to Excel: {}", scheduleId)

        try {
            val excelBytes = exportService.exportScheduleToExcel(scheduleId)

            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=schedule_${scheduleId}.xlsx")
                .body(excelBytes)
        } catch (e: Exception) {
            logger.error("Failed to export Excel", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/schedules/{scheduleId}/export/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    fun exportScheduleToPdf(@PathVariable scheduleId: UUID): ResponseEntity<ByteArray> {
        logger.info("Exporting schedule to PDF: {}", scheduleId)

        try {
            val pdfBytes = exportService.exportScheduleToPdf(scheduleId)

            return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=schedule_${scheduleId}.pdf")
                .body(pdfBytes)
        } catch (e: Exception) {
            logger.error("Failed to export PDF", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}

