package mk.ukim.finki.examscheduling.schedulingservice.handlers

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.aggregate.ExamSessionScheduleAggregate
import mk.ukim.finki.examscheduling.schedulingservice.repository.ExamSessionScheduleRepository
import mk.ukim.finki.examscheduling.schedulingservice.service.*
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.gateway.CommandGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.CompletableFuture

@Component
class EnhancedSchedulingCommandHandlers(
    private val examSessionScheduleRepository: ExamSessionScheduleRepository,
    private val commandGateway: CommandGateway,
    private val externalDataIntegrationService: ExternalDataIntegrationService,
    private val advancedSchedulingService: AdvancedSchedulingService,
    private val resourceManagementService: ResourceManagementService,
    private val performanceMonitor: SchedulingPerformanceMonitor,
    private val qualityScoringService: QualityScoringService
) {

    private val logger = LoggerFactory.getLogger(EnhancedSchedulingCommandHandlers::class.java)


    @CommandHandler
    fun handle(command: GenerateAdvancedScheduleCommand): CompletableFuture<ScheduleGenerationResult> {
        logger.info("Handling advanced schedule generation for: {}", command.examSessionPeriodId)

        return externalDataIntegrationService.fetchSchedulingData(
            command.examSessionPeriodId,
            command.academicYear,
            command.examSession
        ).thenCompose { integratedData ->

            if (integratedData.dataQuality.overallScore < 0.3) {
                logger.error("External data quality too low for scheduling: {}", integratedData.dataQuality.overallScore)
                return@thenCompose CompletableFuture.completedFuture(
                    ScheduleGenerationResult.failure(
                        command.scheduleId,
                        "External data quality insufficient for reliable schedule generation",
                        integratedData.dataQuality
                    )
                )
            }

            val resourceAllocations = resourceManagementService.calculateOptimalRoomAllocations(
                courses = integratedData.courses,
                availableRooms = integratedData.resources.map { it.roomInfo },
                examPeriod = ExamPeriod(command.startDate, command.endDate, command.examSessionPeriodId),
                preferences = integratedData.preferences
            )

            val courseEnrollmentData = integratedData.courses.mapValues { (_, course) ->
                CourseEnrollmentInfo(
                    courseId = course.courseId,
                    studentCount = course.studentCount
                )
            }

            val courseAccreditationData = integratedData.courses.mapValues { (_, course) ->
                CourseAccreditationInfo(
                    courseId = course.courseId,
                    courseName = course.courseName,
                    mandatoryStatus = course.mandatoryStatus,
                    credits = course.credits,
                    professorIds = course.professorIds,
                    prerequisites = course.prerequisites
                )
            }

            val professorPreferences = integratedData.preferences.map { it.preferenceInfo }
            val availableRooms = integratedData.resources.map { it.roomInfo }

            try {
                val schedulingResult = advancedSchedulingService.generateOptimalSchedule(
                    courseEnrollmentData = courseEnrollmentData,
                    courseAccreditationData = courseAccreditationData,
                    professorPreferences = professorPreferences,
                    availableRooms = availableRooms,
                    examPeriod = ExamPeriod(command.startDate, command.endDate, command.examSessionPeriodId),
                    institutionalConstraints = command.institutionalConstraints
                )

                commandGateway.send<Void>(
                    ApplyGeneratedScheduleCommand(
                        scheduleId = command.scheduleId,
                        schedulingResult = schedulingResult,
                        resourceAllocations = resourceAllocations,
                        dataQualityMetrics = integratedData.dataQuality,
                        generationMetadata = GenerationMetadata(
                            externalDataSources = integratedData.integrationMetadata.sourceCount,
                            dataFreshness = integratedData.dataQuality.freshnessScore,
                            algorithmUsed = null,
                            processingTime = schedulingResult.metrics.processingTimeMs
                        ),
                        generatedBy = command.generatedBy
                    )
                )

                performanceMonitor.recordSchedulingResult(
                    SchedulingProblem(
                        examPeriod = ExamPeriod(command.startDate, command.endDate, command.examSessionPeriodId),
                        courses = integratedData.courses.values.map { course ->
                            CourseSchedulingInfo(
                                courseId = course.courseId,
                                courseName = course.courseName,
                                studentCount = course.studentCount,
                                professorIds = course.professorIds,
                                mandatoryStatus = course.mandatoryStatus,
                                estimatedDuration = course.estimatedDuration
                            )
                        },
                        availableRooms = availableRooms,
                        professorPreferences = professorPreferences,
                        institutionalConstraints = command.institutionalConstraints ?: InstitutionalConstraints(
                            workingHours = WorkingHours(java.time.LocalTime.of(8, 0), java.time.LocalTime.of(18, 0))
                        ),
                        constraints = emptyList()
                    ),
                    SchedulingSolution(
                        scheduledExams = schedulingResult.scheduledExams,
                        constraintViolations = schedulingResult.violations,
                        qualityScore = schedulingResult.qualityScore,
                        isComplete = true,
                        processingTimeMs = schedulingResult.metrics.processingTimeMs,
                        algorithmUsed = null,
                        optimizationMetrics = OptimizationMetrics()
                    ),
                    schedulingResult.metrics.processingTimeMs
                )

                CompletableFuture.completedFuture(
                    ScheduleGenerationResult.success(
                        command.scheduleId,
                        schedulingResult,
                        resourceAllocations,
                        integratedData.dataQuality
                    )
                )

            } catch (e: Exception) {
                logger.error("Advanced scheduling failed for session: {}", command.examSessionPeriodId, e)
                CompletableFuture.completedFuture(
                    ScheduleGenerationResult.failure(
                        command.scheduleId,
                        "Advanced scheduling algorithm failed: ${e.message}",
                        integratedData.dataQuality
                    )
                )
            }
        }.exceptionally { throwable ->
            logger.error("External data integration failed for session: {}", command.examSessionPeriodId, throwable)
            ScheduleGenerationResult.failure(
                command.scheduleId,
                "External data integration failed: ${throwable.message}",
                null
            )
        }
    }


  /*  @CommandHandler
    fun handle(command: ReallocateResourcesCommand): CompletableFuture<ResourceReallocationResult> {
        logger.info("Handling resource reallocation for schedule: {}", command.scheduleId)

        return CompletableFuture.supplyAsync {
            try {
                val aggregate = examSessionScheduleRepository.load(command.scheduleId.toString())
                val currentSchedule = extractCurrentSchedule(aggregate)

                val capacityAnalysis = resourceManagementService.performCapacityAnalysis(
                    currentAllocations = command.currentAllocations,
                    proposedChanges = command.proposedChanges
                )

                if (capacityAnalysis.feasibilityScore < 0.6) {
                    return@supplyAsync ResourceReallocationResult.infeasible(
                        command.scheduleId,
                        capacityAnalysis.identifiedBottlenecks,
                        capacityAnalysis.recommendations
                    )
                }

                val optimizationResult = resourceManagementService.optimizeResourceUtilization(
                    currentSchedule = currentSchedule,
                    availableRooms = command.availableRooms
                )

                val updateCommands = createResourceUpdateCommands(
                    command.scheduleId,
                    optimizationResult,
                    command.requestedBy
                )

                updateCommands.forEach { updateCommand ->
                    commandGateway.sendAndWait<Void>(updateCommand)
                }

                ResourceReallocationResult.success(
                    command.scheduleId,
                    optimizationResult,
                    capacityAnalysis
                )

            } catch (e: Exception) {
                logger.error("Resource reallocation failed for schedule: {}", command.scheduleId, e)
                ResourceReallocationResult.failure(
                    command.scheduleId,
                    "Resource reallocation failed: ${e.message}"
                )
            }
        }
    }*/

    @CommandHandler
    fun handle(command: SynchronizeExternalDataCommand): CompletableFuture<DataSynchronizationResult> {
        logger.info("Synchronizing external data for session: {}", command.examSessionPeriodId)

        return externalDataIntegrationService.fetchSchedulingData(
            command.examSessionPeriodId,
            command.academicYear,
            command.examSession
        ).thenApply { integratedData ->

            try {
                val dataChanges = detectDataChanges(command.scheduleId, integratedData)

                if (dataChanges.hasSignificantChanges) {
                    val regenerationCommand = GenerateAdvancedScheduleCommand(
                        scheduleId = command.scheduleId,
                        examSessionPeriodId = command.examSessionPeriodId,
                        academicYear = command.academicYear,
                        examSession = command.examSession,
                        startDate = command.startDate,
                        endDate = command.endDate,
                        generatedBy = "auto-sync-${command.synchronizedBy}",
                        institutionalConstraints = null
                    )

                    commandGateway.send<GenerateAdvancedScheduleCommand>(regenerationCommand)

                    DataSynchronizationResult.withRegeneration(
                        command.scheduleId,
                        dataChanges,
                        integratedData.dataQuality
                    )
                } else {
                    commandGateway.send<UpdateDataQualityMetricsCommand>(
                        UpdateDataQualityMetricsCommand(
                            scheduleId = command.scheduleId,
                            dataQualityMetrics = integratedData.dataQuality,
                            lastSynchronized = java.time.Instant.now(),
                            synchronizedBy = command.synchronizedBy
                        )
                    )

                    DataSynchronizationResult.dataOnly(
                        command.scheduleId,
                        dataChanges,
                        integratedData.dataQuality
                    )
                }

            } catch (e: Exception) {
                logger.error("Data synchronization processing failed", e)
                DataSynchronizationResult.failure(
                    command.scheduleId,
                    "Data synchronization failed: ${e.message}"
                )
            }
        }.exceptionally { throwable ->
            logger.error("External data fetch failed during sync", throwable)
            DataSynchronizationResult.failure(
                command.scheduleId,
                "External data fetch failed: ${throwable.message}"
            )
        }
    }

    /*@CommandHandler
    fun handle(command: ValidateScheduleFeasibilityCommand): CompletableFuture<FeasibilityValidationResult> {
        logger.info("Validating schedule feasibility for: {}", command.scheduleId)

        return externalDataIntegrationService.fetchSchedulingData(
            command.examSessionPeriodId,
            command.academicYear,
            command.examSession
        ).thenApply { integratedData ->

            try {
                val aggregate = examSessionScheduleRepository.load(command.scheduleId.toString())
                val currentSchedule = extractCurrentSchedule(aggregate)

                val feasibilityResults = performFeasibilityAnalysis(
                    currentSchedule = currentSchedule,
                    integratedData = integratedData,
                    validationCriteria = command.validationCriteria
                )

                val recommendations = generateFeasibilityRecommendations(feasibilityResults)

                FeasibilityValidationResult(
                    scheduleId = command.scheduleId,
                    isFeasible = feasibilityResults.overallFeasibility,
                    feasibilityScore = feasibilityResults.feasibilityScore,
                    validationResults = feasibilityResults.detailedResults,
                    recommendations = recommendations,
                    dataQuality = integratedData.dataQuality,
                    validatedAt = java.time.Instant.now(),
                    validatedBy = command.validatedBy
                )

            } catch (e: Exception) {
                logger.error("Feasibility validation failed", e)
                FeasibilityValidationResult.failure(
                    command.scheduleId,
                    "Feasibility validation failed: ${e.message}"
                )
            }
        }
    }*/


    private fun extractCurrentSchedule(aggregate: ExamSessionScheduleAggregate): List<ScheduledExamInfo> {
        return emptyList()
    }

    private fun createResourceUpdateCommands(
        scheduleId: UUID,
        optimizationResult: ResourceOptimizationResult,
        requestedBy: String
    ): List<Any> {
        return optimizationResult.optimizedSchedule.map { examInfo ->
            UpdateScheduledExamSpaceCommand(
                scheduleId = scheduleId,
                scheduledExamId = examInfo.scheduledExamId,
                newRoomId = examInfo.roomId,
                newRoomName = examInfo.roomName,
                newRoomCapacity = examInfo.roomCapacity,
                reason = "Resource optimization",
                updatedBy = requestedBy
            )
        }
    }

    private fun detectDataChanges(scheduleId: UUID, integratedData: IntegratedSchedulingData): DataChangesSummary {
        return DataChangesSummary(
            hasSignificantChanges = false,
            courseChanges = 0,
            enrollmentChanges = 0,
            preferenceChanges = 0,
            resourceChanges = 0,
            changeDetails = emptyList()
        )
    }

    private fun performFeasibilityAnalysis(
        currentSchedule: List<ScheduledExamInfo>,
        integratedData: IntegratedSchedulingData,
        validationCriteria: FeasibilityValidationCriteria
    ): FeasibilityAnalysisResult {
        // Perform comprehensive feasibility analysis
        return FeasibilityAnalysisResult(
            overallFeasibility = true,
            feasibilityScore = 0.8,
            detailedResults = mapOf(
                "capacity" to true,
                "resources" to true,
                "preferences" to true,
                "constraints" to true
            )
        )
    }

    private fun generateFeasibilityRecommendations(results: FeasibilityAnalysisResult): List<String> {
        val recommendations = mutableListOf<String>()

        if (results.feasibilityScore < 0.7) {
            recommendations.add("Schedule feasibility is below optimal threshold - review constraints")
        }

        results.detailedResults.forEach { (aspect, feasible) ->
            if (!feasible) {
                recommendations.add("Address feasibility issues in: $aspect")
            }
        }

        return recommendations.ifEmpty { listOf("Schedule meets all feasibility requirements") }
    }
}