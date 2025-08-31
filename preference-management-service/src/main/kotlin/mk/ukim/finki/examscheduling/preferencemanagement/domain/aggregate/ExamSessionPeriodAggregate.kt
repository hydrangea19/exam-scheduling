package mk.ukim.finki.examscheduling.preferencemanagement.domain.aggregate

import mk.ukim.finki.examscheduling.preferencemanagement.domain.*
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import java.time.Instant

@Aggregate
class ExamSessionPeriodAggregate {

    @AggregateIdentifier
    private lateinit var examSessionPeriodId: ExamSessionPeriodId

    private lateinit var academicYear: String
    private lateinit var examSession: String
    private lateinit var createdBy: String
    private lateinit var plannedStartDate: Instant
    private lateinit var plannedEndDate: Instant
    private var description: String? = null
    private lateinit var createdAt: Instant

    private var isWindowOpen: Boolean = false
    private var submissionDeadline: Instant? = null
    private var windowOpenedAt: Instant? = null
    private var windowClosedAt: Instant? = null
    private var windowOpenedBy: String? = null
    private var windowClosedBy: String? = null
    private var totalSubmissions: Int = 0

    private val logger = LoggerFactory.getLogger(ExamSessionPeriodAggregate::class.java)

    constructor()


    @CommandHandler
    constructor(command: CreateExamSessionPeriodCommand) {
        logger.info("Creating exam session period: {}", command.examSessionPeriodId)

        validateCreateExamSessionPeriod(command)

        AggregateLifecycle.apply(
            ExamSessionPeriodCreatedEvent(
                examSessionPeriodId = command.examSessionPeriodId,
                academicYear = command.academicYear,
                examSession = command.examSession,
                createdBy = command.createdBy,
                plannedStartDate = command.plannedStartDate,
                plannedEndDate = command.plannedEndDate,
                description = command.description,
                createdAt = command.createdAt
            )
        )
    }

    @CommandHandler
    fun handle(command: OpenPreferenceSubmissionWindowCommand) {
        logger.info("Opening preference submission window for: {}", command.examSessionPeriodId)

        validateOpenSubmissionWindow(command)

        AggregateLifecycle.apply(
            PreferenceSubmissionWindowOpenedEvent(
                examSessionPeriodId = command.examSessionPeriodId,
                academicYear = command.academicYear,
                examSession = command.examSession,
                openedBy = command.openedBy,
                submissionDeadline = command.submissionDeadline,
                description = command.description,
                openedAt = command.openedAt
            )
        )
    }

    @CommandHandler
    fun handle(command: ClosePreferenceSubmissionWindowCommand) {
        logger.info("Closing preference submission window for: {}", command.examSessionPeriodId)

        require(isWindowOpen) { "Submission window is not currently open" }

        AggregateLifecycle.apply(
            PreferenceSubmissionWindowClosedEvent(
                examSessionPeriodId = command.examSessionPeriodId,
                closedBy = command.closedBy,
                reason = command.reason,
                closedAt = command.closedAt,
                totalSubmissions = command.totalSubmissions
            )
        )
    }

    @EventSourcingHandler
    fun on(event: ExamSessionPeriodCreatedEvent) {
        this.examSessionPeriodId = event.examSessionPeriodId
        this.academicYear = event.academicYear
        this.examSession = event.examSession
        this.createdBy = event.createdBy
        this.plannedStartDate = event.plannedStartDate
        this.plannedEndDate = event.plannedEndDate
        this.description = event.description
        this.createdAt = event.createdAt

        logger.debug("Exam session period created: {}", event.examSessionPeriodId)
    }

    @EventSourcingHandler
    fun on(event: PreferenceSubmissionWindowOpenedEvent) {
        this.isWindowOpen = true
        this.submissionDeadline = event.submissionDeadline
        this.windowOpenedAt = event.openedAt
        this.windowOpenedBy = event.openedBy
        this.windowClosedAt = null

        logger.debug("Preference submission window opened for: {}", event.examSessionPeriodId)
    }

    @EventSourcingHandler
    fun on(event: PreferenceSubmissionWindowClosedEvent) {
        this.isWindowOpen = false
        this.windowClosedAt = event.closedAt
        this.windowClosedBy = event.closedBy
        this.totalSubmissions = event.totalSubmissions

        logger.debug("Preference submission window closed for: {}", event.examSessionPeriodId)
    }


    private fun validateCreateExamSessionPeriod(command: CreateExamSessionPeriodCommand) {
        require(command.plannedStartDate.isBefore(command.plannedEndDate)) {
            "Start date must be before end date"
        }
        require(command.academicYear.isNotBlank()) {
            "Academic year cannot be blank"
        }
        require(command.examSession.isNotBlank()) {
            "Exam session cannot be blank"
        }
    }

    private fun validateOpenSubmissionWindow(command: OpenPreferenceSubmissionWindowCommand) {
        require(command.submissionDeadline.isAfter(Instant.now())) {
            "Submission deadline must be in the future"
        }
        require(!isWindowOpen) {
            "Submission window is already open"
        }
        require(command.examSessionPeriodId == this.examSessionPeriodId) {
            "Command targets different exam session period"
        }
    }
}