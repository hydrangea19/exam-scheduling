package mk.ukim.finki.examscheduling.externalintegration.domain.courses

import java.time.Duration
import java.time.Instant

data class CourseEnrollment(
    val courseId: String,
    val semesterCode: String,
    val totalEnrolledStudents: Int,
    val activeEnrollments: Int,
    val estimatedExamAttendance: Int,
    val enrollmentDeadline: Instant,
    val lastSynchronized: Instant = Instant.now()
) {
    fun getRequiredRoomCapacity(): Int {
        return (estimatedExamAttendance * 1.1).toInt()
    }

    fun isEnrollmentStale(): Boolean {
        return Duration.between(lastSynchronized, Instant.now()).toHours() > 24
    }
}