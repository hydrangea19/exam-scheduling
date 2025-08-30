package mk.ukim.finki.examscheduling.externalintegration.domain.exams

import java.time.Instant

data class ReservedPeriod(
    val startTime: Instant,
    val endTime: Instant,
    val reservedBy: String,
    val purpose: String
) {
    fun overlaps(otherStart: Instant, otherEnd: Instant): Boolean {
        return startTime.isBefore(otherEnd) && endTime.isAfter(otherStart)
    }
}