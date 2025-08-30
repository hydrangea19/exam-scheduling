package mk.ukim.finki.examscheduling.externalintegration.domain.exams

import mk.ukim.finki.examscheduling.externalintegration.domain.enums.CourseStudyCycle
import java.time.Instant

data class ExamCourse(
    val courseId: String,
    val courseName: String,
    val courseCode: String,
    val credits: Float,
    val isMandatory: Boolean,
    val semester: Int,
    val studyCycle: CourseStudyCycle,
    val professors: List<ExamCourseProfessor>,
    val estimatedDurationMinutes: Int,
    val department: String?,
    val prerequisites: List<String> = emptyList(),
    val lastUpdated: Instant = Instant.now()
) {

    fun getExamDuration(): Int = when {
        credits <= 3.0f -> 90
        credits <= 6.0f -> 120
        else -> 150
    }

    fun requiresSpecialRoom(): Boolean {
        return courseName.contains("Lab", ignoreCase = true) ||
                courseName.contains("Computer", ignoreCase = true) ||
                courseName.contains("Programming", ignoreCase = true)
    }
}
