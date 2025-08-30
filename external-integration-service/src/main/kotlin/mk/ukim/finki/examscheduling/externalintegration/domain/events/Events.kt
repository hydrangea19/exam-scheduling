package mk.ukim.finki.examscheduling.externalintegration.domain.events

import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.DataRefreshResult
import java.time.Instant

data class ExternalDataSynchronizedEvent(
    val courseId: String,
    val semesterCode: String,
    val dataQuality: Double,
    val timestamp: Instant = Instant.now()
)

data class ExternalDataRefreshCompletedEvent(
    val refreshResult: DataRefreshResult,
    val timestamp: Instant = Instant.now()
)