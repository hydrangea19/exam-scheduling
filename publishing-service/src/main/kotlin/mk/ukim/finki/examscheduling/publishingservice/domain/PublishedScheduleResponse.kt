package mk.ukim.finki.examscheduling.publishingservice.domain

import java.util.*

data class PublishedScheduleResponse(
    val id: UUID,
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val title: String,
    val description: String?,
    val publicationStatus: String,
    val publishedAt: String?,
    val publishedBy: String?,
    val isPublic: Boolean,
    val createdAt: String
)