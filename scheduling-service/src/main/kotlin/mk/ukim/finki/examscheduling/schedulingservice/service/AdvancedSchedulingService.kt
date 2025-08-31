package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ConflictSeverity
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ViolationSeverity
import mk.ukim.finki.examscheduling.schedulingservice.domain.exceptions.SchedulingSolvingException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.*

@Service
@Transactional
class AdvancedSchedulingService(
    private val cspSolver: ExamSchedulingCSPSolver,
    private val constraintSystem: ExamSchedulingConstraintSystem,
    private val performanceMonitor: SchedulingPerformanceMonitor,
    private val qualityScoringService: QualityScoringService,
    private val conflictAnalysisService: ConflictAnalysisService,
    private val fallbackStrategyService: SchedulingFallbackStrategyService
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
        logger.info("Starting advanced schedule generation for {} courses with {} preferences",
            courseEnrollmentData.size, professorPreferences.size)

        try {
            val problem = prepareSchedulingProblem(
                courseEnrollmentData,
                courseAccreditationData,
                professorPreferences,
                availableRooms,
                examPeriod,
                institutionalConstraints
            )

            val strategy = selectOptimalStrategy(problem)
            performanceMonitor.startSchedulingSession(problem, strategy)

            var solution = attemptPrimarySolving(problem.copy(solvingStrategy = strategy))

            if (!solution.isComplete || solution.constraintViolations.any { it.severity == ViolationSeverity.CRITICAL }) {
                logger.warn("Primary solving approach failed or has critical violations. Applying fallback strategies.")
                solution = fallbackStrategyService.appleFallbackStrategies(problem, solution)
            }

            solution = optimizeSolution(solution, problem)

            val validatedSolution = validateAndAnalyzeSolution(solution, problem)

            val endTime = Instant.now()
            val totalProcessingTime = Duration.between(startTime, endTime).toMillis()

            performanceMonitor.recordSchedulingResult(
                problem, validatedSolution, totalProcessingTime
            )

            logger.info("Advanced schedule generation completed in {}ms with quality score: {}",
                totalProcessingTime, validatedSolution.qualityScore)

            return convertToSchedulingResult(validatedSolution, totalProcessingTime)

        } catch (e: Exception) {
            logger.error("Failed to generate schedule", e)
            performanceMonitor.recordSchedulingFailure(e)

            // Return emergency fallback
            return generateEmergencyFallback(
                courseEnrollmentData, courseAccreditationData, availableRooms, examPeriod, e
            )
        }
    }

    private fun prepareSchedulingProblem(
        courseEnrollmentData: Map<String, CourseEnrollmentInfo>,
        courseAccreditationData: Map<String, CourseAccreditationInfo>,
        professorPreferences: List<ProfessorPreferenceInfo>,
        availableRooms: List<RoomInfo>,
        examPeriod: ExamPeriod,
        institutionalConstraints: InstitutionalConstraints?
    ): SchedulingProblem {
        logger.debug("Preparing scheduling problem for {} courses", courseEnrollmentData.size)

        val courses = courseEnrollmentData.mapNotNull { (courseId, enrollment) ->
            val accreditation = courseAccreditationData[courseId]
            if (accreditation != null) {
                CourseSchedulingInfo(
                    courseId = courseId,
                    courseName = accreditation.courseName,
                    studentCount = enrollment.studentCount,
                    professorIds = accreditation.professorIds,
                    mandatoryStatus = accreditation.mandatoryStatus,
                    estimatedDuration = calculateEstimatedDuration(accreditation.credits),
                    requiredEquipment = extractRequiredEquipment(accreditation.accreditationDetails),
                    accessibilityRequired = extractAccessibilityRequirement(accreditation.accreditationDetails),
                    specialRequirements = accreditation.accreditationDetails["specialRequirements"] as? String
                )
            } else {
                logger.warn("No accreditation data found for course: {}", courseId)
                null
            }
        }

        val constraints = institutionalConstraints ?: InstitutionalConstraints(
            workingHours = WorkingHours(LocalTime.of(8, 0), LocalTime.of(18, 0)),
            minimumExamDuration = 120,
            minimumGapMinutes = 30,
            maxExamsPerDay = 6,
            maxExamsPerRoom = 8,
            allowWeekendExams = false
        )

        val schedulingConstraints = generateSchedulingConstraints(courses, constraints)

        return SchedulingProblem(
            examPeriod = examPeriod,
            courses = courses,
            availableRooms = availableRooms,
            professorPreferences = professorPreferences,
            institutionalConstraints = constraints,
            constraints = schedulingConstraints
        )
    }


    private fun selectOptimalStrategy(problem: SchedulingProblem): SolvingStrategy {
        val courseCount = problem.courses.size
        val constraintComplexity = calculateConstraintComplexity(problem)
        val preferenceCount = problem.professorPreferences.size

        return when {
            courseCount <= 20 && constraintComplexity < 0.3 ->
                SolvingStrategy.BACKTRACKING_WITH_FORWARD_CHECKING

            courseCount <= 50 && preferenceCount > courseCount * 0.7 ->
                SolvingStrategy.SIMULATED_ANNEALING

            courseCount > 50 || constraintComplexity > 0.7 ->
                SolvingStrategy.HYBRID_APPROACH

            else -> SolvingStrategy.GREEDY_WITH_BACKTRACKING
        }
    }


    private fun attemptPrimarySolving(problem: SchedulingProblem): SchedulingSolution {
        logger.debug("Attempting primary solving with strategy: {}", problem.solvingStrategy)

        val timeout = calculateTimeout(problem)
        val startTime = System.currentTimeMillis()

        try {
            val solution = with(timeout) {
                cspSolver.solve(problem)
            }

            val processingTime = System.currentTimeMillis() - startTime
            logger.debug("Primary solving completed in {}ms", processingTime)

            return solution

        } catch (e: Exception) {
            logger.error("Primary solving failed", e)
            throw SchedulingSolvingException("Primary solving approach failed", e)
        }
    }


    private fun optimizeSolution(solution: SchedulingSolution, problem: SchedulingProblem): SchedulingSolution {
        if (solution.constraintViolations.isEmpty() && solution.qualityScore > 0.9) {
            return solution
        }

        logger.debug("Applying solution optimization")

        var optimizedSolution = solution
        var iterationCount = 0
        val maxIterations = 50
        val targetQuality = 0.85

        while (iterationCount < maxIterations && optimizedSolution.qualityScore < targetQuality) {
            optimizedSolution = when (iterationCount % 4) {
                0 -> applyTimeSlotOptimization(optimizedSolution, problem)
                1 -> applyRoomAllocationOptimization(optimizedSolution, problem)
                2 -> applyConstraintViolationRepair(optimizedSolution, problem)
                else -> applyPreferenceSatisfactionImprovement(optimizedSolution, problem)
            }

            iterationCount++

            if (optimizedSolution.qualityScore <= solution.qualityScore) {
                break
            }
        }

        logger.debug("Solution optimization completed after {} iterations. Quality: {} -> {}",
            iterationCount, solution.qualityScore, optimizedSolution.qualityScore)

        return optimizedSolution
    }


    private fun validateAndAnalyzeSolution(
        solution: SchedulingSolution,
        problem: SchedulingProblem
    ): SchedulingSolution {
        logger.debug("Validating and analyzing final solution")

        val assignments = solution.scheduledExams.associate { exam ->
            exam.courseId to TimeSlot(
                date = exam.examDate,
                startTime = exam.startTime,
                endTime = exam.endTime,
                roomId = exam.roomId ?: "",
                roomName = exam.roomName ?: "",
                roomCapacity = exam.roomCapacity ?: 0,
                dayOfWeek = exam.examDate.dayOfWeek.value
            )
        }

        val validationResult = constraintSystem.validateConstraints(assignments, problem)

        val qualityResult = qualityScoringService.calculateQualityScore(
            solution.scheduledExams.map { convertToScheduledExam(it) },
            problem.professorPreferences,
            ConflictAnalysisResult(
                totalExamSlots = solution.scheduledExams.size,
                totalConflicts = validationResult.hardViolations.size + validationResult.softViolations.size,
                criticalViolations = validationResult.hardViolations.count { it.severity == ViolationSeverity.CRITICAL },
                timeConflicts = extractTimeConflicts(validationResult.hardViolations),
                spaceConflicts = extractSpaceConflicts(validationResult.hardViolations),
                professorConflicts = extractProfessorConflicts(validationResult.hardViolations)
            )
        )

        val recommendations = generateSolutionRecommendations(validationResult, qualityResult)

        return solution.copy(
            constraintViolations = validationResult.hardViolations + validationResult.softViolations,
            qualityScore = qualityResult.overallScore,
            optimizationMetrics = solution.optimizationMetrics.copy(
                constraintSatisfactionRate = validationResult.qualityScore
            )
        )
    }


    private fun convertToSchedulingResult(solution: SchedulingSolution, processingTime: Long): SchedulingResult {
        val metrics = SchedulingMetrics(
            totalCoursesScheduled = solution.scheduledExams.size,
            totalProfessorPreferencesConsidered = 0,
            preferencesSatisfied = 0,
            preferenceSatisfactionRate = solution.qualityScore,
            totalConflicts = solution.constraintViolations.count { it.severity == ViolationSeverity.CRITICAL },
            resolvedConflicts = 0,
            roomUtilizationRate = calculateRoomUtilization(solution.scheduledExams),
            averageStudentExamsPerDay = calculateAverageStudentExamsPerDay(solution.scheduledExams),
            processingTimeMs = processingTime
        )

        return SchedulingResult(
            scheduledExams = solution.scheduledExams,
            metrics = metrics,
            qualityScore = solution.qualityScore,
            violations = solution.constraintViolations
        )
    }


    private fun applyTimeSlotOptimization(
        solution: SchedulingSolution,
        problem: SchedulingProblem
    ): SchedulingSolution {
        val optimizedExams = solution.scheduledExams.map { exam ->
            val preferences = problem.professorPreferences.filter { it.courseId == exam.courseId }

            if (preferences.isNotEmpty()) {
                val bestTimeSlot = findBestTimeSlotForExam(exam, preferences, problem.availableRooms)
                exam.copy(
                    examDate = bestTimeSlot?.date ?: exam.examDate,
                    startTime = bestTimeSlot?.startTime ?: exam.startTime,
                    endTime = bestTimeSlot?.endTime ?: exam.endTime
                )
            } else {
                exam
            }
        }

        return solution.copy(scheduledExams = optimizedExams)
    }

    private fun applyRoomAllocationOptimization(
        solution: SchedulingSolution,
        problem: SchedulingProblem
    ): SchedulingSolution {
        val optimizedExams = solution.scheduledExams.map { exam ->
            val optimalRoom = findOptimalRoom(exam, problem.availableRooms)
            exam.copy(
                roomId = optimalRoom?.roomId ?: exam.roomId,
                roomName = optimalRoom?.roomName ?: exam.roomName,
                roomCapacity = optimalRoom?.capacity ?: exam.roomCapacity
            )
        }

        return solution.copy(scheduledExams = optimizedExams)
    }

    private fun applyConstraintViolationRepair(
        solution: SchedulingSolution,
        problem: SchedulingProblem
    ): SchedulingSolution {
        var repairedSolution = solution

        solution.constraintViolations
            .filter { it.severity == ViolationSeverity.CRITICAL }
            .forEach { violation ->
                repairedSolution = attemptViolationRepair(repairedSolution, violation, problem)
            }

        return repairedSolution
    }

    private fun applyPreferenceSatisfactionImprovement(
        solution: SchedulingSolution,
        problem: SchedulingProblem
    ): SchedulingSolution {
        val improvedExams = solution.scheduledExams.map { exam ->
            val coursePreferences = problem.professorPreferences.filter { it.courseId == exam.courseId }

            if (coursePreferences.isNotEmpty()) {
                improvePreferenceSatisfaction(exam, coursePreferences, problem)
            } else {
                exam
            }
        }

        return solution.copy(scheduledExams = improvedExams)
    }

    private fun calculateEstimatedDuration(credits: Int): Int {
        return when (credits) {
            in 1..3 -> 90
            in 4..6 -> 120
            in 7..9 -> 180
            else -> 120
        }
    }

    private fun extractRequiredEquipment(details: Map<String, Any>): Set<String> {
        return (details["requiredEquipment"] as? List<String>)?.toSet() ?: emptySet()
    }

    private fun extractAccessibilityRequirement(details: Map<String, Any>): Boolean {
        return details["accessibilityRequired"] as? Boolean ?: false
    }

    private fun generateSchedulingConstraints(
        courses: List<CourseSchedulingInfo>,
        institutionalConstraints: InstitutionalConstraints
    ): List<SchedulingConstraint> {
        val constraints = mutableListOf<SchedulingConstraint>()

        constraints.add(
            SchedulingConstraint(
                id = "NO_TIME_CONFLICTS",
                type = ConstraintType.TIME_CONFLICT,
                priority = ConstraintPriority.HARD,
                description = "No two exams can have overlapping times for students with both courses"
            )
        )

        constraints.add(
            SchedulingConstraint(
                id = "ROOM_CAPACITY",
                type = ConstraintType.ROOM_CAPACITY,
                priority = ConstraintPriority.HARD,
                description = "Room capacity must be sufficient for all enrolled students"
            )
        )

        constraints.add(
            SchedulingConstraint(
                id = "PROFESSOR_AVAILABILITY",
                type = ConstraintType.PROFESSOR_AVAILABILITY,
                priority = ConstraintPriority.HARD,
                description = "Professors cannot have conflicting exam times"
            )
        )

        constraints.add(
            SchedulingConstraint(
                id = "PREFERENCE_SATISFACTION",
                type = ConstraintType.PREFERENCE_SATISFACTION,
                priority = ConstraintPriority.SOFT,
                description = "Schedule should satisfy professor preferences when possible"
            )
        )

        return constraints
    }

    private fun calculateConstraintComplexity(problem: SchedulingProblem): Double {
        val courseCount = problem.courses.size
        val roomCount = problem.availableRooms.size
        val preferenceCount = problem.professorPreferences.size

        val roomConstraintRatio = courseCount.toDouble() / roomCount
        val preferenceRatio = preferenceCount.toDouble() / courseCount

        return (roomConstraintRatio * 0.4 + preferenceRatio * 0.6) / 2.0
    }

    private fun calculateTimeout(problem: SchedulingProblem): Long {
        val baseTimeout = 30000L // 30 seconds
        val courseMultiplier = problem.courses.size * 500L
        val constraintMultiplier = problem.constraints.size * 100L

        return baseTimeout + courseMultiplier + constraintMultiplier
    }

    private fun calculateRoomUtilization(scheduledExams: List<ScheduledExamInfo>): Double {
        val utilisations = scheduledExams.mapNotNull { exam ->
            if (exam.roomCapacity != null && exam.roomCapacity > 0) {
                exam.studentCount.toDouble() / exam.roomCapacity
            } else null
        }

        return if (utilisations.isNotEmpty()) utilisations.average() else 0.0
    }

    private fun calculateAverageStudentExamsPerDay(scheduledExams: List<ScheduledExamInfo>): Double {
        val examsByDate = scheduledExams.groupBy { it.examDate }
        val dailyStudentExams = examsByDate.values.map { examsOnDate ->
            examsOnDate.sumOf { it.studentCount }
        }

        return if (dailyStudentExams.isNotEmpty()) dailyStudentExams.average() else 0.0
    }

    private fun findBestTimeSlotForExam(
        exam: ScheduledExamInfo,
        preferences: List<ProfessorPreferenceInfo>,
        availableRooms: List<RoomInfo>
    ): TimeSlot? {
        val preferredDates = preferences.flatMap { it.preferredDates }.distinct()
        val preferredTimes = preferences.flatMap { it.preferredTimeSlots }.distinct()

        for (date in preferredDates) {
            for (timeSlot in preferredTimes) {
                val suitableRoom = availableRooms.find { it.capacity >= exam.studentCount }
                if (suitableRoom != null) {
                    return TimeSlot(
                        date = date,
                        startTime = timeSlot.startTime,
                        endTime = timeSlot.endTime,
                        roomId = suitableRoom.roomId,
                        roomName = suitableRoom.roomName,
                        roomCapacity = suitableRoom.capacity,
                        dayOfWeek = date.dayOfWeek.value
                    )
                }
            }
        }

        return null
    }

    private fun findOptimalRoom(exam: ScheduledExamInfo, availableRooms: List<RoomInfo>): RoomInfo? {
        return availableRooms
            .filter { it.capacity >= exam.studentCount }
            .minByOrNull {
                if (it.capacity >= exam.studentCount) {
                    it.capacity - exam.studentCount
                } else {
                    Int.MAX_VALUE
                }
            }
    }

    private fun attemptViolationRepair(
        solution: SchedulingSolution,
        violation: ConstraintViolation,
        problem: SchedulingProblem
    ): SchedulingSolution {
        return solution
    }

    private fun improvePreferenceSatisfaction(
        exam: ScheduledExamInfo,
        preferences: List<ProfessorPreferenceInfo>,
        problem: SchedulingProblem
    ): ScheduledExamInfo {
        return exam
    }

    private fun convertToScheduledExam(examInfo: ScheduledExamInfo): ScheduledExam {
        return ScheduledExam(
            id = UUID.randomUUID(),
            scheduledExamId = examInfo.scheduledExamId,
            courseId = examInfo.courseId,
            courseName = examInfo.courseName,
            examDate = examInfo.examDate,
            startTime = examInfo.startTime,
            endTime = examInfo.endTime,
            roomId = examInfo.roomId,
            roomName = examInfo.roomName,
            roomCapacity = examInfo.roomCapacity,
            studentCount = examInfo.studentCount,
            mandatoryStatus = examInfo.mandatoryStatus,
            professorIds = examInfo.professorIds.toMutableSet(),
            examSessionSchedule = null
        )
    }

    private fun extractTimeConflicts(violations: List<ConstraintViolation>): List<TimeConflict> {
        return violations
            .filter { it.violationType == "STUDENT_TIME_CONFLICT" || it.violationType == "PROFESSOR_TIME_CONFLICT" }
            .mapIndexed { index, violation ->
                TimeConflict(
                    examId1 = violation.affectedExams.getOrNull(0) ?: "unknown",
                    examId2 = violation.affectedExams.getOrNull(1) ?: "unknown",
                    conflictType = violation.violationType,
                    severity = ConflictSeverity.valueOf(violation.severity.name),
                    affectedStudents = violation.affectedStudents
                )
            }
    }

    private fun extractSpaceConflicts(violations: List<ConstraintViolation>): List<SpaceConflict> {
        return violations
            .filter { it.violationType.contains("ROOM") }
            .map { violation ->
                SpaceConflict(
                    examId = violation.affectedExams.firstOrNull() ?: "unknown",
                    roomId = "unknown",
                    requiredCapacity = violation.affectedStudents,
                    availableCapacity = 0,
                    overflowCount = violation.affectedStudents
                )
            }
    }

    private fun extractProfessorConflicts(violations: List<ConstraintViolation>): List<ProfessorConflict> {
        return violations
            .filter { it.violationType == "PROFESSOR_TIME_CONFLICT" }
            .map { violation ->
                ProfessorConflict(
                    professorId = "unknown",
                    conflictingExamIds = violation.affectedExams,
                    conflictTime = "unknown",
                    severity = ConflictSeverity.valueOf(violation.severity.name)
                )
            }
    }

    private fun generateSolutionRecommendations(
        validationResult: ConstraintValidationResult,
        qualityResult: QualityScoreResult
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (validationResult.hardViolations.isNotEmpty()) {
            recommendations.add("Critical constraints violated - schedule may not be viable")
        }

        if (qualityResult.preferenceSatisfactionScore < 0.6) {
            recommendations.add("Low preference satisfaction - consider adjusting constraints or increasing flexibility")
        }

        recommendations.addAll(qualityResult.breakdown.recommendations)

        return recommendations
    }

    private fun generateEmergencyFallback(
        courseEnrollmentData: Map<String, CourseEnrollmentInfo>,
        courseAccreditationData: Map<String, CourseAccreditationInfo>,
        availableRooms: List<RoomInfo>,
        examPeriod: ExamPeriod,
        originalException: Exception
    ): SchedulingResult {
        logger.error("Generating emergency fallback schedule due to: {}", originalException.message)

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
                    violationType = "EMERGENCY_FALLBACK",
                    severity = ViolationSeverity.HIGH,
                    description = "Emergency fallback schedule generated due to solver failure: ${originalException.message}",
                    affectedExams = fallbackExams.map { it.courseId },
                    affectedStudents = fallbackExams.sumOf { it.studentCount },
                    suggestedResolution = "Review scheduling parameters and retry with adjusted constraints"
                )
            )
        )
    }
}