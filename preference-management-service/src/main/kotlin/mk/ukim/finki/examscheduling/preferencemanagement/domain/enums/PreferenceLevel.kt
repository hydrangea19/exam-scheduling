package mk.ukim.finki.examscheduling.preferencemanagement.domain.enums

enum class PreferenceLevel(val displayName: String, val priority: Int) {
    PREFERRED("Preferred", 1),
    ACCEPTABLE("Acceptable", 2),
    NOT_PREFERRED("Not Preferred", 3),
    UNAVAILABLE("Unavailable", 4)
}