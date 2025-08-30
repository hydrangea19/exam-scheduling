package mk.ukim.finki.examscheduling.externalintegration.domain.exams

import java.time.Instant

data class MaintenancePeriod(
    val startTime: Instant,
    val endTime: Instant,
    val reason: String
) {
    fun overlaps(otherStart: Instant, otherEnd: Instant): Boolean {
        return startTime.isBefore(otherEnd) && endTime.isAfter(otherStart)
    }
}