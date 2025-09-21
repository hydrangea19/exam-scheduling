package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ViolationSeverity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@Service
@Transactional
class AdvancedSchedulingService(
    private val pythonSchedulingClient: PythonSchedulingClient,
) {

    private val logger = LoggerFactory.getLogger(AdvancedSchedulingService::class.java)

    fun generateOptimalSchedule(
        courseEnrollmentData: Map<String, CourseEnrollmentInfo>,
        courseAccreditationData: Map<String, CourseAccreditationInfo>,
        professorPreferences: List<ProfessorPreferenceInfo>,
        availableRooms: List<RoomInfo>,
        examPeriod: ExamPeriod,
        institutionalConstraints: InstitutionalConstraints? = null
    ): SchedulingResult {
        val startTime = Instant.now()
        logger.info(
            "Starting advanced schedule generation for {} courses with {} preferences",
            courseEnrollmentData.size, professorPreferences.size
        )

        try {
            val schedulingRequest = PythonSchedulingRequest(
                examPeriod = examPeriod,
                courses = courseEnrollmentData.map { (courseId, enrollment) ->
                    val accreditation = courseAccreditationData[courseId]
                    if (accreditation != null) {
                        CourseSchedulingInfo(
                            courseId = courseId,
                            courseName = accreditation.courseName,
                            studentCount = enrollment.studentCount,
                            professorIds = accreditation.professorIds.toList(),
                            mandatoryStatus = accreditation.mandatoryStatus,
                            estimatedDuration = calculateEstimatedDuration(accreditation.credits),
                            requiredEquipment = extractRequiredEquipment(accreditation.accreditationDetails),
                            accessibilityRequired = extractAccessibilityRequirement(accreditation.accreditationDetails),
                            specialRequirements = accreditation.accreditationDetails["specialRequirements"] as? String
                        )
                    } else null
                }.filterNotNull(),
                availableRooms = availableRooms,
                professorPreferences = professorPreferences,
                institutionalConstraints = institutionalConstraints ?: getDefaultConstraints()
            )

            val pythonResponse = try {
                pythonSchedulingClient.generateSchedule(schedulingRequest).get(30, TimeUnit.SECONDS)
            } catch (e: Exception) {
                logger.error("Failed to get response from Python service", e)
                throw e
            }

            val schedulingResult = if (pythonResponse.success) {
                pythonResponse.toSchedulingResult()
            } else {
                generateBasicFallbackSchedule(
                    courseEnrollmentData,
                    courseAccreditationData,
                    availableRooms,
                    examPeriod,
                    Exception(pythonResponse.errorMessage ?: "Python service failed")
                )
            }

            val endTime = Instant.now()
            val totalProcessingTime = Duration.between(startTime, endTime).toMillis()

            logger.info(
                "Advanced schedule generation completed in {}ms with quality score: {}",
                totalProcessingTime, schedulingResult.qualityScore
            )

            return schedulingResult

        } catch (e: Exception) {
            logger.error("Failed to generate schedule", e)

            return generateBasicFallbackSchedule(
                courseEnrollmentData, courseAccreditationData, availableRooms, examPeriod, e
            )
        }
    }

    private fun calculateEstimatedDuration(credits: Int): Int {
        return when (credits) {
            in 1..3 -> 90
            in 4..6 -> 120
            in 7..9 -> 180
            else -> 120
        }
    }

    private fun extractRequiredEquipment(details: Map<String, Any>): List<String> {
        return (details["requiredEquipment"] as? List<String>) ?: emptyList()
    }

    private fun extractAccessibilityRequirement(details: Map<String, Any>): Boolean {
        return details["accessibilityRequired"] as? Boolean ?: false
    }

    private fun getDefaultConstraints(): InstitutionalConstraints {
        return InstitutionalConstraints(
            workingHours = WorkingHours(LocalTime.of(8, 0), LocalTime.of(18, 0)),
            minimumExamDuration = 120,
            minimumGapMinutes = 30,
            maxExamsPerDay = 6,
            maxExamsPerRoom = 8,
            allowWeekendExams = false
        )
    }

    private fun generateBasicFallbackSchedule(
        courseEnrollmentData: Map<String, CourseEnrollmentInfo>,
        courseAccreditationData: Map<String, CourseAccreditationInfo>,
        availableRooms: List<RoomInfo>,
        examPeriod: ExamPeriod,
        originalException: Exception
    ): SchedulingResult {
        logger.error("Generating basic fallback schedule due to: {}", originalException.message)

        val fallbackExams = mutableListOf<ScheduledExamInfo>()
        var currentDate = examPeriod.startDate
        var currentTime = LocalTime.of(9, 0)

        courseEnrollmentData.forEach { (courseId, enrollment) ->
            val accreditation = courseAccreditationData[courseId]
            val room = availableRooms.find { it.capacity >= enrollment.studentCount }

            if (accreditation != null && room != null) {
                fallbackExams.add(
                    ScheduledExamInfo(
                        scheduledExamId = "${courseId}_fallback",
                        courseId = courseId,
                        courseName = accreditation.courseName,
                        examDate = currentDate,
                        startTime = currentTime,
                        endTime = currentTime.plusHours(2),
                        roomId = room.roomId,
                        roomName = room.roomName,
                        roomCapacity = room.capacity,
                        studentCount = enrollment.studentCount,
                        mandatoryStatus = accreditation.mandatoryStatus,
                        professorIds = accreditation.professorIds
                    )
                )

                currentTime = currentTime.plusHours(3)
                if (currentTime.isAfter(LocalTime.of(17, 0))) {
                    currentDate = currentDate.plusDays(1)
                    currentTime = LocalTime.of(9, 0)
                }
            }
        }

        return SchedulingResult(
            scheduledExams = fallbackExams,
            metrics = SchedulingMetrics(
                totalCoursesScheduled = fallbackExams.size,
                totalProfessorPreferencesConsidered = 0,
                preferencesSatisfied = 0,
                preferenceSatisfactionRate = 0.3,
                totalConflicts = 0,
                resolvedConflicts = 0,
                roomUtilizationRate = 0.5,
                averageStudentExamsPerDay = fallbackExams.size.toDouble() /
                        ((examPeriod.endDate.toEpochDay() - examPeriod.startDate.toEpochDay()).toInt() + 1),
                processingTimeMs = 100L
            ),
            qualityScore = 0.3,
            violations = listOf(
                ConstraintViolation(
                    violationType = "PYTHON_SERVICE_FAILURE",
                    severity = ViolationSeverity.HIGH,
                    description = "Python scheduling service failed: ${originalException.message}",
                    affectedExams = fallbackExams.map { it.courseId },
                    affectedStudents = fallbackExams.sumOf { it.studentCount },
                    suggestedResolution = "Check Python service availability and retry"
                )
            )
        )
    }
}