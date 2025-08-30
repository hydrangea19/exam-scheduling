package mk.ukim.finki.examscheduling.externalintegration.controller

import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.CompleteExamCourseInfo
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.DataRefreshResult
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.ExamSchedulingResources
import mk.ukim.finki.examscheduling.externalintegration.service.ExternalDataIntegrationOrchestrator
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/external-integration")
@PreAuthorize("hasRole('ADMIN')")
class ExternalDataIntegrationController(
    private val orchestrator: ExternalDataIntegrationOrchestrator
) {

    @GetMapping("/course/{courseId}/semester/{semesterCode}")
    fun getCourseInfo(
        @PathVariable courseId: String,
        @PathVariable semesterCode: String
    ): ResponseEntity<CompleteExamCourseInfo> {
        val courseInfo = orchestrator.getCompleteExamCourseInfo(courseId, semesterCode)
        return if (courseInfo != null) {
            ResponseEntity.ok(courseInfo)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/resources")
    fun getAllResources(): ResponseEntity<ExamSchedulingResources> {
        val resources = orchestrator.getAllAvailableResources()
        return ResponseEntity.ok(resources)
    }

    @PostMapping("/refresh")
    fun refreshExternalData(): ResponseEntity<DataRefreshResult> {
        val result = orchestrator.refreshAllExternalData()
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.status(207).body(result)
        }
    }

    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "external-data-integration",
                "timestamp" to Instant.now().toString()
            )
        )
    }
}