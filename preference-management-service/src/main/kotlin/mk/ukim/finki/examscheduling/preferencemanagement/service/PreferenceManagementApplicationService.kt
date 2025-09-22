package mk.ukim.finki.examscheduling.preferencemanagement.service

import com.fasterxml.jackson.databind.ObjectMapper
import mk.ukim.finki.examscheduling.preferencemanagement.domain.*
import mk.ukim.finki.examscheduling.preferencemanagement.domain.enums.PreferenceLevel
import mk.ukim.finki.examscheduling.preferencemanagement.query.*
import mk.ukim.finki.examscheduling.preferencemanagement.query.repository.PreferenceSubmissionSummaryRepository
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalTime
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
@Transactional
class PreferenceManagementApplicationService(
    private val commandGateway: CommandGateway,
    private val queryGateway: QueryGateway
) {
    private val logger = LoggerFactory.getLogger(PreferenceManagementApplicationService::class.java)

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var submissionRepository: PreferenceSubmissionSummaryRepository

    fun createExamSessionPeriod(request: CreateExamSessionPeriodRequest): CompletableFuture<String> {
        logger.info("Creating exam session period: {} {}", request.academicYear, request.examSession)

        val examSessionPeriodId = ExamSessionPeriodId.from(request.academicYear, request.examSession)

        val command = CreateExamSessionPeriodCommand(
            examSessionPeriodId = examSessionPeriodId,
            academicYear = request.academicYear,
            examSession = request.examSession,
            createdBy = request.createdBy,
            plannedStartDate = request.plannedStartDate,
            plannedEndDate = request.plannedEndDate,
            description = request.description
        )

        return commandGateway.send<Any>(command)
            .thenApply { examSessionPeriodId.value }
    }

    fun openPreferenceSubmissionWindow(request: OpenSubmissionWindowRequest): CompletableFuture<Void> {
        logger.info("Opening preference submission window for: {}", request.examSessionPeriodId)

        val command = OpenPreferenceSubmissionWindowCommand(
            examSessionPeriodId = ExamSessionPeriodId(request.examSessionPeriodId),
            academicYear = request.academicYear,
            examSession = request.examSession,
            openedBy = request.openedBy,
            submissionDeadline = request.submissionDeadline,
            description = request.description
        )

        return commandGateway.send(command)
    }

    fun closePreferenceSubmissionWindow(request: CloseSubmissionWindowRequest): CompletableFuture<Void> {
        logger.info("Closing preference submission window for: {}", request.examSessionPeriodId)

        val command = ClosePreferenceSubmissionWindowCommand(
            examSessionPeriodId = ExamSessionPeriodId(request.examSessionPeriodId),
            closedBy = request.closedBy,
            reason = request.reason,
            totalSubmissions = request.totalSubmissions
        )

        return commandGateway.send(command)
    }

    fun submitProfessorPreferences(request: SubmitPreferencesRequest): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            val submissionId = UUID.randomUUID().toString()

            // Extract time slots from request
            val timePrefs = request.preferences.firstOrNull()?.timePreferences ?: emptyList()
            val preferredSlots = timePrefs.filter { it.preferenceLevel != "UNAVAILABLE" }
            val unavailableSlots = timePrefs.filter { it.preferenceLevel == "UNAVAILABLE" }

            // Create summary directly
            val summary = PreferenceSubmissionSummary(
                submissionId = submissionId,
                professorId = request.professorId.toString(),
                examSessionPeriodId = request.examSessionPeriodId,
                academicYear = extractYear(request.examSessionPeriodId),
                examSession = extractSession(request.examSessionPeriodId),
                status = "SUBMITTED",
                submissionVersion = 1,
                totalTimePreferences = preferredSlots.size,
                totalRoomPreferences = 0,
                coursesCount = 1,
                submittedAt = Instant.now(),
                lastUpdatedAt = Instant.now(),
                hasSpecialRequirements = !request.preferences.firstOrNull()?.specialRequirements.isNullOrBlank(),

                preferredTimeSlotsJson = objectMapper.writeValueAsString(convertToFrontendFormat(preferredSlots)),
                unavailableTimeSlotsJson = objectMapper.writeValueAsString(convertToFrontendFormat(unavailableSlots)),
                additionalNotes = request.preferences.firstOrNull()?.specialRequirements
            )

            submissionRepository.save(summary)
            submissionId
        }
    }

    private fun convertToFrontendFormat(slots: List<TimeSlotPreferenceRequest>) =
        slots.map { slot ->
            mapOf(
                "dayOfWeek" to convertNumberToDay(slot.dayOfWeek),
                "startTime" to slot.startTime,
                "endTime" to slot.endTime,
                "priority" to if(slot.preferenceLevel == "PREFERRED") 1 else 3,
                "reason" to slot.reason
            )
        }

    private fun extractYear(sessionId: String): String {
        return sessionId.split("_").firstOrNull() ?: "2025"
    }

    private fun extractSession(sessionId: String): String {
        return sessionId.split("_").drop(1).joinToString("_")
    }

    private fun convertNumberToDay(dayNum: Int): String {
        return when(dayNum) {
            1 -> "MONDAY"
            2 -> "TUESDAY"
            3 -> "WEDNESDAY"
            4 -> "THURSDAY"
            5 -> "FRIDAY"
            6 -> "SATURDAY"
            7 -> "SUNDAY"
            else -> "MONDAY"
        }
    }

    fun updateProfessorPreferences(request: UpdatePreferencesRequest): CompletableFuture<Void> {
        logger.info("Updating preferences for submission: {}", request.submissionId)

        val updatedPreferences = mapToPreferenceDetails(request.updatedPreferences)

        val command = UpdateProfessorPreferenceCommand(
            submissionId = SubmissionId(request.submissionId),
            professorId = request.professorId,
            examSessionPeriodId = ExamSessionPeriodId(request.examSessionPeriodId),
            updatedPreferences = updatedPreferences,
            updateReason = request.updateReason,
            expectedVersion = request.expectedVersion
        )

        return commandGateway.send(command)
    }

    fun withdrawPreferences(request: WithdrawPreferencesRequest): CompletableFuture<Void> {
        logger.info("Withdrawing preferences for submission: {}", request.submissionId)

        val command = WithdrawPreferenceSubmissionCommand(
            submissionId = SubmissionId(request.submissionId),
            professorId = request.professorId,
            examSessionPeriodId = ExamSessionPeriodId(request.examSessionPeriodId),
            withdrawnBy = request.withdrawnBy,
            withdrawalReason = request.withdrawalReason
        )

        return commandGateway.send(command)
    }

    fun getPreferencesByProfessor(
        professorId: UUID,
        sessionId: String?
    ): CompletableFuture<List<PreferenceSubmissionSummary>> {
        logger.debug("Fetching preferences for professor: {}", professorId)

        val query = FindPreferencesByProfessorQuery(
            professorId = ProfessorId(professorId),
            examSessionPeriodId = sessionId?.let { ExamSessionPeriodId(it) }
        )

        return queryGateway.query(
            query, ResponseTypes.multipleInstancesOf(PreferenceSubmissionSummary::class.java)
        )
    }

    fun getPreferencesBySession(sessionId: String): CompletableFuture<List<PreferenceSubmissionSummary>> {
        logger.debug("Fetching preferences for session: {}", sessionId)

        val query = FindPreferencesBySessionQuery(
            examSessionPeriodId = sessionId
        )

        return queryGateway.query(
            query, ResponseTypes.multipleInstancesOf(PreferenceSubmissionSummary::class.java)
        )
    }

    fun getPreferenceStatistics(sessionId: String): CompletableFuture<List<PreferenceStatisticsView>> {
        logger.debug("Fetching preference statistics for session: {}", sessionId)

        val query = GetPreferenceStatisticsQuery(
            examSessionPeriodId = ExamSessionPeriodId(sessionId)
        )

        return queryGateway.query(
            query, ResponseTypes.multipleInstancesOf(PreferenceStatisticsView::class.java)
        )
    }

    fun getAllExamSessionPeriods(): CompletableFuture<List<ExamSessionPeriodView>> {
        logger.debug("Fetching all exam session periods")

        val query = GetExamSessionPeriodsQuery()

        return queryGateway.query(
            query, ResponseTypes.multipleInstancesOf(ExamSessionPeriodView::class.java)
        )
    }

    fun getConflictingPreferences(sessionId: String): CompletableFuture<List<PreferenceStatisticsView>> {
        logger.debug("Fetching conflicting preferences for session: {}", sessionId)

        val query = FindConflictingPreferencesQuery(
            examSessionPeriodId = ExamSessionPeriodId(sessionId)
        )

        return queryGateway.query(
            query, ResponseTypes.multipleInstancesOf(PreferenceStatisticsView::class.java)
        )
    }

    private fun mapToPreferenceDetails(requestPreferences: List<PreferenceDetailsRequest>): List<PreferenceDetails> {
        return requestPreferences.map { request ->
            PreferenceDetails(
                courseId = CourseId(request.courseId),
                timePreferences = request.timePreferences.map { timeReq ->
                    TimeSlotPreference(
                        timeSlot = TimeSlot(
                            dayOfWeek = timeReq.dayOfWeek,
                            startTime = LocalTime.parse(timeReq.startTime),
                            endTime = LocalTime.parse(timeReq.endTime)
                        ),
                        preferenceLevel = PreferenceLevel.valueOf(timeReq.preferenceLevel),
                        reason = timeReq.reason
                    )
                },
                roomPreferences = request.roomPreferences.map { roomReq ->
                    RoomPreference(
                        roomId = roomReq.roomId,
                        preferenceLevel = PreferenceLevel.valueOf(roomReq.preferenceLevel),
                        reason = roomReq.reason
                    )
                },
                durationPreference = request.durationPreference?.let { durReq ->
                    DurationPreference(
                        preferredDurationMinutes = durReq.preferredDurationMinutes,
                        minimumDurationMinutes = durReq.minimumDurationMinutes,
                        maximumDurationMinutes = durReq.maximumDurationMinutes
                    )
                },
                specialRequirements = request.specialRequirements
            )
        }
    }
}