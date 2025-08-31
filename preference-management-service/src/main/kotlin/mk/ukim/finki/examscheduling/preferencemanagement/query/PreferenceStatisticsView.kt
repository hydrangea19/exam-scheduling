package mk.ukim.finki.examscheduling.preferencemanagement.query

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.*

@Entity
@Table(name = "preference_statistics_view")
data class PreferenceStatisticsView(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val examSessionPeriodId: String,
    val timeSlotDay: Int,
    val timeSlotStart: String,
    val timeSlotEnd: String,
    val preferenceLevel: String,
    val preferenceCount: Int,
    val conflictingPreferences: Int = 0,
    val lastUpdated: Instant = Instant.now()
) {
    constructor() : this("", "", 0, "", "", "", 0, 0, Instant.now())
}