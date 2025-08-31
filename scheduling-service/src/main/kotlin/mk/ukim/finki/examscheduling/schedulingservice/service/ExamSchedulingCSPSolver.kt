package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.exp
import kotlin.random.Random

@Service
class ExamSchedulingCSPSolver {

    private val logger = LoggerFactory.getLogger(ExamSchedulingCSPSolver::class.java)


    fun solve(problem: SchedulingProblem): SchedulingSolution {
        val startTime = System.currentTimeMillis()
        logger.info("Starting CSP solving for {} courses with {} constraints",
            problem.courses.size, problem.constraints.size)

        val cspState = initializeCSPState(problem)

        val solution = when (problem.solvingStrategy) {
            SolvingStrategy.BACKTRACKING_WITH_FORWARD_CHECKING -> solveWithBacktrackingFC(cspState)
            SolvingStrategy.SIMULATED_ANNEALING -> solveWithSimulatedAnnealing(cspState)
            SolvingStrategy.HYBRID_APPROACH -> solveWithHybridApproach(cspState)
            SolvingStrategy.GREEDY_WITH_BACKTRACKING -> solveWithGreedyBacktracking(cspState)
        }

        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime

        logger.info("CSP solving completed in {}ms with {} violations",
            processingTime, solution.constraintViolations.size)

        return solution.copy(
            processingTimeMs = processingTime,
            algorithmUsed = problem.solvingStrategy.name
        )
    }

    private fun initializeCSPState(problem: SchedulingProblem): CSPState {
        val variables = mutableMapOf<String, Variable>()
        val domains = mutableMapOf<String, MutableSet<TimeSlot>>()

        problem.courses.forEach { course ->
            val variable = Variable(
                courseId = course.courseId,
                courseName = course.courseName,
                studentCount = course.studentCount,
                professorIds = course.professorIds,
                mandatoryStatus = course.mandatoryStatus,
                estimatedDuration = course.estimatedDuration
            )
            variables[course.courseId] = variable

            domains[course.courseId] = generateDomain(course, problem).toMutableSet()
        }

        return CSPState(
            problem = problem,
            variables = variables,
            domains = domains,
            assignments = mutableMapOf(),
            constraintViolations = mutableListOf()
        )
    }

    private fun generateDomain(course: CourseSchedulingInfo, problem: SchedulingProblem): List<TimeSlot> {
        val domain = mutableListOf<TimeSlot>()

        val examPeriod = problem.examPeriod
        var currentDate = examPeriod.startDate

        while (!currentDate.isAfter(examPeriod.endDate)) {
            val daySlots = generateTimeSlotsForDay(currentDate, course, problem)
            domain.addAll(daySlots)
            currentDate = currentDate.plusDays(1)
        }

        return filterDomainByPreferences(domain, course, problem.professorPreferences)
    }

    private fun generateTimeSlotsForDay(
        date: LocalDate,
        course: CourseSchedulingInfo,
        problem: SchedulingProblem
    ): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()
        val workingHours = problem.institutionalConstraints.workingHours

        var currentTime = workingHours.startTime

        while (currentTime.plusMinutes(course.estimatedDuration.toLong()).isBefore(workingHours.endTime) ||
            currentTime.plusMinutes(course.estimatedDuration.toLong()) == workingHours.endTime) {

            val endTime = currentTime.plusMinutes(course.estimatedDuration.toLong())

            problem.availableRooms.forEach { room ->
                if (room.capacity >= course.studentCount) {
                    slots.add(
                        TimeSlot(
                            date = date,
                            startTime = currentTime,
                            endTime = endTime,
                            roomId = room.roomId,
                            roomName = room.roomName,
                            roomCapacity = room.capacity,
                            dayOfWeek = date.dayOfWeek.value
                        )
                    )
                }
            }

            currentTime = currentTime.plusMinutes(problem.institutionalConstraints.minimumGapMinutes.toLong())
        }

        return slots
    }


    private fun filterDomainByPreferences(
        domain: List<TimeSlot>,
        course: CourseSchedulingInfo,
        preferences: List<ProfessorPreferenceInfo>
    ): List<TimeSlot> {
        val coursePreferences = preferences.filter { it.courseId == course.courseId }

        if (coursePreferences.isEmpty()) return domain

        return domain.filter { timeSlot ->
            coursePreferences.any { preference ->
                isTimeSlotCompatibleWithPreference(timeSlot, preference)
            }
        }
    }


    private fun isTimeSlotCompatibleWithPreference(
        timeSlot: TimeSlot,
        preference: ProfessorPreferenceInfo
    ): Boolean {
        if (preference.preferredDates.isNotEmpty() &&
            !preference.preferredDates.contains(timeSlot.date)) {
            return false
        }

        if (preference.unavailableDates.contains(timeSlot.date)) {
            return false
        }

        if (preference.preferredTimeSlots.isNotEmpty()) {
            val hasCompatibleTimeSlot = preference.preferredTimeSlots.any { prefTimeSlot ->
                timeSlotsOverlap(
                    timeSlot.startTime, timeSlot.endTime,
                    prefTimeSlot.startTime, prefTimeSlot.endTime
                ) || (prefTimeSlot.dayOfWeek == null || prefTimeSlot.dayOfWeek == timeSlot.dayOfWeek)
            }
            if (!hasCompatibleTimeSlot) return false
        }
        if (preference.unavailableTimeSlots.isNotEmpty()) {
            val hasConflictingTimeSlot = preference.unavailableTimeSlots.any { unavailableSlot ->
                timeSlotsOverlap(
                    timeSlot.startTime, timeSlot.endTime,
                    unavailableSlot.startTime, unavailableSlot.endTime
                )
            }
            if (hasConflictingTimeSlot) return false
        }

        return true
    }

    private fun solveWithBacktrackingFC(cspState: CSPState): SchedulingSolution {
        logger.debug("Solving with backtracking + forward checking")

        val result = backtrackWithForwardChecking(cspState, ArrayList(cspState.variables.keys))

        return if (result) {
            createSuccessfulSolution(cspState)
        } else {
            createPartialSolution(cspState, "Backtracking with forward checking could not find complete solution")
        }
    }

    private fun backtrackWithForwardChecking(state: CSPState, unassigned: MutableList<String>): Boolean {
        if (unassigned.isEmpty()) {
            return validateAllConstraints(state)
        }

        val variable = selectVariableMRV(unassigned, state)
        unassigned.remove(variable)

        val orderedDomain = orderDomainLCV(variable, state)

        for (timeSlot in orderedDomain) {
            state.assignments[variable] = timeSlot

            val removedValues = forwardCheck(variable, timeSlot, state)

            if (removedValues != null) {
                if (backtrackWithForwardChecking(state, unassigned)) {
                    return true
                }
            }

            restoreDomains(removedValues, state)
            state.assignments.remove(variable)
        }

        unassigned.add(variable)
        return false
    }

    private fun selectVariableMRV(unassigned: List<String>, state: CSPState): String {
        return unassigned.minByOrNull { variable ->
            state.domains[variable]?.size ?: Int.MAX_VALUE
        } ?: unassigned.first()
    }

    private fun orderDomainLCV(variable: String, state: CSPState): List<TimeSlot> {
        val domain = state.domains[variable] ?: return emptyList()

        return domain.sortedBy { timeSlot ->
            var eliminatedCount = 0
            state.domains.forEach { (otherVar, otherDomain) ->
                if (otherVar != variable) {
                    eliminatedCount += otherDomain.count { otherSlot ->
                        wouldViolateConstraints(variable, timeSlot, otherVar, otherSlot, state)
                    }
                }
            }
            eliminatedCount
        }
    }

    private fun forwardCheck(
        assignedVar: String,
        assignedSlot: TimeSlot,
        state: CSPState
    ): MutableMap<String, MutableSet<TimeSlot>>? {
        val removedValues = mutableMapOf<String, MutableSet<TimeSlot>>()

        state.domains.forEach { (variable, domain) ->
            if (variable != assignedVar) {
                val toRemove = domain.filter { timeSlot ->
                    wouldViolateConstraints(assignedVar, assignedSlot, variable, timeSlot, state)
                }.toMutableSet()

                if (toRemove.isNotEmpty()) {
                    removedValues[variable] = toRemove
                    domain.removeAll(toRemove)

                    // Check for domain wipe-out
                    if (domain.isEmpty()) {
                        restoreDomains(removedValues, state)
                        return null
                    }
                }
            }
        }

        return removedValues
    }


    private fun restoreDomains(
        removedValues: MutableMap<String, MutableSet<TimeSlot>>?,
        state: CSPState
    ) {
        removedValues?.forEach { (variable, values) ->
            state.domains[variable]?.addAll(values)
        }
    }


    private fun solveWithSimulatedAnnealing(cspState: CSPState): SchedulingSolution {
        logger.debug("Solving with simulated annealing")

        val currentSolution = generateRandomSolution(cspState)
        var bestSolution = currentSolution.copy()

        var temperature = 1000.0
        val coolingRate = 0.995
        val minTemperature = 0.1
        val maxIterations = 10000

        var iteration = 0

        while (temperature > minTemperature && iteration < maxIterations) {
            val neighborSolution = generateNeighbor(currentSolution, cspState)

            val currentEnergy = calculateEnergy(currentSolution, cspState)
            val neighborEnergy = calculateEnergy(neighborSolution, cspState)

            if (shouldAccept(currentEnergy, neighborEnergy, temperature)) {
                currentSolution.assignments.clear()
                currentSolution.assignments.putAll(neighborSolution.assignments)

                if (neighborEnergy < calculateEnergy(bestSolution, cspState)) {
                    bestSolution = neighborSolution.copy()
                }
            }

            temperature *= coolingRate
            iteration++
        }

        logger.debug("Simulated annealing completed after {} iterations", iteration)

        return createSolutionFromAssignments(bestSolution, cspState, "Simulated Annealing")
    }

    private fun generateRandomSolution(state: CSPState): CSPSolution {
        val assignments = mutableMapOf<String, TimeSlot>()

        state.variables.keys.forEach { variable ->
            val domain = state.domains[variable]
            if (!domain.isNullOrEmpty()) {
                assignments[variable] = domain.random()
            }
        }

        return CSPSolution(assignments = assignments)
    }


    private fun generateNeighbor(current: CSPSolution, state: CSPState): CSPSolution {
        val neighbor = current.copy()

        val variables = neighbor.assignments.keys.toList()
        if (variables.isEmpty()) return neighbor

        val selectedVariable = variables.random()
        val domain = state.domains[selectedVariable]

        if (!domain.isNullOrEmpty()) {
            neighbor.assignments[selectedVariable] = domain.random()
        }

        return neighbor
    }


    private fun calculateEnergy(solution: CSPSolution, state: CSPState): Double {
        var energy = 0.0

        val hardViolations = countHardConstraintViolations(solution, state)
        energy += hardViolations * 1000.0

        val softViolations = countSoftConstraintViolations(solution, state)
        energy += softViolations * 10.0

        energy += calculateRoomUtilizationPenalty(solution, state)

        energy += calculateWorkloadDistributionPenalty(solution, state)

        return energy
    }


    private fun shouldAccept(currentEnergy: Double, neighborEnergy: Double, temperature: Double): Boolean {
        if (neighborEnergy < currentEnergy) return true

        val probability = exp((currentEnergy - neighborEnergy) / temperature)
        return Random.nextDouble() < probability
    }

    private fun solveWithHybridApproach(cspState: CSPState): SchedulingSolution {
        logger.debug("Solving with hybrid approach")

        val backtrackingSolution = solveWithBacktrackingFC(cspState.copy())

        if (backtrackingSolution.isComplete && backtrackingSolution.constraintViolations.isEmpty()) {
            return backtrackingSolution
        }

        val optimizedSolution = solveWithSimulatedAnnealing(cspState.copy())

        return applyLocalImprovements(optimizedSolution, cspState)
    }

    private fun applyLocalImprovements(solution: SchedulingSolution, state: CSPState): SchedulingSolution {
        logger.debug("Applying local improvements")

        val improved = solution.copy()
        var improvementMade = true
        var iterations = 0
        val maxIterations = 100

        while (improvementMade && iterations < maxIterations) {
            improvementMade = false

            val violations = improved.constraintViolations.toList()
            for (violation in violations) {
                if (tryToFixViolation(violation, improved, state)) {
                    improvementMade = true
                    break
                }
            }

            iterations++
        }

        logger.debug("Local improvements completed after {} iterations", iterations)
        return improved
    }


    private fun wouldViolateConstraints(
        var1: String, slot1: TimeSlot,
        var2: String, slot2: TimeSlot,
        state: CSPState
    ): Boolean {
        if (slot1.date == slot2.date &&
            timeSlotsOverlap(slot1.startTime, slot1.endTime, slot2.startTime, slot2.endTime)) {
            return true
        }

        if (slot1.roomId == slot2.roomId && slot1.date == slot2.date &&
            timeSlotsOverlap(slot1.startTime, slot1.endTime, slot2.startTime, slot2.endTime)) {
            return true
        }

        val prof1 = state.variables[var1]?.professorIds ?: emptySet()
        val prof2 = state.variables[var2]?.professorIds ?: emptySet()

        if (prof1.intersect(prof2).isNotEmpty() && slot1.date == slot2.date &&
            timeSlotsOverlap(slot1.startTime, slot1.endTime, slot2.startTime, slot2.endTime)) {
            return true
        }

        return false
    }

    private fun validateAllConstraints(state: CSPState): Boolean {
        val violations = findAllConstraintViolations(state)
        state.constraintViolations.clear()
        state.constraintViolations.addAll(violations)
        return violations.isEmpty()
    }

    private fun timeSlotsOverlap(start1: LocalTime, end1: LocalTime, start2: LocalTime, end2: LocalTime): Boolean {
        return start1.isBefore(end2) && start2.isBefore(end1)
    }

    private fun createSuccessfulSolution(state: CSPState): SchedulingSolution {
        return SchedulingSolution(
            scheduledExams = state.assignments.map { (courseId, timeSlot) ->
                val variable = state.variables[courseId]!!
                ScheduledExamInfo(
                    scheduledExamId = "${courseId}_${UUID.randomUUID()}",
                    courseId = courseId,
                    courseName = variable.courseName,
                    examDate = timeSlot.date,
                    startTime = timeSlot.startTime,
                    endTime = timeSlot.endTime,
                    roomId = timeSlot.roomId,
                    roomName = timeSlot.roomName,
                    roomCapacity = timeSlot.roomCapacity,
                    studentCount = variable.studentCount,
                    mandatoryStatus = variable.mandatoryStatus,
                    professorIds = variable.professorIds
                )
            },
            constraintViolations = state.constraintViolations.toList(),
            qualityScore = calculateQualityScore(state),
            isComplete = true,
            processingTimeMs = 0L,
            algorithmUsed = "",
            optimizationMetrics = calculateOptimizationMetrics(state)
        )
    }

    private fun createPartialSolution(state: CSPState, reason: String): SchedulingSolution {
        return SchedulingSolution(
            scheduledExams = state.assignments.map { (courseId, timeSlot) ->
                val variable = state.variables[courseId]!!
                ScheduledExamInfo(
                    scheduledExamId = "${courseId}_${UUID.randomUUID()}",
                    courseId = courseId,
                    courseName = variable.courseName,
                    examDate = timeSlot.date,
                    startTime = timeSlot.startTime,
                    endTime = timeSlot.endTime,
                    roomId = timeSlot.roomId,
                    roomName = timeSlot.roomName,
                    roomCapacity = timeSlot.roomCapacity,
                    studentCount = variable.studentCount,
                    mandatoryStatus = variable.mandatoryStatus,
                    professorIds = variable.professorIds
                )
            },
            constraintViolations = state.constraintViolations.toList(),
            qualityScore = calculateQualityScore(state),
            isComplete = false,
            processingTimeMs = 0L,
            algorithmUsed = "",
            optimizationMetrics = calculateOptimizationMetrics(state),
            failureReason = reason
        )
    }

    private fun solveWithGreedyBacktracking(cspState: CSPState): SchedulingSolution = solveWithBacktrackingFC(cspState)
    private fun countHardConstraintViolations(solution: CSPSolution, state: CSPState): Int = 0
    private fun countSoftConstraintViolations(solution: CSPSolution, state: CSPState): Int = 0
    private fun calculateRoomUtilizationPenalty(solution: CSPSolution, state: CSPState): Double = 0.0
    private fun calculateWorkloadDistributionPenalty(solution: CSPSolution, state: CSPState): Double = 0.0
    private fun createSolutionFromAssignments(solution: CSPSolution, state: CSPState, algorithm: String): SchedulingSolution = createSuccessfulSolution(state)
    private fun tryToFixViolation(violation: ConstraintViolation, solution: SchedulingSolution, state: CSPState): Boolean = false
    private fun findAllConstraintViolations(state: CSPState): List<ConstraintViolation> = emptyList()
    private fun calculateQualityScore(state: CSPState): Double = 0.8
    private fun calculateOptimizationMetrics(state: CSPState): OptimizationMetrics = OptimizationMetrics()
}
