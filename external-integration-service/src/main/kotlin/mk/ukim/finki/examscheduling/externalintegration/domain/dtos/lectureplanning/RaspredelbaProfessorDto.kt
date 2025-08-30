package mk.ukim.finki.examscheduling.externalintegration.domain.dtos.lectureplanning

data class RaspredelbaProfessorDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val department: String,
    val status: String,
    val availableFrom: String,
    val availableTo: String,
    val teachesSubjects: List<String>,
    val lectures: Float,
    val auditory: Float,
    val labaratory: Float,
    val englishGroup: Boolean,
    val mentoring: Boolean,
    val lastUpdated: String
)