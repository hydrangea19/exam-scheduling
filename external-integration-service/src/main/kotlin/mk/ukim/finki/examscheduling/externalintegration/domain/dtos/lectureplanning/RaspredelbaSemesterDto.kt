package mk.ukim.finki.examscheduling.externalintegration.domain.dtos.lectureplanning

data class RaspredelbaSemesterDto(
    val code: String,
    val year: String,
    val startDate: String,
    val endDate: String,
    val enrollmentStartDate: String,
    val enrollmentEndDate: String,
    val semesterType: String,
    val semesterCycle: String,
    val semesterStatus: String
)