package mk.ukim.finki.examscheduling.externalintegration.service

import mk.ukim.finki.examscheduling.externalintegration.domain.courses.CourseEnrollment
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.CompleteExamCourseInfo
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.DataRefreshResult
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.ExamSchedulingResources
import mk.ukim.finki.examscheduling.externalintegration.domain.events.ExternalDataRefreshCompletedEvent
import mk.ukim.finki.examscheduling.externalintegration.domain.events.ExternalDataSynchronizedEvent
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamCourse
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamProfessor
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamRoom
import mk.ukim.finki.examscheduling.externalintegration.infrastructure.ExternalServiceMetricsCollector
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
@Transactional(readOnly = true)
class ExternalDataIntegrationOrchestrator(
    private val accreditationAntiCorruptionLayer: AccreditationAntiCorruptionLayer,
    private val raspredelbaAntiCorruptionLayer: RaspredelbaAntiCorruptionLayer,
    private val metricsCollector: ExternalServiceMetricsCollector,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    private val logger = LoggerFactory.getLogger(ExternalDataIntegrationOrchestrator::class.java)

    /**
     * Get complete course information including enrollment data
     */
    fun getCompleteExamCourseInfo(courseId: String, semesterCode: String): CompleteExamCourseInfo? {
        return try {
            logger.info("Fetching complete course info for: {} in semester: {}", courseId, semesterCode)

            val course = accreditationAntiCorruptionLayer.getCourseInformation(courseId)
            if (course == null) {
                logger.warn("Course not found: {}", courseId)
                return null
            }

            val enrollment = raspredelbaAntiCorruptionLayer.getEnrollmentData(courseId, semesterCode)
            val professors = course.professors.mapNotNull { courseProf ->
                raspredelbaAntiCorruptionLayer.getProfessorAvailability(courseProf.professorId)
            }

            val completeInfo = CompleteExamCourseInfo(
                course = course,
                enrollment = enrollment,
                availableProfessors = professors,
                dataCompleteness = calculateDataCompleteness(course, enrollment, professors),
                lastSynchronized = Instant.now()
            )

            applicationEventPublisher.publishEvent(
                ExternalDataSynchronizedEvent(
                    courseId = courseId,
                    semesterCode = semesterCode,
                    dataQuality = completeInfo.dataCompleteness
                )
            )

            completeInfo
        } catch (e: Exception) {
            logger.error("Failed to get complete course info for: {}", courseId, e)
            null
        }
    }

    /**
     * Get all available resources for exam scheduling
     */
    fun getAllAvailableResources(): ExamSchedulingResources {
        return try {
            logger.info("Fetching all available resources for exam scheduling")

            val courses = accreditationAntiCorruptionLayer.getAllCourses()
            val professors = raspredelbaAntiCorruptionLayer.getAllAvailableProfessors()
            val rooms = raspredelbaAntiCorruptionLayer.getAllAvailableClassrooms()
            val semesterInfo = raspredelbaAntiCorruptionLayer.getCurrentSemesterInfo()

            ExamSchedulingResources(
                availableCourses = courses,
                availableProfessors = professors,
                availableRooms = rooms,
                currentSemester = semesterInfo,
                lastSynchronized = Instant.now(),
                resourceCompleteness = calculateResourceCompleteness(courses, professors, rooms)
            )
        } catch (e: Exception) {
            logger.error("Failed to get all available resources", e)
            ExamSchedulingResources(
                availableCourses = emptyList(),
                availableProfessors = emptyList(),
                availableRooms = emptyList(),
                currentSemester = null,
                lastSynchronized = Instant.now(),
                resourceCompleteness = 0.0
            )
        }
    }

    /**
     * Refresh all external data caches
     */
    fun refreshAllExternalData(): DataRefreshResult {
        return try {
            logger.info("Starting full external data refresh")
            val startTime = Instant.now()

            val results = mutableMapOf<String, Boolean>()

            try {
                accreditationAntiCorruptionLayer.refreshCourseCache()
                results["accreditation"] = true
            } catch (e: Exception) {
                logger.error("Failed to refresh accreditation cache", e)
                results["accreditation"] = false
            }

            try {
                raspredelbaAntiCorruptionLayer.refreshAvailabilityCache()
                results["raspredelba"] = true
            } catch (e: Exception) {
                logger.error("Failed to refresh raspredelba cache", e)
                results["raspredelba"] = false
            }

            val endTime = Instant.now()
            val duration = Duration.between(startTime, endTime)

            val refreshResult = DataRefreshResult(
                success = results.values.all { it },
                serviceResults = results,
                duration = duration,
                refreshedAt = endTime
            )

            applicationEventPublisher.publishEvent(
                ExternalDataRefreshCompletedEvent(refreshResult)
            )

            logger.info("External data refresh completed in {} ms", duration.toMillis())
            refreshResult
        } catch (e: Exception) {
            logger.error("Failed to refresh external data", e)
            DataRefreshResult(
                success = false,
                serviceResults = mapOf("error" to false),
                duration = Duration.ZERO,
                refreshedAt = Instant.now()
            )
        }
    }

    private fun calculateDataCompleteness(
        course: ExamCourse?,
        enrollment: CourseEnrollment?,
        professors: List<ExamProfessor>
    ): Double {
        var completeness = 0.0
        if (course != null) completeness += 0.5
        if (enrollment != null) completeness += 0.3
        if (professors.isNotEmpty()) completeness += 0.2
        return completeness
    }

    private fun calculateResourceCompleteness(
        courses: List<ExamCourse>,
        professors: List<ExamProfessor>,
        rooms: List<ExamRoom>
    ): Double {
        val courseScore = if (courses.isNotEmpty()) 0.4 else 0.0
        val professorScore = if (professors.isNotEmpty()) 0.3 else 0.0
        val roomScore = if (rooms.isNotEmpty()) 0.3 else 0.0
        return courseScore + professorScore + roomScore
    }
}