package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.ConstraintValidationResult
import mk.ukim.finki.examscheduling.schedulingservice.domain.ConstraintViolation
import mk.ukim.finki.examscheduling.schedulingservice.domain.SchedulingProblem
import mk.ukim.finki.examscheduling.schedulingservice.domain.TimeSlot
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.MandatoryStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ViolationSeverity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalTime
import kotlin.math.abs

@Component
class ExamSchedulingConstraintSystem {

    private val logger = LoggerFactory.getLogger(ExamSchedulingConstraintSystem::class.java)

    fun validateConstraints(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): ConstraintValidationResult {
        val hardViolations = mutableListOf<ConstraintViolation>()
        val softViolations = mutableListOf<ConstraintViolation>()

        hardViolations.addAll(validateHardConstraints(assignments, problem))

        softViolations.addAll(validateSoftConstraints(assignments, problem))

        val isValid = hardViolations.isEmpty()
        val qualityScore = calculateConstraintQualityScore(hardViolations, softViolations, assignments.size)

        return ConstraintValidationResult(
            isValid = isValid,
            hardViolations = hardViolations,
            softViolations = softViolations,
            qualityScore = qualityScore,
            totalConstraintsChecked = assignments.size * (assignments.size - 1) / 2
        )
    }

    // === HARD CONSTRAINTS ===

    private fun validateHardConstraints(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()

        violations.addAll(checkStudentTimeConflicts(assignments, problem))

        violations.addAll(checkProfessorTimeConflicts(assignments, problem))

        violations.addAll(checkRoomCapacityConstraints(assignments, problem))

        violations.addAll(checkRoomBookingConflicts(assignments, problem))

        violations.addAll(checkTimingConstraints(assignments, problem))

        violations.addAll(checkResourceConstraints(assignments, problem))

        violations.addAll(checkInstitutionalPolicyConstraints(assignments, problem))

        return violations
    }

    private fun checkStudentTimeConflicts(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()
        val assignmentList = assignments.toList()

        for (i in assignmentList.indices) {
            for (j in i + 1 until assignmentList.size) {
                val (courseId1, timeSlot1) = assignmentList[i]
                val (courseId2, timeSlot2) = assignmentList[j]

                if (timeSlot1.date == timeSlot2.date &&
                    timeSlotsOverlap(timeSlot1.startTime, timeSlot1.endTime, timeSlot2.startTime, timeSlot2.endTime)) {

                    val studentOverlap = estimateStudentOverlap(courseId1, courseId2, problem)

                    if (studentOverlap > 0) {
                        violations.add(
                            ConstraintViolation(
                                violationType = "STUDENT_TIME_CONFLICT",
                                severity = ViolationSeverity.CRITICAL,
                                description = "Time conflict for $studentOverlap students between courses $courseId1 and $courseId2",
                                affectedExams = listOf(courseId1, courseId2),
                                affectedStudents = studentOverlap,
                                suggestedResolution = "Reschedule one of the conflicting exams to a different time slot"
                            )
                        )
                    }
                }
            }
        }

        return violations
    }

    private fun checkProfessorTimeConflicts(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()
        val assignmentList = assignments.toList()

        for (i in assignmentList.indices) {
            for (j in i + 1 until assignmentList.size) {
                val (courseId1, timeSlot1) = assignmentList[i]
                val (courseId2, timeSlot2) = assignmentList[j]

                if (timeSlot1.date == timeSlot2.date &&
                    timeSlotsOverlap(timeSlot1.startTime, timeSlot1.endTime, timeSlot2.startTime, timeSlot2.endTime)) {

                    val course1Info = problem.courses.find { it.courseId == courseId1 }
                    val course2Info = problem.courses.find { it.courseId == courseId2 }

                    if (course1Info != null && course2Info != null) {
                        val sharedProfessors = course1Info.professorIds.intersect(course2Info.professorIds)

                        if (sharedProfessors.isNotEmpty()) {
                            violations.add(
                                ConstraintViolation(
                                    violationType = "PROFESSOR_TIME_CONFLICT",
                                    severity = ViolationSeverity.CRITICAL,
                                    description = "Professor(s) ${sharedProfessors.joinToString(", ")} have conflicting exams for courses $courseId1 and $courseId2",
                                    affectedExams = listOf(courseId1, courseId2),
                                    affectedStudents = 0,
                                    suggestedResolution = "Reschedule one exam or assign different professors"
                                )
                            )
                        }
                    }
                }
            }
        }

        return violations
    }

    private fun checkRoomCapacityConstraints(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()

        assignments.forEach { (courseId, timeSlot) ->
            val courseInfo = problem.courses.find { it.courseId == courseId }

            if (courseInfo != null && courseInfo.studentCount > timeSlot.roomCapacity) {
                val overflow = courseInfo.studentCount - timeSlot.roomCapacity

                violations.add(
                    ConstraintViolation(
                        violationType = "ROOM_CAPACITY_EXCEEDED",
                        severity = ViolationSeverity.CRITICAL,
                        description = "Room ${timeSlot.roomName} capacity (${timeSlot.roomCapacity}) exceeded by $overflow students for course $courseId",
                        affectedExams = listOf(courseId),
                        affectedStudents = overflow,
                        suggestedResolution = "Assign exam to a larger room or split into multiple sessions"
                    )
                )
            }
        }

        return violations
    }

    private fun checkRoomBookingConflicts(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()
        val assignmentList = assignments.toList()

        for (i in assignmentList.indices) {
            for (j in i + 1 until assignmentList.size) {
                val (courseId1, timeSlot1) = assignmentList[i]
                val (courseId2, timeSlot2) = assignmentList[j]

                if (timeSlot1.roomId == timeSlot2.roomId &&
                    timeSlot1.date == timeSlot2.date &&
                    timeSlotsOverlap(timeSlot1.startTime, timeSlot1.endTime, timeSlot2.startTime, timeSlot2.endTime)) {

                    violations.add(
                        ConstraintViolation(
                            violationType = "ROOM_DOUBLE_BOOKING",
                            severity = ViolationSeverity.CRITICAL,
                            description = "Room ${timeSlot1.roomName} double-booked for courses $courseId1 and $courseId2 on ${timeSlot1.date}",
                            affectedExams = listOf(courseId1, courseId2),
                            affectedStudents = 0,
                            suggestedResolution = "Assign one exam to a different room or time slot"
                        )
                    )
                }
            }
        }

        return violations
    }

    private fun checkTimingConstraints(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()
        val constraints = problem.institutionalConstraints

        assignments.forEach { (courseId, timeSlot) ->
            val courseInfo = problem.courses.find { it.courseId == courseId }

            if (courseInfo != null) {
                val actualDuration = java.time.Duration.between(timeSlot.startTime, timeSlot.endTime).toMinutes()
                if (actualDuration < constraints.minimumExamDuration) {
                    violations.add(
                        ConstraintViolation(
                            violationType = "INSUFFICIENT_EXAM_DURATION",
                            severity = ViolationSeverity.HIGH,
                            description = "Exam for course $courseId duration (${actualDuration}min) is less than minimum required (${constraints.minimumExamDuration}min)",
                            affectedExams = listOf(courseId),
                            affectedStudents = courseInfo.studentCount,
                            suggestedResolution = "Extend exam duration or use longer time slot"
                        )
                    )
                }

                if (timeSlot.startTime.isBefore(constraints.workingHours.startTime) ||
                    timeSlot.endTime.isAfter(constraints.workingHours.endTime)) {
                    violations.add(
                        ConstraintViolation(
                            violationType = "OUTSIDE_WORKING_HOURS",
                            severity = ViolationSeverity.HIGH,
                            description = "Exam for course $courseId scheduled outside working hours (${constraints.workingHours.startTime}-${constraints.workingHours.endTime})",
                            affectedExams = listOf(courseId),
                            affectedStudents = courseInfo.studentCount,
                            suggestedResolution = "Reschedule exam within standard working hours"
                        )
                    )
                }
            }
        }

        return violations
    }

    private fun checkResourceConstraints(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()

        assignments.forEach { (courseId, timeSlot) ->
            val courseInfo = problem.courses.find { it.courseId == courseId }
            val roomInfo = problem.availableRooms.find { it.roomId == timeSlot.roomId }

            if (courseInfo != null && roomInfo != null) {
                val missingEquipment = courseInfo.requiredEquipment.minus(roomInfo.equipment)

                if (missingEquipment.isNotEmpty()) {
                    violations.add(
                        ConstraintViolation(
                            violationType = "MISSING_REQUIRED_EQUIPMENT",
                            severity = ViolationSeverity.HIGH,
                            description = "Room ${roomInfo.roomName} missing required equipment for course $courseId: ${missingEquipment.joinToString(", ")}",
                            affectedExams = listOf(courseId),
                            affectedStudents = courseInfo.studentCount,
                            suggestedResolution = "Assign exam to room with required equipment or provide mobile equipment"
                        )
                    )
                }

                if (courseInfo.accessibilityRequired && !roomInfo.accessibility) {
                    violations.add(
                        ConstraintViolation(
                            violationType = "ACCESSIBILITY_NOT_MET",
                            severity = ViolationSeverity.CRITICAL,
                            description = "Room ${roomInfo.roomName} does not meet accessibility requirements for course $courseId",
                            affectedExams = listOf(courseId),
                            affectedStudents = courseInfo.studentCount,
                            suggestedResolution = "Assign exam to accessible room"
                        )
                    )
                }
            }
        }

        return violations
    }

    private fun checkInstitutionalPolicyConstraints(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()
        val constraints = problem.institutionalConstraints

        val examsByDate = assignments.values.groupBy { it.date }

        examsByDate.forEach { (date, examsOnDate) ->
            if (examsOnDate.size > constraints.maxExamsPerDay) {
                violations.add(
                    ConstraintViolation(
                        violationType = "TOO_MANY_EXAMS_PER_DAY",
                        severity = ViolationSeverity.MEDIUM,
                        description = "Too many exams scheduled on $date (${examsOnDate.size} > ${constraints.maxExamsPerDay})",
                        affectedExams = assignments.filter { it.value.date == date }.keys.toList(),
                        affectedStudents = 0,
                        suggestedResolution = "Distribute exams across more days"
                    )
                )
            }
        }

        return violations
    }

    private fun validateSoftConstraints(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()

        violations.addAll(checkProfessorPreferences(assignments, problem))

        violations.addAll(checkStudentWorkloadDistribution(assignments, problem))

        violations.addAll(checkRoomUtilizationOptimization(assignments, problem))

        violations.addAll(checkExamSpacingPreferences(assignments, problem))

        violations.addAll(checkTimeSlotPreferences(assignments, problem))

        return violations
    }

    private fun checkProfessorPreferences(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()

        problem.professorPreferences.forEach { preference ->
            val assignedTimeSlot = assignments[preference.courseId]

            if (assignedTimeSlot != null) {
                var satisfactionScore = 0.0
                var violationReasons = mutableListOf<String>()

                if (preference.preferredDates.isNotEmpty() &&
                    !preference.preferredDates.contains(assignedTimeSlot.date)) {
                    satisfactionScore -= 0.3
                    violationReasons.add("not on preferred date")
                }

                if (preference.preferredTimeSlots.isNotEmpty()) {
                    val hasMatchingTimeSlot = preference.preferredTimeSlots.any { prefSlot ->
                        timeSlotsOverlap(
                            assignedTimeSlot.startTime, assignedTimeSlot.endTime,
                            prefSlot.startTime, prefSlot.endTime
                        )
                    }
                    if (!hasMatchingTimeSlot) {
                        satisfactionScore -= 0.4
                        violationReasons.add("not in preferred time slot")
                    }
                }

                if (preference.preferredRooms.isNotEmpty() &&
                    !preference.preferredRooms.contains(assignedTimeSlot.roomId)) {
                    satisfactionScore -= 0.2
                    violationReasons.add("not in preferred room")
                }

                if (preference.unavailableDates.contains(assignedTimeSlot.date)) {
                    satisfactionScore -= 0.8
                    violationReasons.add("scheduled on unavailable date")
                }

                if (violationReasons.isNotEmpty()) {
                    val severity = when {
                        satisfactionScore <= -0.7 -> ViolationSeverity.HIGH
                        satisfactionScore <= -0.4 -> ViolationSeverity.MEDIUM
                        else -> ViolationSeverity.LOW
                    }

                    violations.add(
                        ConstraintViolation(
                            violationType = "PROFESSOR_PREFERENCE_NOT_SATISFIED",
                            severity = severity,
                            description = "Professor ${preference.professorId} preferences not satisfied for course ${preference.courseId}: ${violationReasons.joinToString(", ")}",
                            affectedExams = listOf(preference.courseId),
                            affectedStudents = 0,
                            suggestedResolution = "Consider rescheduling to better match professor preferences"
                        )
                    )
                }
            }
        }

        return violations
    }

    private fun checkStudentWorkloadDistribution(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()

        val examsByDate = assignments.values.groupBy { it.date }

        examsByDate.forEach { (date, examsOnDate) ->
            val totalStudentLoad = examsOnDate.sumOf { timeSlot ->
                val courseId = assignments.entries.find { it.value == timeSlot }?.key
                val courseInfo = problem.courses.find { it.courseId == courseId }
                courseInfo?.studentCount ?: 0
            }

            val averageLoad = totalStudentLoad.toDouble() / examsOnDate.size

            if (examsOnDate.size > 3) {
                violations.add(
                    ConstraintViolation(
                        violationType = "UNEVEN_STUDENT_WORKLOAD",
                        severity = ViolationSeverity.MEDIUM,
                        description = "High student workload on $date with ${examsOnDate.size} exams affecting approximately $totalStudentLoad student-exam instances",
                        affectedExams = assignments.filter { it.value.date == date }.keys.toList(),
                        affectedStudents = totalStudentLoad,
                        suggestedResolution = "Redistribute some exams to other dates for better workload balance"
                    )
                )
            }
        }

        return violations
    }

    private fun checkRoomUtilizationOptimization(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()

        assignments.forEach { (courseId, timeSlot) ->
            val courseInfo = problem.courses.find { it.courseId == courseId }

            if (courseInfo != null) {
                val utilizationRate = courseInfo.studentCount.toDouble() / timeSlot.roomCapacity

                if (utilizationRate < 0.4 && timeSlot.roomCapacity > 50) {
                    violations.add(
                        ConstraintViolation(
                            violationType = "LOW_ROOM_UTILIZATION",
                            severity = ViolationSeverity.LOW,
                            description = "Low room utilization for course $courseId: ${courseInfo.studentCount} students in room with capacity ${timeSlot.roomCapacity} (${String.format("%.1f", utilizationRate * 100)}%)",
                            affectedExams = listOf(courseId),
                            affectedStudents = courseInfo.studentCount,
                            suggestedResolution = "Consider using smaller room to improve utilization"
                        )
                    )
                }
            }
        }

        return violations
    }

    private fun checkExamSpacingPreferences(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()
        val constraints = problem.institutionalConstraints
        val assignmentList = assignments.toList()

        for (i in assignmentList.indices) {
            for (j in i + 1 until assignmentList.size) {
                val (courseId1, timeSlot1) = assignmentList[i]
                val (courseId2, timeSlot2) = assignmentList[j]

                if (timeSlot1.date == timeSlot2.date) {
                    val timeBetween = abs(
                        java.time.Duration.between(timeSlot1.endTime, timeSlot2.startTime).toMinutes()
                    )

                    if (timeBetween > 0 && timeBetween < constraints.minimumGapMinutes) {
                        violations.add(
                            ConstraintViolation(
                                violationType = "INSUFFICIENT_EXAM_SPACING",
                                severity = ViolationSeverity.LOW,
                                description = "Insufficient gap between exams $courseId1 and $courseId2 (${timeBetween}min < ${constraints.minimumGapMinutes}min preferred)",
                                affectedExams = listOf(courseId1, courseId2),
                                affectedStudents = 0,
                                suggestedResolution = "Increase gap between exams for better student transition time"
                            )
                        )
                    }
                }
            }
        }

        return violations
    }

    private fun checkTimeSlotPreferences(
        assignments: Map<String, TimeSlot>,
        problem: SchedulingProblem
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()

        assignments.forEach { (courseId, timeSlot) ->
            when {
                timeSlot.startTime.isBefore(LocalTime.of(8, 0)) -> {
                    violations.add(
                        ConstraintViolation(
                            violationType = "VERY_EARLY_EXAM_TIME",
                            severity = ViolationSeverity.MEDIUM,
                            description = "Exam for course $courseId scheduled very early at ${timeSlot.startTime}",
                            affectedExams = listOf(courseId),
                            affectedStudents = problem.courses.find { it.courseId == courseId }?.studentCount ?: 0,
                            suggestedResolution = "Consider scheduling exam later in the day"
                        )
                    )
                }

                timeSlot.endTime.isAfter(LocalTime.of(18, 0)) -> {
                    violations.add(
                        ConstraintViolation(
                            violationType = "VERY_LATE_EXAM_TIME",
                            severity = ViolationSeverity.MEDIUM,
                            description = "Exam for course $courseId scheduled very late ending at ${timeSlot.endTime}",
                            affectedExams = listOf(courseId),
                            affectedStudents = problem.courses.find { it.courseId == courseId }?.studentCount ?: 0,
                            suggestedResolution = "Consider scheduling exam earlier in the day"
                        )
                    )
                }
            }
        }

        return violations
    }


    private fun timeSlotsOverlap(start1: LocalTime, end1: LocalTime, start2: LocalTime, end2: LocalTime): Boolean {
        return start1.isBefore(end2) && start2.isBefore(end1)
    }

    private fun estimateStudentOverlap(courseId1: String, courseId2: String, problem: SchedulingProblem): Int {
        val course1 = problem.courses.find { it.courseId == courseId1 }
        val course2 = problem.courses.find { it.courseId == courseId2 }

        if (course1 == null || course2 == null) return 0

        val overlapRate = when {
            course1.mandatoryStatus == MandatoryStatus.MANDATORY &&
                    course2.mandatoryStatus == MandatoryStatus.MANDATORY -> 0.7

            course1.mandatoryStatus == MandatoryStatus.MANDATORY ||
                    course2.mandatoryStatus == MandatoryStatus.MANDATORY -> 0.4

            else -> 0.2
        }

        return (kotlin.math.min(course1.studentCount, course2.studentCount) * overlapRate).toInt()
    }

    private fun calculateConstraintQualityScore(
        hardViolations: List<ConstraintViolation>,
        softViolations: List<ConstraintViolation>,
        totalExams: Int
    ): Double {
        if (hardViolations.isNotEmpty()) return 0.0

        val softViolationPenalty = softViolations.sumOf { violation ->
            when (violation.severity) {
                ViolationSeverity.CRITICAL -> 0.8
                ViolationSeverity.HIGH -> 0.6
                ViolationSeverity.MEDIUM -> 0.3
                ViolationSeverity.LOW -> 0.1
            }
        }

        val maxPossiblePenalty = totalExams * 0.8 // Assume worst case
        val penaltyRate = if (maxPossiblePenalty > 0) softViolationPenalty / maxPossiblePenalty else 0.0

        return kotlin.math.max(0.0, 1.0 - penaltyRate)
    }
}