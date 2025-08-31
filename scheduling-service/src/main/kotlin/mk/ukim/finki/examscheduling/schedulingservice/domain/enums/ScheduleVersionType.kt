package mk.ukim.finki.examscheduling.schedulingservice.domain.enums

enum class ScheduleVersionType {
    INITIAL_GENERATION,
    MANUAL_ADJUSTMENT,
    BULK_UPDATE,
    FEEDBACK_INTEGRATION,
    FINAL_VERSION,
    EMERGENCY_ROLLBACK
}