package mk.ukim.finki.examscheduling.externalintegration.domain.exams

import mk.ukim.finki.examscheduling.externalintegration.domain.enums.ExamSessionType
import java.time.LocalDate

data class ExamSessionContext(
    val sessionId: String,
    val sessionName: String,
    val semesterCode: String,
    val sessionStartDate: LocalDate,
    val sessionEndDate: LocalDate,
    val examTimeSlots: List<ExamTimeSlot>,
    val sessionType: ExamSessionType
) {
    fun isWithinSession(date: LocalDate): Boolean {
        return !date.isBefore(sessionStartDate) && !date.isAfter(sessionEndDate)
    }
}