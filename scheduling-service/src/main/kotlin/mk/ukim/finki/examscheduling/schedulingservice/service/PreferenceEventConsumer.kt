package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.ExamSessionSchedule
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ScheduleStatus
import mk.ukim.finki.examscheduling.schedulingservice.repository.ExamSessionScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.time.LocalDate


@Component
class PreferenceEventConsumer(
    private val examSessionScheduleRepository: ExamSessionScheduleRepository,
    private val eventPublisher: EventPublisher
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

            val eventType = message["serviceName"] as? String ?: "unknown"
            val eventClass = message.javaClass.simpleName

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

        logger.info(
            "Processing preference window opened: sessionId={}, academicYear={}, examSession={}",
            examSessionPeriodId, academicYear, examSession
        )

        val existingSchedule = examSessionScheduleRepository.findByExamSessionPeriodId(examSessionPeriodId)

        if (existingSchedule == null) {
            val newSchedule = ExamSessionSchedule(
                examSessionPeriodId = examSessionPeriodId,
                academicYear = academicYear,
                examSession = examSession,
                startDate = LocalDate.now().plusDays(30),
                endDate = LocalDate.now().plusDays(45),
                status = ScheduleStatus.DRAFT
            )

            val savedSchedule = examSessionScheduleRepository.save(newSchedule)

            logger.info("Created new draft schedule: id={}, sessionId={}", savedSchedule.id, examSessionPeriodId)

            val event = mapOf(
                "eventType" to "ScheduleCreated",
                "scheduleId" to savedSchedule.id.toString(),
                "examSessionPeriodId" to examSessionPeriodId,
                "status" to "DRAFT",
                "createdBy" to "scheduling-service",
                "timestamp" to java.time.Instant.now().toString()
            )

            eventPublisher.publishSchedulingEvent(event, "schedule-created-$examSessionPeriodId")
        } else {
            logger.info(
                "Schedule already exists for session: {}, current status: {}",
                examSessionPeriodId, existingSchedule.status
            )
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

        val schedule = examSessionScheduleRepository.findByExamSessionPeriodId(examSessionPeriodId)

        if (schedule != null) {
            logger.info(
                "Found schedule for preference submission: scheduleId={}, currentStatus={}",
                schedule.id, schedule.status
            )

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
        } else {
            logger.warn(
                "No schedule found for preference submission: sessionId={}, professorId={}",
                examSessionPeriodId, professorId
            )
        }
    }

    private fun handlePreferenceWindowClosed(message: Map<String, Any>) {
        val examSessionPeriodId = message["examSessionPeriodId"] as String
        val totalSubmissions = message["totalSubmissions"] as? Int ?: 0

        logger.info(
            "Processing preference window closed: sessionId={}, totalSubmissions={}",
            examSessionPeriodId, totalSubmissions
        )

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
                "timestamp" to java.time.Instant.now().toString()
            )

            eventPublisher.publishSchedulingEvent(event, "ready-for-generation-$examSessionPeriodId")
        }
    }
}