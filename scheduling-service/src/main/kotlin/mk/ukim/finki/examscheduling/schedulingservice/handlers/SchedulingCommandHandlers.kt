package mk.ukim.finki.examscheduling.schedulingservice.handlers

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ExamUpdateType
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.MandatoryStatus
import mk.ukim.finki.examscheduling.schedulingservice.repository.ExamSessionScheduleRepository
import mk.ukim.finki.examscheduling.schedulingservice.service.ExternalIntegrationClient
import mk.ukim.finki.examscheduling.schedulingservice.service.PreferenceManagementClient
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.gateway.CommandGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class SchedulingCommandHandlers(
    private val examSessionScheduleRepository: ExamSessionScheduleRepository,
    private val commandGateway: CommandGateway,
    private val preferenceManagementClient: PreferenceManagementClient,
    private val externalIntegrationClient: ExternalIntegrationClient
) {

    private val logger = LoggerFactory.getLogger(SchedulingCommandHandlers::class.java)

    @CommandHandler
    fun handle(command: InitiateScheduleGenerationCommand) {
        logger.info("Initiating schedule generation for session: {}", command.examSessionPeriodId)

        try {
            commandGateway.sendAndWait<Void>(
                TriggerDraftScheduleGenerationCommand(
                    scheduleId = command.scheduleId,
                    triggeredBy = command.initiatedBy,
                    externalDataRequired = true
                )
            )

            val enrollmentDataFuture = externalIntegrationClient.getEnrollmentData()
            val courseDataFuture = externalIntegrationClient.getAllCourses()
            val preferenceDataFuture = preferenceManagementClient.getPreferencesBySession(
                command.academicYear,
                command.examSession
            )

            val enrollmentData = enrollmentDataFuture.get()
            val courseData = courseDataFuture.get()
            val preferenceData = preferenceDataFuture.get()

            val courseEnrollmentData = transformEnrollmentData(enrollmentData)
            val courseAccreditationData = transformCourseData(courseData)
            val professorPreferences = transformPreferenceData(preferenceData)
            val availableRooms = getAvailableRooms()

            commandGateway.sendAndWait<Void>(
                GenerateDraftScheduleCommand(
                    scheduleId = command.scheduleId,
                    courseEnrollmentData = courseEnrollmentData,
                    courseAccreditationData = courseAccreditationData,
                    professorPreferences = professorPreferences,
                    availableRooms = availableRooms,
                    generatedBy = command.initiatedBy
                )
            )

            logger.info("Schedule generation completed for session: {}", command.examSessionPeriodId)

        } catch (e: Exception) {
            logger.error("Failed to generate schedule for session: {}", command.examSessionPeriodId, e)

            commandGateway.send<HandleScheduleGenerationFailureCommand>(
                HandleScheduleGenerationFailureCommand(
                    scheduleId = command.scheduleId,
                    examSessionPeriodId = command.examSessionPeriodId,
                    failureReason = e.message ?: "Unknown error",
                    failedAt = Instant.now()
                )
            )

            throw e
        }
    }

    @CommandHandler
    fun handle(command: BatchUpdateScheduledExamsCommand) {
        logger.info(
            "Processing batch update for {} exams in schedule: {}",
            command.examUpdates.size, command.scheduleId
        )

        command.examUpdates.forEach { update ->
            try {
                when (update.updateType) {
                    ExamUpdateType.TIME_CHANGE -> {
                        commandGateway.sendAndWait<Void>(
                            UpdateScheduledExamTimeCommand(
                                scheduleId = command.scheduleId,
                                scheduledExamId = update.scheduledExamId,
                                newExamDate = update.newExamDate!!,
                                newStartTime = update.newStartTime!!,
                                newEndTime = update.newEndTime!!,
                                reason = update.reason ?: "Batch update",
                                updatedBy = command.updatedBy
                            )
                        )
                    }

                    ExamUpdateType.SPACE_CHANGE -> {
                        commandGateway.sendAndWait<Void>(
                            UpdateScheduledExamSpaceCommand(
                                scheduleId = command.scheduleId,
                                scheduledExamId = update.scheduledExamId,
                                newRoomId = update.newRoomId,
                                newRoomName = update.newRoomName,
                                newRoomCapacity = update.newRoomCapacity,
                                reason = update.reason ?: "Batch update",
                                updatedBy = command.updatedBy
                            )
                        )
                    }

                    ExamUpdateType.REMOVAL -> {
                        commandGateway.sendAndWait<Void>(
                            RemoveScheduledExamCommand(
                                scheduleId = command.scheduleId,
                                scheduledExamId = update.scheduledExamId,
                                reason = update.reason ?: "Batch removal",
                                removedBy = command.updatedBy
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to update exam {} in batch operation", update.scheduledExamId, e)
            }
        }

        logger.info("Completed batch update for schedule: {}", command.scheduleId)
    }

    private fun transformEnrollmentData(enrollmentData: Map<String, Any>): Map<String, CourseEnrollmentInfo> {
        val courses = enrollmentData["courses"] as? List<Map<String, Any>> ?: emptyList()

        return courses.associate { course ->
            val courseId = course["courseId"] as String
            val studentCount = course["enrolledStudents"] as? Int ?: 0

            courseId to CourseEnrollmentInfo(
                courseId = courseId,
                studentCount = studentCount,
                enrollmentDetails = course.filterKeys { it != "courseId" && it != "enrolledStudents" }
            )
        }
    }

    private fun transformCourseData(courseData: Map<String, Any>): Map<String, CourseAccreditationInfo> {
        val courses = courseData["courses"] as? List<Map<String, Any>> ?: emptyList()

        return courses.associate { course ->
            val courseId = course["courseId"] as String
            val courseName = course["courseName"] as? String ?: "Unknown Course"
            val mandatoryStatus = if (course["mandatory"] as? Boolean == true) {
                MandatoryStatus.MANDATORY
            } else {
                MandatoryStatus.ELECTIVE
            }
            val credits = course["credits"] as? Int ?: 6
            val professorIds = (course["professors"] as? List<String>)?.toSet() ?: emptySet()

            courseId to CourseAccreditationInfo(
                courseId = courseId,
                courseName = courseName,
                mandatoryStatus = mandatoryStatus,
                credits = credits,
                professorIds = professorIds,
                accreditationDetails = course.filterKeys {
                    it !in setOf("courseId", "courseName", "mandatory", "credits", "professors")
                }
            )
        }
    }

    private fun transformPreferenceData(preferenceData: Map<String, Any>): List<ProfessorPreferenceInfo> {
        val preferences = preferenceData["preferences"] as? List<Map<String, Any>> ?: emptyList()

        return preferences.map { pref ->
            ProfessorPreferenceInfo(
                preferenceId = pref["preferenceId"] as? String ?: UUID.randomUUID().toString(),
                professorId = pref["professorId"] as String,
                courseId = pref["courseId"] as String,
                preferredDates = transformDates(pref["preferredDates"] as? List<String>),
                preferredTimeSlots = transformTimeSlots(pref["preferredTimeSlots"] as? List<Map<String, Any>>),
                preferredRooms = pref["preferredRooms"] as? List<String> ?: emptyList(),
                unavailableDates = transformDates(pref["unavailableDates"] as? List<String>),
                unavailableTimeSlots = transformTimeSlots(pref["unavailableTimeSlots"] as? List<Map<String, Any>>),
                specialRequirements = pref["specialRequirements"] as? String,
                priority = pref["priority"] as? Int ?: 3
            )
        }
    }

    private fun transformDates(dateStrings: List<String>?): List<java.time.LocalDate> {
        return dateStrings?.mapNotNull { dateStr ->
            try {
                java.time.LocalDate.parse(dateStr)
            } catch (e: Exception) {
                logger.warn("Failed to parse date: {}", dateStr)
                null
            }
        } ?: emptyList()
    }

    private fun transformTimeSlots(timeSlots: List<Map<String, Any>>?): List<TimeSlotPreference> {
        return timeSlots?.mapNotNull { slot ->
            try {
                TimeSlotPreference(
                    startTime = java.time.LocalTime.parse(slot["startTime"] as String),
                    endTime = java.time.LocalTime.parse(slot["endTime"] as String),
                    dayOfWeek = slot["dayOfWeek"] as? Int
                )
            } catch (e: Exception) {
                logger.warn("Failed to parse time slot: {}", slot)
                null
            }
        } ?: emptyList()
    }

    private fun getAvailableRooms(): List<RoomInfo> {
        return listOf(
            RoomInfo(
                roomId = "A1-001",
                roomName = "Lecture Hall A1-001",
                capacity = 100,
                equipment = setOf("projector", "whiteboard"),
                location = "Building A1, Ground Floor"
            ),
            RoomInfo(
                roomId = "A2-205",
                roomName = "Classroom A2-205",
                capacity = 50,
                equipment = setOf("projector"),
                location = "Building A2, Second Floor"
            )
        )
    }
}