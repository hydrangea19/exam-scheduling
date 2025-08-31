package mk.ukim.finki.examscheduling.schedulingservice.domain.enums

enum class ConflictType {
    TIME_OVERLAP,
    ROOM_CAPACITY,
    PROFESSOR_AVAILABILITY,
    STUDENT_OVERLOAD,
    RESOURCE_CONSTRAINT,
    POLICY_VIOLATION
}