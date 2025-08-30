package mk.ukim.finki.examscheduling.externalintegration.domain.exams

import java.time.Instant

data class RoomAvailabilityPeriod(
    val availableFrom: Instant,
    val availableTo: Instant,
    val maintenancePeriods: List<MaintenancePeriod> = emptyList(),
    val reservedPeriods: List<ReservedPeriod> = emptyList()
) {
    fun isAvailableForExam(examStart: Instant, examEnd: Instant): Boolean {
        if (examStart.isBefore(availableFrom) || examEnd.isAfter(availableTo)) {
            return false
        }

        val isInMaintenance = maintenancePeriods.any { it.overlaps(examStart, examEnd) }
        val isReserved = reservedPeriods.any { it.overlaps(examStart, examEnd) }

        return !isInMaintenance && !isReserved
    }
}