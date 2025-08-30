package mk.ukim.finki.examscheduling.externalintegration.service.impl

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import mk.ukim.finki.examscheduling.externalintegration.client.AccreditationServiceClient
import mk.ukim.finki.examscheduling.externalintegration.domain.CourseDataCache
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.accreditation.AccreditationSubjectDto
import mk.ukim.finki.examscheduling.externalintegration.domain.enums.CourseStudyCycle
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamCourse
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamCourseProfessor
import mk.ukim.finki.examscheduling.externalintegration.domain.exceptions.ExternalServiceIntegrationException
import mk.ukim.finki.examscheduling.externalintegration.domain.isStale
import mk.ukim.finki.examscheduling.externalintegration.infrastructure.DataQualityValidator
import mk.ukim.finki.examscheduling.externalintegration.infrastructure.ExternalServiceMetricsCollector
import mk.ukim.finki.examscheduling.externalintegration.service.AccreditationAntiCorruptionLayer
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class AccreditationAntiCorruptionLayerImpl(
    private val accreditationServiceClient: AccreditationServiceClient,
    private val courseDataCache: CourseDataCache,
    private val dataQualityValidator: DataQualityValidator,
    private val metricsCollector: ExternalServiceMetricsCollector
) : AccreditationAntiCorruptionLayer {

    private val logger = LoggerFactory.getLogger(AccreditationAntiCorruptionLayerImpl::class.java)

    @Retryable(value = [Exception::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
    @CircuitBreaker(name = "accreditation-service", fallbackMethod = "getCourseInformationFallback")
    override fun getCourseInformation(courseId: String): ExamCourse? {
        return try {
            metricsCollector.incrementAccreditationApiCall("getCourseInformation")

            val cachedCourse = courseDataCache.getCourse(courseId)
            if (cachedCourse != null && !cachedCourse.isStale()) {
                logger.debug("Returning cached course data for: {}", courseId)
                return cachedCourse
            }

            val subjectDto = accreditationServiceClient.getSubjectByCode(courseId)
            val examCourse = mapAccreditationSubjectToExamCourse(subjectDto)

            val validationResult = dataQualityValidator.validateCourseData(examCourse)
            if (!validationResult.isValid) {
                logger.warn("Course data validation failed for {}: {}", courseId, validationResult.errors)
                metricsCollector.incrementDataValidationFailure("course", courseId)
            }

            courseDataCache.cacheCourse(examCourse)
            metricsCollector.recordSuccessfulApiCall("accreditation", "getCourseInformation")

            examCourse
        } catch (e: Exception) {
            logger.error("Failed to get course information for: {}", courseId, e)
            metricsCollector.recordFailedApiCall("accreditation", "getCourseInformation", e)
            throw ExternalServiceIntegrationException("Failed to fetch course information", e)
        }
    }

    override fun getAllCourses(): List<ExamCourse> {
        return try {
            metricsCollector.incrementAccreditationApiCall("getAllCourses")

            val cachedCourses = courseDataCache.getAllCourses()
            if (cachedCourses.isNotEmpty() && !courseDataCache.isStale()) {
                logger.debug("Returning {} cached courses", cachedCourses.size)
                return cachedCourses
            }

            val studyProgramsDto = accreditationServiceClient.getAllStudyPrograms()
            val allSubjectsDto = mutableListOf<AccreditationSubjectDto>()

            studyProgramsDto.forEach { program ->
                val programSubjects = getMockSubjectsForProgram(program.code)
                allSubjectsDto.addAll(programSubjects)
            }

            val examCourses = allSubjectsDto.map { subjectDto ->
                mapAccreditationSubjectToExamCourse(subjectDto)
            }

            val validCourses = examCourses.filter { course ->
                val validationResult = dataQualityValidator.validateCourseData(course)
                if (!validationResult.isValid) {
                    logger.warn("Filtering out invalid course {}: {}", course.courseId, validationResult.errors)
                    metricsCollector.incrementDataValidationFailure("course", course.courseId)
                    false
                } else {
                    true
                }
            }

            courseDataCache.cacheAllCourses(validCourses)
            metricsCollector.recordSuccessfulApiCall("accreditation", "getAllCourses")

            validCourses
        } catch (e: Exception) {
            logger.error("Failed to get all courses", e)
            metricsCollector.recordFailedApiCall("accreditation", "getAllCourses", e)
            throw ExternalServiceIntegrationException("Failed to fetch all courses", e)
        }
    }

    override fun getCoursesByStudyProgram(studyProgramId: String): List<ExamCourse> {
        return try {
            val allCourses = getAllCourses()
            allCourses.filter { course ->
                course.courseCode.startsWith(studyProgramId.take(3))
            }
        } catch (e: Exception) {
            logger.error("Failed to get courses for study program: {}", studyProgramId, e)
            throw ExternalServiceIntegrationException("Failed to fetch courses for study program", e)
        }
    }

    override fun getCoursesForSemester(semester: Int): List<ExamCourse> {
        return try {
            val allCourses = getAllCourses()
            allCourses.filter { course -> course.semester == semester }
        } catch (e: Exception) {
            logger.error("Failed to get courses for semester: {}", semester, e)
            throw ExternalServiceIntegrationException("Failed to fetch courses for semester", e)
        }
    }

    override fun refreshCourseCache() {
        try {
            logger.info("Refreshing course cache")
            courseDataCache.clearCache()
            getAllCourses()
            logger.info("Course cache refreshed successfully")
        } catch (e: Exception) {
            logger.error("Failed to refresh course cache", e)
            throw ExternalServiceIntegrationException("Failed to refresh course cache", e)
        }
    }

    fun getCourseInformationFallback(courseId: String, ex: Exception): ExamCourse? {
        logger.warn("Falling back for course information: {}, error: {}", courseId, ex.message)
        metricsCollector.incrementFallbackUsage("accreditation", "getCourseInformation")

        return courseDataCache.getCourse(courseId)
            ?: createFallbackCourse(courseId)
    }

    private fun mapAccreditationSubjectToExamCourse(subjectDto: AccreditationSubjectDto): ExamCourse {
        return ExamCourse(
            courseId = subjectDto.code,
            courseName = subjectDto.name,
            courseCode = subjectDto.code,
            credits = subjectDto.credits ?: 6.0f,
            isMandatory = subjectDto.placeholder != true,
            semester = subjectDto.semester ?: subjectDto.defaultSemester?.toInt() ?: 1,
            studyCycle = mapStudyCycle(subjectDto.studyCycle),
            professors = subjectDto.professors.map { professorId ->
                ExamCourseProfessor(
                    professorId = professorId,
                    fullName = "Professor $professorId",
                    email = "$professorId@university.edu.mk",
                    department = "Unknown",
                    isMainProfessor = subjectDto.professors.indexOf(professorId) == 0
                )
            },
            estimatedDurationMinutes = calculateExamDuration(subjectDto.credits ?: 6.0f),
            department = extractDepartmentFromCode(subjectDto.code),
            prerequisites = extractPrerequisites(subjectDto.dependencies),
            lastUpdated = Instant.now()
        )
    }

    private fun mapStudyCycle(studyCycle: String): CourseStudyCycle {
        return when (studyCycle.uppercase()) {
            "UNDERGRADUATE", "FIRST_CYCLE" -> CourseStudyCycle.UNDERGRADUATE
            "GRADUATE", "SECOND_CYCLE", "MASTER" -> CourseStudyCycle.GRADUATE
            "POSTGRADUATE", "THIRD_CYCLE", "DOCTORAL" -> CourseStudyCycle.POSTGRADUATE
            else -> CourseStudyCycle.UNDERGRADUATE
        }
    }

    private fun calculateExamDuration(credits: Float): Int {
        return when {
            credits <= 3.0f -> 90
            credits <= 6.0f -> 120
            else -> 150
        }
    }

    private fun extractDepartmentFromCode(code: String): String? {
        return when {
            code.startsWith("CS") -> "Computer Science"
            code.startsWith("MATH") -> "Mathematics"
            code.startsWith("PHYS") -> "Physics"
            code.startsWith("EE") -> "Electrical Engineering"
            else -> "General"
        }
    }

    private fun extractPrerequisites(dependencies: Map<String, Any>?): List<String> {
        return dependencies?.get("prerequisites") as? List<String> ?: emptyList()
    }

    private fun createFallbackCourse(courseId: String): ExamCourse {
        return ExamCourse(
            courseId = courseId,
            courseName = "Course $courseId",
            courseCode = courseId,
            credits = 6.0f,
            isMandatory = true,
            semester = 1,
            studyCycle = CourseStudyCycle.UNDERGRADUATE,
            professors = emptyList(),
            estimatedDurationMinutes = 120,
            department = "Unknown"
        )
    }

    private fun getMockSubjectsForProgram(programCode: String): List<AccreditationSubjectDto> {
        return listOf(
            AccreditationSubjectDto(
                code = "${programCode}101",
                name = "Introduction to ${programCode}",
                abbreviation = "${programCode}101",
                semester = 1,
                professors = listOf("PROF001", "PROF002"),
                weeklyLecturesClasses = 3,
                weeklyAuditoriumClasses = 1,
                weeklyLabClasses = 2,
                placeholder = false,
                nameEn = "Introduction to ${programCode}",
                defaultSemester = 1,
                credits = 6.0f,
                studyCycle = "UNDERGRADUATE",
                language = "mk",
                accreditation = "ACC001"
            ),
            AccreditationSubjectDto(
                code = "${programCode}201",
                name = "Advanced ${programCode}",
                abbreviation = "${programCode}201",
                semester = 3,
                professors = listOf("PROF003"),
                weeklyLecturesClasses = 3,
                weeklyAuditoriumClasses = 1,
                weeklyLabClasses = 0,
                placeholder = false,
                nameEn = "Advanced ${programCode}",
                defaultSemester = 3,
                credits = 6.0f,
                studyCycle = "UNDERGRADUATE",
                language = "mk",
                accreditation = "ACC001"
            )
        )
    }
}