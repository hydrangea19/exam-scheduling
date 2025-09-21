package mk.ukim.finki.examscheduling.publishingservice.domain

import java.util.*

data class CreatePublishedScheduleRequest(
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val title: String,
    val description: String?,
    val publishedBy: String,
    val isPublic: Boolean
)
