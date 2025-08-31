package mk.ukim.finki.examscheduling.preferencemanagement.domain.enums

enum class ValidationRuleType {
    NO_TIME_CONFLICTS,
    MINIMUM_PREFERENCES_COUNT,
    MAXIMUM_PREFERENCES_COUNT,
    VALID_TIME_SLOTS,
    VALID_ROOM_ASSIGNMENTS,
    BUSINESS_HOURS_ONLY,
    REQUIRED_COURSE_COVERAGE,
    NO_OVERLAPPING_EXAMS
}