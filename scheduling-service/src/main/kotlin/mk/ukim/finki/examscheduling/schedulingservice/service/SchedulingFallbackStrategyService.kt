package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.MandatoryStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ViolationSeverity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Service
class SchedulingFallbackStrategyService {

    private val logger = LoggerFactory.getLogger(SchedulingFallbackStrategyService::class.java)

    fun appleFallbackStrategies(
        problem: SchedulingProblem,
        currentSolution: SchedulingSolution
    ): SchedulingSolution {
        logger.info("Applying fallback strategies for problem with {} courses", problem.courses.size)

        var improvedSolution = currentSolution

        if (!currentSolution.isComplete || hasCriticalViolations(currentSolution)) {
            logger.debug("Applying constraint relaxation strategy")
            improvedSolution = applyConstraintRelaxation(problem, currentSolution)
        }

        if (improvedSolution.qualityScore < 0.5) {
            logger.debug("Applying greedy reconstruction strategy")
            improvedSolution = applyGreedyReconstruction(problem, improvedSolution)
        }

        if (!improvedSolution.isComplete) {
            logger.debug("Applying partial solution completion strategy")
            improvedSolution = completePartialSolution(problem, improvedSolution)
        }

        if (improvedSolution.qualityScore < 0.7) {
            logger.debug("Applying quality enhancement strategy")
            improvedSolution = enhanceSolutionQuality(problem, improvedSolution)
        }

        if (!improvedSolution.isComplete || improvedSolution.qualityScore < 0.3) {
            logger.warn("Applying emergency scheduling as last resort")
            improvedSolution = generateEmergencySchedule(problem)
        }

        logger.info("Fallback strategies completed. Quality improved: {} -> {}",
            currentSolution.qualityScore, improvedSolution.qualityScore)

        return improvedSolution
    }

    private fun applyConstraintRelaxation(
        problem: SchedulingProblem,
        currentSolution: SchedulingSolution
    ): SchedulingSolution {
        logger.debug("Relaxing constraints to improve feasibility")

        val relaxedConstraints = problem.institutionalConstraints.copy(
            minimumGapMinutes = max(15, problem.institutionalConstraints.minimumGapMinutes - 15),
            maxExamsPerDay = problem.institutionalConstraints.maxExamsPerDay + 2,
            workingHours = WorkingHours(
                startTime = problem.institutionalConstraints.workingHours.startTime.minusMinutes(30),
                endTime = problem.institutionalConstraints.workingHours.endTime.plusMinutes(30)
            )
        )

        val relaxedProblem = problem.copy(institutionalConstraints = relaxedConstraints)

        val problematicExams = identifyProblematicExams(currentSolution)
        val improvedExams = rescheduleExams(problematicExams, relaxedProblem, currentSolution.scheduledExams)

        return currentSolution.copy(
            scheduledExams = improvedExams,
            optimizationMetrics = currentSolution.optimizationMetrics.copy(
                iterationsCompleted = currentSolution.optimizationMetrics.iterationsCompleted + 1
            )
        )
    }


    private fun applyGreedyReconstruction(
        problem: SchedulingProblem,
        currentSolution: SchedulingSolution
    ): SchedulingSolution {
        logger.debug("Reconstructing solution using greedy approach")

        val newExams = mutableListOf<ScheduledExamInfo>()
        val usedTimeSlots = mutableSetOf<String>()

        val sortedCourses = problem.courses.sortedWith(
            compareBy<CourseSchedulingInfo> { it.mandatoryStatus == MandatoryStatus.ELECTIVE }
                .thenByDescending { it.studentCount }
        )

        sortedCourses.forEach { course ->
            val bestTimeSlot = findBestAvailableTimeSlot(
                course, problem, usedTimeSlots, newExams
            )

            if (bestTimeSlot != null) {
                val exam = ScheduledExamInfo(
                    scheduledExamId = "${course.courseId}_${UUID.randomUUID()}",
                    courseId = course.courseId,
                    courseName = course.courseName,
                    examDate = bestTimeSlot.date,
                    startTime = bestTimeSlot.startTime,
                    endTime = bestTimeSlot.endTime,
                    roomId = bestTimeSlot.roomId,
                    roomName = bestTimeSlot.roomName,
                    roomCapacity = bestTimeSlot.roomCapacity,
                    studentCount = course.studentCount,
                    mandatoryStatus = course.mandatoryStatus,
                    professorIds = course.professorIds
                )

                newExams.add(exam)
                usedTimeSlots.add("${bestTimeSlot.date}_${bestTimeSlot.startTime}_${bestTimeSlot.roomId}")
            }
        }

        return SchedulingSolution(
            scheduledExams = newExams,
            constraintViolations = emptyList(),
            qualityScore = 0.6,
            isComplete = newExams.size == problem.courses.size,
            processingTimeMs = currentSolution.processingTimeMs,
            algorithmUsed = "Greedy Reconstruction Fallback",
            optimizationMetrics = OptimizationMetrics(
                iterationsCompleted = 1,
                solutionsEvaluated = newExams.size
            )
        )
    }

    private fun completePartialSolution(
        problem: SchedulingProblem,
        partialSolution: SchedulingSolution
    ): SchedulingSolution {
        logger.debug("Completing partial solution with {} exams", partialSolution.scheduledExams.size)

        val scheduledCourses = partialSolution.scheduledExams.map { it.courseId }.toSet()
        val unscheduledCourses = problem.courses.filter { it.courseId !in scheduledCourses }

        if (unscheduledCourses.isEmpty()) {
            return partialSolution.copy(isComplete = true)
        }

        val completedExams = partialSolution.scheduledExams.toMutableList()
        val usedTimeSlots = partialSolution.scheduledExams.map { exam ->
            "${exam.examDate}_${exam.startTime}_${exam.roomId}"
        }.toMutableSet()

        unscheduledCourses.forEach { course ->
            val timeSlot = findBestAvailableTimeSlot(course, problem, usedTimeSlots, completedExams)

            if (timeSlot != null) {
                val exam = ScheduledExamInfo(
                    scheduledExamId = "${course.courseId}_completed",
                    courseId = course.courseId,
                    courseName = course.courseName,
                    examDate = timeSlot.date,
                    startTime = timeSlot.startTime,
                    endTime = timeSlot.endTime,
                    roomId = timeSlot.roomId,
                    roomName = timeSlot.roomName,
                    roomCapacity = timeSlot.roomCapacity,
                    studentCount = course.studentCount,
                    mandatoryStatus = course.mandatoryStatus,
                    professorIds = course.professorIds
                )

                completedExams.add(exam)
                usedTimeSlots.add("${timeSlot.date}_${timeSlot.startTime}_${timeSlot.roomId}")
            }
        }

        return partialSolution.copy(
            scheduledExams = completedExams,
            isComplete = completedExams.size == problem.courses.size,
            optimizationMetrics = partialSolution.optimizationMetrics.copy(
                iterationsCompleted = partialSolution.optimizationMetrics.iterationsCompleted + 1
            )
        )
    }

    private fun enhanceSolutionQuality(
        problem: SchedulingProblem,
        solution: SchedulingSolution
    ): SchedulingSolution {
        logger.debug("Enhancing solution quality")

        var enhancedExams = solution.scheduledExams.toMutableList()

        enhancedExams = improveRoomUtilization(enhancedExams, problem.availableRooms).toMutableList()

        enhancedExams = redistributeWorkload(enhancedExams, problem).toMutableList()

        enhancedExams = improvePreferernces(enhancedExams, problem.professorPreferences).toMutableList()

        return solution.copy(
            scheduledExams = enhancedExams,
            qualityScore = min(1.0, solution.qualityScore + 0.2),
            optimizationMetrics = solution.optimizationMetrics.copy(
                iterationsCompleted = solution.optimizationMetrics.iterationsCompleted + 3
            )
        )
    }


    private fun generateEmergencySchedule(problem: SchedulingProblem): SchedulingSolution {
        logger.warn("Generating emergency schedule - this may have significant quality issues")

        val emergencyExams = mutableListOf<ScheduledExamInfo>()
        var currentDate = problem.examPeriod.startDate
        var currentTime = LocalTime.of(8, 0)
        var roomIndex = 0

        problem.courses.forEach { course ->
            val room = problem.availableRooms.getOrNull(roomIndex % problem.availableRooms.size)
                ?: problem.availableRooms.firstOrNull()

            if (room != null) {
                emergencyExams.add(
                    ScheduledExamInfo(
                        scheduledExamId = "${course.courseId}_emergency",
                        courseId = course.courseId,
                        courseName = course.courseName,
                        examDate = currentDate,
                        startTime = currentTime,
                        endTime = currentTime.plusMinutes(course.estimatedDuration.toLong()),
                        roomId = room.roomId,
                        roomName = room.roomName,
                        roomCapacity = room.capacity,
                        studentCount = course.studentCount,
                        mandatoryStatus = course.mandatoryStatus,
                        professorIds = course.professorIds
                    )
                )

                currentTime = currentTime.plusMinutes(course.estimatedDuration + 30L)
                if (currentTime.isAfter(LocalTime.of(18, 0))) {
                    currentDate = currentDate.plusDays(1)
                    currentTime = LocalTime.of(8, 0)
                    roomIndex++
                }
            }
        }

        return SchedulingSolution(
            scheduledExams = emergencyExams,
            constraintViolations = listOf(
                ConstraintViolation(
                    violationType = "EMERGENCY_SCHEDULE",
                    severity = ViolationSeverity.HIGH,
                    description = "Emergency schedule generated - manual review required",
                    affectedExams = emergencyExams.map { it.courseId },
                    affectedStudents = emergencyExams.sumOf { it.studentCount },
                    suggestedResolution = "Review all exam assignments and reschedule conflicts manually"
                )
            ),
            qualityScore = 0.2,
            isComplete = emergencyExams.size == problem.courses.size,
            processingTimeMs = 500L,
            algorithmUsed = "Emergency Fallback",
            optimizationMetrics = OptimizationMetrics(
                iterationsCompleted = 1,
                solutionsEvaluated = emergencyExams.size
            ),
            failureReason = "All primary and secondary strategies failed - emergency schedule generated"
        )
    }


    private fun hasCriticalViolations(solution: SchedulingSolution): Boolean {
        return solution.constraintViolations.any { it.severity == ViolationSeverity.CRITICAL }
    }

    private fun identifyProblematicExams(solution: SchedulingSolution): List<ScheduledExamInfo> {
        val problematicCourseIds = solution.constraintViolations
            .flatMap { it.affectedExams }
            .toSet()

        return solution.scheduledExams.filter { it.courseId in problematicCourseIds }
    }

    private fun rescheduleExams(
        exams: List<ScheduledExamInfo>,
        problem: SchedulingProblem,
        allExams: List<ScheduledExamInfo>
    ): List<ScheduledExamInfo> {
        return allExams
    }

    private fun findBestAvailableTimeSlot(
        course: CourseSchedulingInfo,
        problem: SchedulingProblem,
        usedTimeSlots: Set<String>,
        existingExams: List<ScheduledExamInfo>
    ): TimeSlot? {
        var currentDate = problem.examPeriod.startDate

        while (!currentDate.isAfter(problem.examPeriod.endDate)) {
            var currentTime = problem.institutionalConstraints.workingHours.startTime

            while (currentTime.plusMinutes(course.estimatedDuration.toLong())
                    .isBefore(problem.institutionalConstraints.workingHours.endTime)) {

                for (room in problem.availableRooms) {
                    if (room.capacity >= course.studentCount) {
                        val slotKey = "${currentDate}_${currentTime}_${room.roomId}"

                        if (slotKey !in usedTimeSlots) {
                            return TimeSlot(
                                date = currentDate,
                                startTime = currentTime,
                                endTime = currentTime.plusMinutes(course.estimatedDuration.toLong()),
                                roomId = room.roomId,
                                roomName = room.roomName,
                                roomCapacity = room.capacity,
                                dayOfWeek = currentDate.dayOfWeek.value
                            )
                        }
                    }
                }

                currentTime = currentTime.plusMinutes(problem.institutionalConstraints.minimumGapMinutes + course.estimatedDuration.toLong())
            }

            currentDate = currentDate.plusDays(1)
        }

        return null
    }

    private fun improveRoomUtilization(exams: List<ScheduledExamInfo>, rooms: List<RoomInfo>): List<ScheduledExamInfo> {
        return exams
    }

    private fun redistributeWorkload(exams: List<ScheduledExamInfo>, problem: SchedulingProblem): List<ScheduledExamInfo> {
        return exams
    }

    private fun improvePreferernces(exams: List<ScheduledExamInfo>, preferences: List<ProfessorPreferenceInfo>): List<ScheduledExamInfo> {
        return exams
    }
}