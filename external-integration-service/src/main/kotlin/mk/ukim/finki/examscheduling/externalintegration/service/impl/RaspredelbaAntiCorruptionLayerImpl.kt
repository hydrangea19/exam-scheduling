package mk.ukim.finki.examscheduling.externalintegration.service.impl

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import mk.ukim.finki.examscheduling.externalintegration.client.RaspredelbaServiceClient
import mk.ukim.finki.examscheduling.externalintegration.domain.AvailabilityDataCache
import mk.ukim.finki.examscheduling.externalintegration.domain.courses.CourseEnrollment
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.lectureplanning.RaspredelbaClassroomDto
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.lectureplanning.RaspredelbaProfessorDto
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.lectureplanning.RaspredelbaSemesterDto
import mk.ukim.finki.examscheduling.externalintegration.domain.enums.ExamRoomType
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamProfessor
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamRoom
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.MaintenancePeriod
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.RoomAvailabilityPeriod
import mk.ukim.finki.examscheduling.externalintegration.domain.exceptions.ExternalServiceIntegrationException
import mk.ukim.finki.examscheduling.externalintegration.domain.isStale
import mk.ukim.finki.examscheduling.externalintegration.domain.professor.ProfessorAvailabilityPeriod
import mk.ukim.finki.examscheduling.externalintegration.infrastructure.DataQualityValidator
import mk.ukim.finki.examscheduling.externalintegration.infrastructure.ExternalServiceMetricsCollector
import mk.ukim.finki.examscheduling.externalintegration.service.RaspredelbaAntiCorruptionLayer
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class RaspredelbaAntiCorruptionLayerImpl(
    private val raspredelbaServiceClient: RaspredelbaServiceClient,
    private val availabilityDataCache: AvailabilityDataCache,
    private val dataQualityValidator: DataQualityValidator,
    private val metricsCollector: ExternalServiceMetricsCollector
) : RaspredelbaAntiCorruptionLayer {

    private val logger = LoggerFactory.getLogger(RaspredelbaAntiCorruptionLayerImpl::class.java)

    @Retryable(value = [Exception::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
    @CircuitBreaker(name = "raspredelba-service", fallbackMethod = "getProfessorAvailabilityFallback")
    override fun getProfessorAvailability(professorId: String): ExamProfessor? {
        return try {
            metricsCollector.incrementRaspredelbaApiCall("getProfessorAvailability")

            val cachedProfessor = availabilityDataCache.getProfessor(professorId)
            if (cachedProfessor != null && !cachedProfessor.isStale()) {
                logger.debug("Returning cached professor data for: {}", professorId)
                return cachedProfessor
            }

            val professorDto = raspredelbaServiceClient.getProfessorById(professorId)
            val examProfessor = mapRaspredelbaProfessorToExamProfessor(professorDto)

            val validationResult = dataQualityValidator.validateProfessorData(examProfessor)
            if (!validationResult.isValid) {
                logger.warn("Professor data validation failed for {}: {}", professorId, validationResult.errors)
                metricsCollector.incrementDataValidationFailure("professor", professorId)
            }

            availabilityDataCache.cacheProfessor(examProfessor)
            metricsCollector.recordSuccessfulApiCall("raspredelba", "getProfessorAvailability")

            examProfessor
        } catch (e: Exception) {
            logger.error("Failed to get professor availability for: {}", professorId, e)
            metricsCollector.recordFailedApiCall("raspredelba", "getProfessorAvailability", e)
            throw ExternalServiceIntegrationException("Failed to fetch professor availability", e)
        }
    }

    override fun getAllAvailableProfessors(): List<ExamProfessor> {
        return try {
            metricsCollector.incrementRaspredelbaApiCall("getAllAvailableProfessors")

            val cachedProfessors = availabilityDataCache.getAllProfessors()
            if (cachedProfessors.isNotEmpty() && !availabilityDataCache.isProfessorCacheStale()) {
                logger.debug("Returning {} cached professors", cachedProfessors.size)
                return cachedProfessors
            }

            val professorsDto = raspredelbaServiceClient.getAvailableProfessors()
            val examProfessors = professorsDto.map { dto ->
                mapRaspredelbaProfessorToExamProfessor(dto)
            }

            val validProfessors = examProfessors.filter { professor ->
                val validationResult = dataQualityValidator.validateProfessorData(professor)
                if (!validationResult.isValid) {
                    logger.warn(
                        "Filtering out invalid professor {}: {}",
                        professor.professorId,
                        validationResult.errors
                    )
                    metricsCollector.incrementDataValidationFailure("professor", professor.professorId)
                    false
                } else {
                    true
                }
            }

            availabilityDataCache.cacheAllProfessors(validProfessors)
            metricsCollector.recordSuccessfulApiCall("raspredelba", "getAllAvailableProfessors")

            validProfessors
        } catch (e: Exception) {
            logger.error("Failed to get all available professors", e)
            metricsCollector.recordFailedApiCall("raspredelba", "getAllAvailableProfessors", e)
            throw ExternalServiceIntegrationException("Failed to fetch available professors", e)
        }
    }

    override fun getProfessorsForSubject(subjectCode: String): List<ExamProfessor> {
        return try {
            val professorsDto = raspredelbaServiceClient.getProfessorsBySubject(subjectCode)
            professorsDto.map { dto -> mapRaspredelbaProfessorToExamProfessor(dto) }
        } catch (e: Exception) {
            logger.error("Failed to get professors for subject: {}", subjectCode, e)
            throw ExternalServiceIntegrationException("Failed to fetch professors for subject", e)
        }
    }

    @CircuitBreaker(name = "raspredelba-service", fallbackMethod = "getClassroomAvailabilityFallback")
    override fun getClassroomAvailability(classroomId: String): ExamRoom? {
        return try {
            metricsCollector.incrementRaspredelbaApiCall("getClassroomAvailability")

            val cachedRoom = availabilityDataCache.getRoom(classroomId)
            if (cachedRoom != null && !cachedRoom.isStale()) {
                logger.debug("Returning cached room data for: {}", classroomId)
                return cachedRoom
            }

            val classroomDto = raspredelbaServiceClient.getClassroomById(classroomId)
            val examRoom = mapRaspredelbaClassroomToExamRoom(classroomDto)

            availabilityDataCache.cacheRoom(examRoom)
            metricsCollector.recordSuccessfulApiCall("raspredelba", "getClassroomAvailability")

            examRoom
        } catch (e: Exception) {
            logger.error("Failed to get classroom availability for: {}", classroomId, e)
            metricsCollector.recordFailedApiCall("raspredelba", "getClassroomAvailability", e)
            throw ExternalServiceIntegrationException("Failed to fetch classroom availability", e)
        }
    }

    override fun getAllAvailableClassrooms(): List<ExamRoom> {
        return try {
            metricsCollector.incrementRaspredelbaApiCall("getAllAvailableClassrooms")

            val cachedRooms = availabilityDataCache.getAllRooms()
            if (cachedRooms.isNotEmpty() && !availabilityDataCache.isRoomCacheStale()) {
                logger.debug("Returning {} cached rooms", cachedRooms.size)
                return cachedRooms
            }

            val classroomsDto = raspredelbaServiceClient.getAvailableClassrooms()
            val examRooms = classroomsDto.map { dto ->
                mapRaspredelbaClassroomToExamRoom(dto)
            }

            availabilityDataCache.cacheAllRooms(examRooms)
            metricsCollector.recordSuccessfulApiCall("raspredelba", "getAllAvailableClassrooms")

            examRooms
        } catch (e: Exception) {
            logger.error("Failed to get all available classrooms", e)
            metricsCollector.recordFailedApiCall("raspredelba", "getAllAvailableClassrooms", e)
            throw ExternalServiceIntegrationException("Failed to fetch available classrooms", e)
        }
    }

    override fun getEnrollmentData(courseId: String, semesterCode: String): CourseEnrollment? {
        return try {
            metricsCollector.incrementRaspredelbaApiCall("getEnrollmentData")

            val mockEnrollment = createMockEnrollmentData(courseId, semesterCode)

            metricsCollector.recordSuccessfulApiCall("raspredelba", "getEnrollmentData")
            mockEnrollment
        } catch (e: Exception) {
            logger.error("Failed to get enrollment data for course: {} semester: {}", courseId, semesterCode, e)
            metricsCollector.recordFailedApiCall("raspredelba", "getEnrollmentData", e)
            null
        }
    }

    override fun getCurrentSemesterInfo(): RaspredelbaSemesterDto? {
        return try {
            metricsCollector.incrementRaspredelbaApiCall("getCurrentSemesterInfo")

            val currentSemester = RaspredelbaSemesterDto(
                code = "2024-WINTER",
                year = "2024",
                startDate = "2024-09-15T00:00:00Z",
                endDate = "2025-01-31T23:59:59Z",
                enrollmentStartDate = "2024-08-01T00:00:00Z",
                enrollmentEndDate = "2024-09-10T23:59:59Z",
                semesterType = "WINTER",
                semesterCycle = "UNDERGRADUATE",
                semesterStatus = "ACTIVE"
            )

            metricsCollector.recordSuccessfulApiCall("raspredelba", "getCurrentSemesterInfo")
            currentSemester
        } catch (e: Exception) {
            logger.error("Failed to get current semester info", e)
            metricsCollector.recordFailedApiCall("raspredelba", "getCurrentSemesterInfo", e)
            null
        }
    }

    override fun refreshAvailabilityCache() {
        try {
            logger.info("Refreshing availability cache")
            availabilityDataCache.clearCache()
            getAllAvailableProfessors()
            getAllAvailableClassrooms()
            logger.info("Availability cache refreshed successfully")
        } catch (e: Exception) {
            logger.error("Failed to refresh availability cache", e)
            throw ExternalServiceIntegrationException("Failed to refresh availability cache", e)
        }
    }

    fun getProfessorAvailabilityFallback(professorId: String, ex: Exception): ExamProfessor? {
        logger.warn("Falling back for professor availability: {}, error: {}", professorId, ex.message)
        metricsCollector.incrementFallbackUsage("raspredelba", "getProfessorAvailability")

        return availabilityDataCache.getProfessor(professorId)
            ?: createFallbackProfessor(professorId)
    }

    fun getClassroomAvailabilityFallback(classroomId: String, ex: Exception): ExamRoom? {
        logger.warn("Falling back for classroom availability: {}, error: {}", classroomId, ex.message)
        metricsCollector.incrementFallbackUsage("raspredelba", "getClassroomAvailability")

        return availabilityDataCache.getRoom(classroomId)
            ?: createFallbackRoom(classroomId)
    }

    private fun mapRaspredelbaProfessorToExamProfessor(dto: RaspredelbaProfessorDto): ExamProfessor {
        return ExamProfessor(
            professorId = dto.id,
            firstName = dto.firstName,
            lastName = dto.lastName,
            email = dto.email,
            department = dto.department,
            isActive = dto.status.uppercase() == "ACTIVE",
            teachingSubjects = dto.teachesSubjects,
            availabilityPeriod = ProfessorAvailabilityPeriod(
                availableFrom = Instant.parse(dto.availableFrom),
                availableTo = Instant.parse(dto.availableTo)
            ),
            preferredTimeSlots = inferPreferredTimeSlots(dto),
            maxExamsPerDay = calculateMaxExamsPerDay(dto),
            lastUpdated = Instant.parse(dto.lastUpdated)
        )
    }

    private fun mapRaspredelbaClassroomToExamRoom(dto: RaspredelbaClassroomDto): ExamRoom {
        return ExamRoom(
            roomId = dto.id,
            roomName = dto.name,
            capacity = dto.capacity,
            building = dto.building,
            roomType = inferRoomType(dto.name),
            hasSpecialEquipment = hasSpecialEquipment(dto.name),
            equipmentList = inferEquipmentList(dto.name),
            availabilityPeriod = RoomAvailabilityPeriod(
                availableFrom = Instant.parse(dto.availableFrom),
                availableTo = Instant.parse(dto.availableTo),
                maintenancePeriods = extractMaintenancePeriods(dto)
            ),
            lastUpdated = Instant.parse(dto.lastAuditDate)
        )
    }

    private fun inferPreferredTimeSlots(dto: RaspredelbaProfessorDto): List<String> {
        return listOf("09:00-11:00", "11:00-13:00")
    }

    private fun calculateMaxExamsPerDay(dto: RaspredelbaProfessorDto): Int {
        return when {
            dto.lectures + dto.auditory + dto.labaratory > 2.0f -> 2
            dto.mentoring -> 2
            else -> 3
        }
    }

    private fun inferRoomType(roomName: String): ExamRoomType {
        return when {
            roomName.contains("Lab", ignoreCase = true) -> ExamRoomType.LABORATORY
            roomName.contains("Computer", ignoreCase = true) -> ExamRoomType.COMPUTER_LAB
            roomName.contains("Auditorium", ignoreCase = true) -> ExamRoomType.AUDITORIUM
            roomName.contains("Amphitheater", ignoreCase = true) -> ExamRoomType.LECTURE_HALL
            else -> ExamRoomType.REGULAR_CLASSROOM
        }
    }

    private fun hasSpecialEquipment(roomName: String): Boolean {
        return roomName.contains("Lab", ignoreCase = true) ||
                roomName.contains("Computer", ignoreCase = true)
    }

    private fun inferEquipmentList(roomName: String): List<String> {
        return when {
            roomName.contains("Computer", ignoreCase = true) ->
                listOf("Computers", "Projector", "Internet Access")

            roomName.contains("Lab", ignoreCase = true) ->
                listOf("Laboratory Equipment", "Projector", "Whiteboard")

            else -> listOf("Projector", "Whiteboard")
        }
    }

    private fun extractMaintenancePeriods(dto: RaspredelbaClassroomDto): List<MaintenancePeriod> {
        if (dto.maintenanceStart != null && dto.maintenanceEnd != null) {
            return listOf(
                MaintenancePeriod(
                    startTime = Instant.parse(dto.maintenanceStart),
                    endTime = Instant.parse(dto.maintenanceEnd),
                    reason = "Scheduled Maintenance"
                )
            )
        }
        return emptyList()
    }

    private fun createMockEnrollmentData(courseId: String, semesterCode: String): CourseEnrollment {
        val baseEnrollment = when {
            courseId.contains("101") -> 120
            courseId.contains("201") -> 80
            courseId.contains("301") -> 50
            courseId.contains("401") -> 30
            else -> 60
        }

        return CourseEnrollment(
            courseId = courseId,
            semesterCode = semesterCode,
            totalEnrolledStudents = baseEnrollment,
            activeEnrollments = (baseEnrollment * 0.95).toInt(),
            estimatedExamAttendance = (baseEnrollment * 0.85).toInt(),
            enrollmentDeadline = Instant.now().plus(30, ChronoUnit.DAYS),
            lastSynchronized = Instant.now()
        )
    }

    private fun createFallbackProfessor(professorId: String): ExamProfessor {
        return ExamProfessor(
            professorId = professorId,
            firstName = "Professor",
            lastName = professorId,
            email = "$professorId@university.edu.mk",
            department = "Unknown",
            isActive = true,
            teachingSubjects = emptyList(),
            availabilityPeriod = ProfessorAvailabilityPeriod(
                availableFrom = Instant.now(),
                availableTo = Instant.now().plus(365, ChronoUnit.DAYS)
            )
        )
    }

    private fun createFallbackRoom(classroomId: String): ExamRoom {
        return ExamRoom(
            roomId = classroomId,
            roomName = "Room $classroomId",
            capacity = 50,
            building = "Unknown",
            roomType = ExamRoomType.REGULAR_CLASSROOM,
            availabilityPeriod = RoomAvailabilityPeriod(
                availableFrom = Instant.now(),
                availableTo = Instant.now().plus(365, ChronoUnit.DAYS)
            )
        )
    }
}