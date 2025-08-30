package mk.ukim.finki.examscheduling.externalintegration.domain.exams

import mk.ukim.finki.examscheduling.externalintegration.domain.professor.ProfessorAvailabilityPeriod
import java.time.Instant

data class ExamProfessor(
    val professorId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val department: String,
    val isActive: Boolean,
    val teachingSubjects: List<String>,
    val availabilityPeriod: ProfessorAvailabilityPeriod?,
    val preferredTimeSlots: List<String> = emptyList(),
    val maxExamsPerDay: Int = 3,
    val lastUpdated: Instant = Instant.now()
) {
    fun getFullName(): String = "$firstName $lastName"

    fun isAvailableForExamPeriod(examStart: Instant, examEnd: Instant): Boolean {
        return isActive && availabilityPeriod?.isAvailableInPeriod(examStart, examEnd) == true
    }

    fun canTeachCourse(courseId: String): Boolean = teachingSubjects.contains(courseId)
}