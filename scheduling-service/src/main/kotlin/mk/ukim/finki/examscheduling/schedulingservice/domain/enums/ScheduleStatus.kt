package mk.ukim.finki.examscheduling.schedulingservice.domain.enums

enum class ScheduleStatus {
    DRAFT,
    PREFERENCES_COLLECTED,
    GENERATING,
    GENERATED,
    PUBLISHED_FOR_REVIEW,
    UNDER_REVIEW,
    FINALIZED,
    PUBLISHED
}