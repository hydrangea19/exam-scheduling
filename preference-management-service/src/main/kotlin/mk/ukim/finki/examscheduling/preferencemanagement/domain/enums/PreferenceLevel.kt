package mk.ukim.finki.examscheduling.preferencemanagement.domain.enums

enum class PreferenceLevel(val displayName: String, val priority: Int) {
    HIGHLY_PREFERRED("Highly Preferred", 3),
    PREFERRED("Preferred", 2),
    ACCEPTABLE("Acceptable", 1),
    NOT_PREFERRED("Not Preferred", 0),
    AVOID("Avoid", -1),
    NOT_AVAILABLE("Not Available", -2);

    fun isPositive(): Boolean = priority > 0
    fun isNegative(): Boolean = priority < 0
}
