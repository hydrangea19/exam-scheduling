package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.CreateExamSessionScheduleCommand
import mk.ukim.finki.examscheduling.schedulingservice.domain.ExamSessionSchedule
import mk.ukim.finki.examscheduling.schedulingservice.domain.InitiateScheduleGenerationCommand
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ScheduleStatus
import mk.ukim.finki.examscheduling.schedulingservice.repository.ExamSessionScheduleRepository
import org.axonframework.commandhandling.gateway.CommandGateway
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.*


@Component
class PreferenceEventConsumer(
    private val examSessionScheduleRepository: ExamSessionScheduleRepository,
    private val eventPublisher: EventPublisher,
    private val commandGateway: CommandGateway
) {
    private val logger = LoggerFactory.getLogger(PreferenceEventConsumer::class.java)

    @KafkaListener(topics = ["preference-management-events"], groupId = "scheduling-service")
    fun handlePreferenceEvent(
        @Payload message: Map<String, Any>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        acknowledgment: Acknowledgment
    ) {
        try {
            logger.info("Received preference event: topic={}, message={}", topic, message)

            when {
                message.containsKey("examSessionPeriodId") && message.containsKey("openedBy") -> {
                    handlePreferenceWindowOpened(message)
                }

                message.containsKey("professorId") && message.containsKey("preferenceId") -> {
                    handlePreferenceSubmitted(message)
                }

                message.containsKey("closedBy") -> {
                    handlePreferenceWindowClosed(message)
                }

                else -> {
                    logger.info("Unknown preference event type: {}", message)
                }
            }

            acknowledgment.acknowledge()

        } catch (e: Exception) {
            logger.error("Error processing preference event: {}", e.message, e)
        }
    }

    private fun handlePreferenceWindowOpened(message: Map<String, Any>) {
        val examSessionPeriodId = message["examSessionPeriodId"] as String
        val academicYear = message["academicYear"] as String
        val examSession = message["examSession"] as String
        val openedBy = message["openedBy"] as String

        logger.info(
            "Processing preference window opened: sessionId={}, academicYear={}, examSession={}",
            examSessionPeriodId, academicYear, examSession
        )

        try {
            val existingSchedule = examSessionScheduleRepository.findByExamSessionPeriodId(examSessionPeriodId)

            if (existingSchedule == null) {
                val scheduleId = UUID.randomUUID()

                commandGateway.send<Void>(
                    CreateExamSessionScheduleCommand(
                        scheduleId = scheduleId,
                        examSessionPeriodId = examSessionPeriodId,
                        academicYear = academicYear,
                        examSession = examSession,
                        startDate = LocalDate.now().plusDays(30),
                        endDate = LocalDate.now().plusDays(45),
                        createdBy = openedBy
                    )
                ).whenComplete { _, exception ->
                    if (exception == null) {
                        logger.info("Successfully created new schedule via command: scheduleId={}", scheduleId)

                        val integrationEvent = mapOf(
                            "eventType" to "ScheduleCreatedFromPreferenceWindow",
                            "scheduleId" to scheduleId.toString(),
                            "examSessionPeriodId" to examSessionPeriodId,
                            "academicYear" to academicYear,
                            "examSession" to examSession,
                            "status" to "DRAFT",
                            "createdBy" to openedBy,
                            "timestamp" to java.time.Instant.now().toString()
                        )
                        eventPublisher.publishSchedulingEvent(integrationEvent, "schedule-created-$examSessionPeriodId")

                    } else {
                        logger.error("Failed to create schedule via command: {}", exception.message, exception)
                    }
                }
            } else {
                logger.info(
                    "Schedule already exists for session: {}, current status: {}",
                    examSessionPeriodId, existingSchedule.status
                )

                if (existingSchedule.status == ScheduleStatus.DRAFT) {
                    val integrationEvent = mapOf(
                        "eventType" to "PreferenceWindowOpenedForExistingSchedule",
                        "scheduleId" to existingSchedule.id.toString(),
                        "examSessionPeriodId" to examSessionPeriodId,
                        "timestamp" to java.time.Instant.now().toString()
                    )
                    eventPublisher.publishSchedulingEvent(integrationEvent, "window-opened-$examSessionPeriodId")
                }
            }

        } catch (e: Exception) {
            logger.error("Failed to handle preference window opened event", e)
            throw e
        }
    }

    private fun handlePreferenceSubmitted(message: Map<String, Any>) {
        val professorId = message["professorId"] as String
        val examSessionPeriodId = message["examSessionPeriodId"] as String
        val preferenceId = message["preferenceId"] as String
        val courseIds = message["courseIds"] as? List<String> ?: emptyList()

        logger.info(
            "Processing preference submitted: professorId={}, sessionId={}, preferenceId={}, courses={}",
            professorId, examSessionPeriodId, preferenceId, courseIds
        )

        try {
            val schedule = examSessionScheduleRepository.findByExamSessionPeriodId(examSessionPeriodId)

            if (schedule != null) {
                logger.info(
                    "Found schedule for preference submission: scheduleId={}, currentStatus={}",
                    schedule.id, schedule.status
                )

                if (schedule.status == ScheduleStatus.DRAFT) {
                    val event = mapOf(
                        "eventType" to "PreferenceReceivedForScheduling",
                        "scheduleId" to schedule.id.toString(),
                        "professorId" to professorId,
                        "preferenceId" to preferenceId,
                        "courseIds" to courseIds,
                        "examSessionPeriodId" to examSessionPeriodId,
                        "processedBy" to "scheduling-service",
                        "timestamp" to java.time.Instant.now().toString()
                    )

                    eventPublisher.publishSchedulingEvent(event, "preference-received-$professorId")
                }

            } else {
                logger.warn(
                    "No schedule found for preference submission: sessionId={}, professorId={}",
                    examSessionPeriodId, professorId
                )

                logger.info("Creating schedule automatically for preference submission")
                handlePreferenceWindowOpened(
                    mapOf(
                        "examSessionPeriodId" to examSessionPeriodId,
                        "academicYear" to extractAcademicYear(examSessionPeriodId),
                        "examSession" to extractExamSession(examSessionPeriodId),
                        "openedBy" to "system-auto-creation"
                    )
                )
            }

        } catch (e: Exception) {
            logger.error("Failed to handle preference submitted event", e)
            throw e
        }
    }

    private fun handlePreferenceWindowClosed(message: Map<String, Any>) {
        val examSessionPeriodId = message["examSessionPeriodId"] as String
        val totalSubmissions = message["totalSubmissions"] as? Int ?: 0
        val closedBy = message["closedBy"] as String

        logger.info(
            "Processing preference window closed: sessionId={}, totalSubmissions={}",
            examSessionPeriodId, totalSubmissions
        )

        try {
            val schedule = examSessionScheduleRepository.findByExamSessionPeriodId(examSessionPeriodId)

            if (schedule != null) {
                logger.info("Preference window closed for schedule: scheduleId={}, ready for generation", schedule.id)

                val event = mapOf(
                    "eventType" to "ScheduleReadyForGeneration",
                    "scheduleId" to schedule.id.toString(),
                    "examSessionPeriodId" to examSessionPeriodId,
                    "totalPreferences" to totalSubmissions,
                    "status" to "READY_FOR_GENERATION",
                    "processedBy" to "scheduling-service",
                    "closedBy" to closedBy,
                    "timestamp" to java.time.Instant.now().toString()
                )

                eventPublisher.publishSchedulingEvent(event, "ready-for-generation-$examSessionPeriodId")

                if (shouldAutoTriggerGeneration(totalSubmissions)) {
                    logger.info("Auto-triggering schedule generation for session: {}", examSessionPeriodId)

                    commandGateway.send<Void>(
                        InitiateScheduleGenerationCommand(
                            scheduleId = schedule.id,
                            examSessionPeriodId = examSessionPeriodId,
                            academicYear = schedule.academicYear,
                            examSession = schedule.examSession,
                            initiatedBy = "auto-generation-system"
                        )
                    ).whenComplete { _, exception ->
                        if (exception == null) {
                            logger.info("Successfully auto-triggered generation for: {}", examSessionPeriodId)
                        } else {
                            logger.error("Failed to auto-trigger generation: {}", exception.message, exception)
                        }
                    }
                }

            } else {
                logger.error("No schedule found for closed preference window: {}", examSessionPeriodId)
            }

        } catch (e: Exception) {
            logger.error("Failed to handle preference window closed event", e)
            throw e
        }
    }

    private fun extractAcademicYear(examSessionPeriodId: String): String {
        return examSessionPeriodId.split("_")
            .find { it.matches("\\d{4}".toRegex()) }
            ?: "2025"
    }

    private fun extractExamSession(examSessionPeriodId: String): String {
        val parts = examSessionPeriodId.split("_")
        val session = parts.firstOrNull() ?: "UNKNOWN"
        val type = parts.lastOrNull() ?: "MIDTERM"
        return "${session}_${type}"
    }

    private fun shouldAutoTriggerGeneration(totalSubmissions: Int): Boolean {
        return totalSubmissions >= 5
    }
}