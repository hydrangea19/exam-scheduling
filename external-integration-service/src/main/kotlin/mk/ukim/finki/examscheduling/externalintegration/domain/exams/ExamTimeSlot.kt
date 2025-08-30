package mk.ukim.finki.examscheduling.externalintegration.domain.exams

import java.time.Duration
import java.time.LocalTime

data class ExamTimeSlot(
    val slotId: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val duration: Duration
) {
    fun getDurationInMinutes(): Int = duration.toMinutes().toInt()
}