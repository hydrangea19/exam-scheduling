package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.ConstraintViolation
import mk.ukim.finki.examscheduling.schedulingservice.domain.InstitutionalConstraints
import mk.ukim.finki.examscheduling.schedulingservice.domain.ScheduledExamInfo
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ViolationSeverity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalTime

@Component
class ExamSchedulingConstraintSystem {

    private val logger = LoggerFactory.getLogger(ExamSchedulingConstraintSystem::class.java)

    fun validateScheduledExams(
        scheduledExams: List<ScheduledExamInfo>,
        institutionalConstraints: InstitutionalConstraints
    ): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()

        violations.addAll(checkBasicTimeConflicts(scheduledExams))

        violations.addAll(checkBasicRoomCapacity(scheduledExams))

        violations.addAll(checkWorkingHours(scheduledExams, institutionalConstraints))

        return violations
    }

    private fun checkBasicTimeConflicts(scheduledExams: List<ScheduledExamInfo>): List<ConstraintViolation> {
        val violations = mutableListOf<ConstraintViolation>()

        for (i in scheduledExams.indices) {
            for (j in i + 1 until scheduledExams.size) {
                val exam1 = scheduledExams[i]
                val exam2 = scheduledExams[j]

                if (exam1.examDate == exam2.examDate &&
                    exam1.roomId == exam2.roomId &&
                    timeSlotsOverlap(exam1.startTime, exam1.endTime, exam2.startTime, exam2.endTime)
                ) {

                    violations.add(
                        ConstraintViolation(
                            violationType = "ROOM_DOUBLE_BOOKING",
                            severity = ViolationSeverity.CRITICAL,
                            description = "Room ${exam1.roomName} double-booked for courses ${exam1.courseId} and ${exam2.courseId} on ${exam1.examDate}",
                            affectedExams = listOf(exam1.courseId, exam2.courseId),
                            affectedStudents = exam1.studentCount + exam2.studentCount,
                            suggestedResolution = "Reschedule one exam to different time or room"
                        )
                    )
                }
            }
        }

        return violations
    }

    private fun checkBasicRoomCapacity(scheduledExams: List<ScheduledExamInfo>): List<ConstraintViolation> {
        return scheduledExams.mapNotNull { exam ->
            if (exam.roomCapacity != null && exam.studentCount > exam.roomCapacity) {
                ConstraintViolation(
                    violationType = "ROOM_CAPACITY_EXCEEDED",
                    severity = ViolationSeverity.CRITICAL,
                    description = "Room ${exam.roomName} capacity (${exam.roomCapacity}) exceeded by ${exam.studentCount - exam.roomCapacity} students for course ${exam.courseId}",
                    affectedExams = listOf(exam.courseId),
                    affectedStudents = exam.studentCount - exam.roomCapacity,
                    suggestedResolution = "Assign to larger room or split into multiple sessions"
                )
            } else null
        }
    }

    private fun checkWorkingHours(
        scheduledExams: List<ScheduledExamInfo>,
        constraints: InstitutionalConstraints
    ): List<ConstraintViolation> {
        return scheduledExams.mapNotNull { exam ->
            if (exam.startTime.isBefore(constraints.workingHours.startTime) ||
                exam.endTime.isAfter(constraints.workingHours.endTime)
            ) {

                ConstraintViolation(
                    violationType = "OUTSIDE_WORKING_HOURS",
                    severity = ViolationSeverity.HIGH,
                    description = "Exam for course ${exam.courseId} scheduled outside working hours (${constraints.workingHours.startTime}-${constraints.workingHours.endTime})",
                    affectedExams = listOf(exam.courseId),
                    affectedStudents = exam.studentCount,
                    suggestedResolution = "Reschedule exam within standard working hours"
                )
            } else null
        }
    }

    private fun timeSlotsOverlap(start1: LocalTime, end1: LocalTime, start2: LocalTime, end2: LocalTime): Boolean {
        return start1.isBefore(end2) && start2.isBefore(end1)
    }
}