package mk.ukim.finki.examscheduling.externalintegration.domain.exams

data class ExamCourseProfessor(
    val professorId: String,
    val fullName: String,
    val email: String,
    val department: String,
    val isMainProfessor: Boolean = false
)