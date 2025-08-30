package mk.ukim.finki.examscheduling.externalintegration.domain.professor

import java.time.Instant

data class UnavailablePeriod(
    val startTime: Instant,
    val endTime: Instant,
    val reason: String
) {
    fun overlaps(otherStart: Instant, otherEnd: Instant): Boolean {
        return startTime.isBefore(otherEnd) && endTime.isAfter(otherStart)
    }
}