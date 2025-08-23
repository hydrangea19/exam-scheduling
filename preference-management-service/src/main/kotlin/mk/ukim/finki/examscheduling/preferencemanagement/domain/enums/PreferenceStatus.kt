package mk.ukim.finki.examscheduling.preferencemanagement.domain.enums

enum class PreferenceStatus(val displayName: String) {
    DRAFT("Draft"),
    SUBMITTED("Submitted"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}