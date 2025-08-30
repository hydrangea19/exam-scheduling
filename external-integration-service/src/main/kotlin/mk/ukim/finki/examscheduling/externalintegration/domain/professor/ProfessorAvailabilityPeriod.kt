package mk.ukim.finki.examscheduling.externalintegration.domain.professor

import java.time.Instant

data class ProfessorAvailabilityPeriod(
    val availableFrom: Instant,
    val availableTo: Instant,
    val unavailablePeriods: List<UnavailablePeriod> = emptyList()
) {
    fun isAvailableInPeriod(startTime: Instant, endTime: Instant): Boolean {
        if (startTime.isBefore(availableFrom) || endTime.isAfter(availableTo)) {
            return false
        }

        return unavailablePeriods.none { period ->
            period.overlaps(startTime, endTime)
        }
    }
}